package com.coinomi.wallet.ui;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.Unbinder;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.eth.CallTransaction;
import com.coinomi.core.coins.eth.CallTransaction.Function;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.util.MonetaryFormat;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.families.eth.ERC20Token;
import com.coinomi.core.wallet.families.eth.EthAddress;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.adaptors.ContractFunctionsInputsListAdapter;
import com.coinomi.wallet.ui.adaptors.ContractFunctionsListAdapter;
import com.coinomi.wallet.ui.widget.AmountEditView;
import com.coinomi.wallet.ui.widget.Amount;
import com.coinomi.wallet.util.ThrottlingAccountContractChangeListener;
import com.coinomi.wallet.util.UiUtils;
import com.coinomi.wallet.util.WeakHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class ContractFunctionFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(ContractFunctionFragment.class);
    MenuItem actionDeleteMenu;
    private EthContract contract;
    @BindView(2131689698)
    TextView contractAddress;
    @BindView(2131689699)
    Amount contractBalance;
    @BindView(2131689696)
    TextView contractDescription;
    @BindView(2131689695)
    ListView contractFunctions;
    @BindView(2131689697)
    TextView contractUrl;
    private String contractId;
    @BindView(2131689701)
    TextView description;
    private Function function;
    @BindView(2131689698)
    ListView functionInputs;
    private final MyHandler handler = new MyHandler(this);
    private final MyAccountContractListener listener = new MyAccountContractListener(this.handler);
    @BindView(2131689702)
    TextView noInputsDescription;
    @BindView(2131689700)
    TextView outputHistory;
    private EthFamilyWallet pocket;
    @BindView(2131689706)
    AmountEditView sendCoinAmountView;
    @BindView(2131689700)
    Amount exchangedBalance;
    private CoinType type;
    private Unbinder unbinder;
    private boolean isFullAmount = false;
    private ExchangeRate lastRate;
    private EthFamilyWallet account;
    private String accountId;
    private MonetaryFormat fullMonetaryFormat;
    private ContractFunctionsListAdapter functionsAdapter;
    private MyLoaderCallback loaderCallback;
    private MonetaryFormat shortMonetaryFormat;

    static class MyAccountContractListener extends ThrottlingAccountContractChangeListener {
        private final MyHandler handler;

        public MyAccountContractListener(MyHandler handler) {
            this.handler = handler;
        }

        public void onThrottledContractChanged() {
            this.handler.sendEmptyMessage(0);
        }
    }

    private static class MyHandler extends WeakHandler<ContractFunctionFragment> {
        public MyHandler(ContractFunctionFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(ContractFunctionFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.updateView();
                    return;
                default:
                    return;
            }
        }
    }

    private static class MyLoaderCallback implements LoaderCallbacks<Cursor> {
        private final WalletApplication app;
        Configuration config;
        MyHandler handler;
        String symbol;

        MyLoaderCallback(WalletApplication app, String symbol, MyHandler handler) {
            this.app = app;
            this.config = app.getConfiguration();
            this.symbol = symbol;
            this.handler = handler;
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new ExchangeRateLoader(this.app, this.config, this.config.getExchangeCurrencyCode(), this.symbol);
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                this.handler.sendMessage(this.handler.obtainMessage(1, ExchangeRatesProvider.getExchangeRate(data).rate));
            }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

   /* public static ContractDetailsFragment newInstance(String accountId, ERC20Token subType) {
        ContractDetailsFragment fragment = new ContractDetailsFragment();
        Bundle args = new Bundle();
        args.putString("account_id", accountId);
        args.putString("contract_id", subType.getAddress());
        fragment.setArguments(args);
        return fragment;
    }*/

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String accountId = getArguments().getString("account_id");
            String functionName = getArguments().getString("function_name");
            this.contractId = getArguments().getString("contract_id");
            this.pocket = (EthFamilyWallet) getWalletApplication().getAccount(accountId);
            if (this.pocket == null) {
                Toast.makeText(getActivity(), R.string.no_such_pocket_error, Toast.LENGTH_LONG).show();
                return;
            }
            this.type = this.pocket.getCoinType();
            this.contract = (EthContract) this.pocket.getAllContracts().get(this.contractId);
            if (this.contract == null) {
                functionError();
                return;
            }
            this.function = this.contract.getContract().getByName(functionName);
            if (this.function == null) {
                functionError();
                return;
            }
            if (getActivity() instanceof AppCompatActivity) {
                AppCompatActivity a = (AppCompatActivity) getActivity();
                if (a.getSupportActionBar() != null) {
                    a.getSupportActionBar().setTitle(this.function.name.isEmpty() ? getString(R.string.function_default) : this.function.name);
                }
            }
            setHasOptionsMenu(true);
            return;
        }
        functionError();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contract_function, container, false);
        addHeaderAndFooterToList(inflater, container, view);
        this.unbinder = ButterKnife.bind((Object) this, view);
        if (this.function.inputs.length == 0 && this.function.constant) {
            this.noInputsDescription.setVisibility(View.VISIBLE);
        } else {
            this.noInputsDescription.setVisibility(View.GONE);
        }
        this.description.setVisibility(View.GONE);
        this.sendCoinAmountView.resetType(this.type, true);
        if (this.function.constant) {
            this.sendCoinAmountView.setVisibility(View.GONE);
        } else {
            this.sendCoinAmountView.setVisibility(View.VISIBLE);
        }
        setupAdapter(inflater);
        updateView();
        return view;
    }

    public void onDestroyView() {
        this.unbinder.unbind();
        super.onDestroyView();
    }

    private void setupAdapter(LayoutInflater inflater) {
        if (!isRemoving() && !isDetached()) {
            this.functionInputs.setAdapter(new ContractFunctionsInputsListAdapter(inflater.getContext(), this.function.inputs));
        }
    }


        /*if (this.function.constant) {
            this.pocket.callContractFunction(this.contractId, this.function.name, inputValues, this.sendCoinAmountView.getAmount());
            return;
        }
        onMakeTransaction(new EthAddress(this.pocket.getCoinType(), this.contractId), this.sendCoinAmountView.getAmount(), Hex.toHexString(CallTransaction.createCallTransaction(0, 1, 1000000, this.contract.getContractAddress().replace("0x", ""), 0, this.contract.getContract().getByName(this.function.name), inputValues.toArray()).getData()));*/

    @OnClick({2131689702})
    public void onExecuteClick() {
        List<String> inputValues = new ArrayList();
        for (int i = this.functionInputs.getHeaderViewsCount(); i < this.functionInputs.getCount() - this.functionInputs.getFooterViewsCount(); i++) {
            EditText et = (EditText) this.functionInputs.getChildAt(i).findViewById(R.id.function_input_edit);
            if (et.getText() == null || et.getText().toString().isEmpty()) {
                Toast.makeText(getActivity(), R.string.function_input_empty, Toast.LENGTH_LONG).show();
                return;
            }
            inputValues.add(et.getText().toString());
        }
        if (this.function.constant) {
            this.pocket.callContractFunction(this.contractId, this.function.name, this.sendCoinAmountView.getAmount(), inputValues.toArray());
            return;
        }
        try {
            EthAddress contractAddress = (EthAddress) this.pocket.getCoinType().newAddress(this.contractId);
            onMakeTransaction(contractAddress, this.sendCoinAmountView.getAmount(), Hex.toHexString(CallTransaction.createCallTransaction(0, 1, 1000000, contractAddress.getHexString(), 0, this.contract.getContract().getByName(this.function.name), inputValues.toArray()).getData()));
        } catch (AddressMalformedException e) {
            Toast.makeText(getActivity(), R.string.address_error, Toast.LENGTH_LONG).show();
        }
    }
    public void onMakeTransaction(AbstractAddress toAddress, Value amount, String contractData) {
        Intent intent = new Intent(getActivity(), SignTransactionActivity.class);
        if (amount.compareTo(this.pocket.getBalance()) == 0) {
            intent.putExtra("empty_wallet", true);
        } else {
            intent.putExtra("send_value", amount);
        }
        intent.putExtra("account_id", this.pocket.getId());
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

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.exchange_status, menu);
        this.actionDeleteMenu = menu.findItem(R.id.action_delete);
        updateView();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                new Builder(getActivity()).setMessage(R.string.clear_function_history).setNegativeButton(R.string.button_cancel, null).setPositiveButton(R.string.button_ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ContractFunctionFragment.this.contract.getHistory(ContractFunctionFragment.this.function.name).clear();
                        ContractFunctionFragment.this.updateView();
                    }
                }).create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addHeaderAndFooterToList(LayoutInflater inflater, ViewGroup container, View view) {
        ListView list = (ListView) ButterKnife.findById(view, (int) R.id.output_rows);
        list.addHeaderView(inflater.inflate(R.layout.fragment_contract_details_header, null), null, true);
        View listFooter = new View(inflater.getContext());
        listFooter.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin));
        list.addFooterView(listFooter);
    }

    @OnClick({2131689698})
    public void onContractAddressClick() {
        UiUtils.copy(getContext(), this.contractId);
        Toast.makeText(getActivity(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
    }

    @OnItemClick({2131689695})
    public void onItemClick(int position) {
        if (position >= this.contractFunctions.getHeaderViewsCount()) {
            Object obj = this.contractFunctions.getItemAtPosition(position);
            if (obj == null || !(obj instanceof Function)) {
                Toast.makeText(getActivity(), getString(R.string.get_contract_info_error), Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(getActivity(), ContractFunctionActivity.class);
            intent.putExtra("account_id", this.account.getId());
            intent.putExtra("function_name", ((Function) obj).name);
            intent.putExtra("contract_id", this.contractId);
            startActivity(intent);
        }
    }

    private void updateView() {
        if (!isRemoving() && !isDetached()) {
            if (this.contractId == null) {
                cannotShowTxDetails();
            } else if (this.account.getAllContracts().containsKey(this.contractId)) {
                this.contractBalance.setAmount(GenericUtils.formatValue(this.isFullAmount ? this.fullMonetaryFormat : this.shortMonetaryFormat, this.contract.getBalance()));
                if (this.lastRate != null) {
                    this.exchangedBalance.setAmount(GenericUtils.formatFiatValue(this.lastRate.convert(this.contract.getBalance())));
                }
            } else {
                cannotShowTxDetails();
            }
        }
    }

    public void onDetach() {
        getLoaderManager().destroyLoader(0);
        super.onDetach();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.loaderCallback == null) {
            this.loaderCallback = new MyLoaderCallback((WalletApplication) getActivity().getApplication(), this.type.getSymbol(), this.handler);
        }
        getLoaderManager().initLoader(0, null, this.loaderCallback);
    }

    public void onResume() {
        super.onResume();
        this.account.addContractEventListener(this.contractId, this.listener);
        updateView();
    }

    public void onPause() {
        this.account.removeContractEventListener(this.contractId, this.listener);
        this.listener.removeCallbacks();
        super.onPause();
    }

  /*  private void updateView() {
        if (!isRemoving() && !isDetached()) {
            List<JSONObject> historyEntries = this.contract.getHistory(this.function.name);
            if (this.actionDeleteMenu != null) {
                this.actionDeleteMenu.setVisible(historyEntries.size() > 0);
            }
            this.outputHistory.setText(parseHistory(historyEntries));
        }
    }*/

    private String parseHistory(List<JSONObject> historyEntries) {
        StringBuilder builder = new StringBuilder();
        for (JSONObject history : historyEntries) {
            try {
                String key;
                builder.append("Inputs:\n");
                JSONObject inputs = history.getJSONObject("inputs");
                Iterator<String> iter = inputs.keys();
                while (iter.hasNext()) {
                    key = (String) iter.next();
                    builder.append(key).append(": ");
                    builder.append(inputs.getString(key));
                    builder.append("\n");
                }
                builder.append("Outputs:\n");
                JSONObject outputs = history.getJSONObject("outputs");
                iter = outputs.keys();
                while (iter.hasNext()) {
                    key = (String) iter.next();
                    builder.append(key).append(": ");
                    builder.append(outputs.getString(key));
                    builder.append("\n");
                }
            } catch (JSONException e) {
                builder.append(history.toString());
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private void functionError() {
        Toast.makeText(getActivity(), getString(R.string.function_error), Toast.LENGTH_LONG).show();
        getActivity().finish();
    }
    private void cannotShowTxDetails() {
        Toast.makeText(getActivity(), getString(R.string.get_tx_info_error), Toast.LENGTH_LONG).show();
        getActivity().finish();
    }
    WalletApplication getWalletApplication() {
        return (WalletApplication) getActivity().getApplication();
    }
}
