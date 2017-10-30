package com.coinomi.wallet.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.eth.CallTransaction;
import com.coinomi.core.coins.eth.CallTransaction.Function;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.exceptions.ExecutionException;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.families.eth.EthAddress;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.core.wallet.families.eth.EthTransaction;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.util.ThrottlingAccountContractChangeListener;
import com.coinomi.wallet.util.TimeUtils;
import com.coinomi.wallet.util.WeakHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class SmartInFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(SmartInFragment.class);
    private EthFamilyWallet account;
    private String accountId;
    private EthContract contract;
    private String contractId;
    private final MyHandler handler = new MyHandler(this);
    private final MyAccountContractListener listener = new MyAccountContractListener(this.handler);
    private CoinType type;

    static class MyAccountContractListener extends ThrottlingAccountContractChangeListener {
        private final MyHandler handler;

        public MyAccountContractListener(MyHandler handler) {
            this.handler = handler;
        }

        public void onThrottledContractChanged() {
            this.handler.sendEmptyMessage(0);
        }
    }

    private static class MyHandler extends WeakHandler<SmartInFragment> {
        public MyHandler(SmartInFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(SmartInFragment ref, Message msg) {
            int i = msg.what;
        }
    }

    public class SmartInInterface {
        Context mContext;

        public SmartInInterface(Context c) {
            this.mContext = c;
        }

        @JavascriptInterface
        public boolean executeFunction(String functionName, String value, String[] functionInputs) {
            try {
                String data = EthContract.executeFunction(SmartInFragment.this.account, SmartInFragment.this.contractId, functionName, value, functionInputs);
                if (!(SmartInFragment.this.contract.getContract().getByName(functionName).constant || data == null)) {
                    SmartInFragment.this.onMakeTransaction(SmartInFragment.this.type.newAddress(SmartInFragment.this.contractId), Value.parse(SmartInFragment.this.type, value), data);
                }
                return true;
            } catch (ExecutionException e) {
                return false;
            } catch (AddressMalformedException e2) {
                return false;
            }
        }

        @JavascriptInterface
        public String getContractName() {
            return SmartInFragment.this.contract.getName();
        }

        @JavascriptInterface
        public String getContractDescription() {
            return SmartInFragment.this.contract.getDescription();
        }

        @JavascriptInterface
        public String getContractAddress() {
            return SmartInFragment.this.contract.getContractAddress();
        }

        @JavascriptInterface
        public String getContractWebsite() {
            return SmartInFragment.this.contract.getOfficialSite();
        }

        @JavascriptInterface
        public String hex2amount(String hex) {
            return Value.valueOf(SmartInFragment.this.type, hex).toPlainString();
        }

        @JavascriptInterface
        public String getMyAddress() {
            try {
                return SmartInFragment.this.account.getReceiveAddress().toString();
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String getBalance() {
            try {
                return SmartInFragment.this.account.getBalance().toFriendlyString();
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String getLastResult(String function) {
            try {
                if (SmartInFragment.this.contract.getHistory(function).isEmpty()) {
                    return "";
                }
                return ((JSONObject) SmartInFragment.this.contract.getHistory(function).get(0)).toString();
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String getResults(String function) {
            try {
                return new JSONArray(SmartInFragment.this.contract.getHistory(function)).toString();
            } catch (Exception e) {
                return "[]";
            }
        }

        @JavascriptInterface
        public String getTransactions() {
            try {
                JSONArray array = new JSONArray();
                List<EthTransaction> transactions = new ArrayList(SmartInFragment.this.account.getTransactions().values());
                Collections.sort(transactions, new Comparator<EthTransaction>() {
                    public int compare(EthTransaction ethTransaction, EthTransaction t1) {
                        return ethTransaction.getTimestamp() > t1.getTimestamp() ? -1 : 1;
                    }
                });
                for (EthTransaction ethTransaction : transactions) {
                    if (((AbstractOutput) ethTransaction.getSentTo().get(0)).getAddress().toString().equalsIgnoreCase(SmartInFragment.this.contract.getContractAddress())) {
                        array.put(new JSONObject().put("tx", ethTransaction.getJSONString()).put("logs", ethTransaction.getLogs(SmartInFragment.this.account)));
                    }
                }
                return array.toString();
            } catch (Exception e) {
                return "[]";
            }
        }

        @JavascriptInterface
        public String formatAddress(String address) {
            try {
                address = GenericUtils.addressSplitToGroupsMultiline(SmartInFragment.this.type.newAddress(address));
            } catch (AddressMalformedException e) {
            }
            return address;
        }

        @JavascriptInterface
        public String formatTime(String timestamp) {
            try {
                timestamp = TimeUtils.toRelativeTimeString(Long.valueOf(timestamp).longValue()).toString();
            } catch (Exception e) {
            }
            return timestamp;
        }

        @JavascriptInterface
        public String getIcon() {
            return SmartInFragment.this.contract.getIcon();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_default:
                Intent intent = new Intent(getActivity(), ContractDetailsActivity.class);
                intent.putExtras(getArguments());
                intent.putExtra("show_default", true);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERSION.SDK_INT < 17) {
            Toast.makeText(getContext(), R.string.error_contract_suit, Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
        if (getArguments() != null) {
            this.accountId = getArguments().getString("account_id");
            this.contractId = getArguments().getString("contract_id");
        }
        setHasOptionsMenu(true);
        WalletAccount a = getWalletApplication().getAccount(this.accountId);
        if (a == null || !(a instanceof EthFamilyWallet)) {
            Toast.makeText(getActivity(), R.string.no_such_pocket_error, Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
        this.account = (EthFamilyWallet) a;
        this.type = this.account.getCoinType();
        this.account.subscribeToContract(this.contractId);
        this.contract = (EthContract) this.account.getAllContracts().get(this.contractId);
        ((ContractDetailsActivity) getActivity()).setActionBarTitle(this.contract.getName());
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.contract_show_default, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.smart_in, container, false);
        WebView smartIn = (WebView) view.findViewById(R.id.smart_in);
        if (VERSION.SDK_INT >= 17) {
            smartIn.getSettings().setJavaScriptEnabled(true);
            smartIn.addJavascriptInterface(new SmartInInterface(getContext()), "coinomi");
            smartIn.loadData(this.contract.getContractSuit(), "text/html", "UTF-8");
        }
        return view;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onResume() {
        this.account.addContractEventListener(this.contractId, this.listener);
        super.onResume();
    }

    public void onPause() {
        this.account.removeContractEventListener(this.contractId, this.listener);
        this.listener.removeCallbacks();
        super.onPause();
    }

    WalletApplication getWalletApplication() {
        return (WalletApplication) getActivity().getApplication();
    }

    public void onMakeTransaction(AbstractAddress toAddress, Value amount, String contractData) {
        Intent intent = new Intent(getActivity(), SignTransactionActivity.class);
        if (amount.compareTo(this.account.getBalance()) == 0) {
            intent.putExtra("empty_wallet", true);
        } else {
            intent.putExtra("send_value", amount);
        }
        intent.putExtra("account_id", this.account.getId());
        intent.putExtra("send_to_address", toAddress);
        if (contractData != null) {
            intent.putExtra("contract_data", contractData);
        }
        startActivityForResult(intent, 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode != 1 || resultCode != -1) {
            return;
        }
        if (((Exception) intent.getSerializableExtra("error")) == null) {
            Toast.makeText(getActivity(), R.string.sending_msg, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), R.string.get_tx_broadcast_error, Toast.LENGTH_LONG).show();
        }
    }
}
