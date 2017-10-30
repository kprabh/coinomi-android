package com.coinomi.wallet.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.Unbinder;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.eth.CallTransaction.Function;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.util.MonetaryFormat;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.families.eth.ERC20Token;
import com.coinomi.core.wallet.families.eth.EthAddress;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.adaptors.ContractFunctionsListAdapter;
import com.coinomi.wallet.ui.widget.Amount;
import com.coinomi.wallet.util.ThrottlingAccountContractChangeListener;
import com.coinomi.wallet.util.UiUtils;
import com.coinomi.wallet.util.WeakHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractDetailsFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(ContractDetailsFragment.class);
    private EthFamilyWallet account;
    private String accountId;
    private EthContract contract;
    @BindView(2131689698)
    TextView contractAddress;
    @BindView(2131689696)
    Amount contractBalance;
    @BindView(2131689694)
    TextView contractDescription;
    @BindView(2131689693)
    ListView contractFunctions;
    private String contractId;
    @BindView(2131689695)
    TextView contractUrl;
    @BindView(2131689697)
    Amount exchangedBalance;
    private MonetaryFormat fullMonetaryFormat;
    private ContractFunctionsListAdapter functionsAdapter;
    private final MyHandler handler = new MyHandler(this);
    private boolean isFullAmount = false;
    private ExchangeRate lastRate;
    private final MyAccountContractListener listener = new MyAccountContractListener(this.handler);
    private MyLoaderCallback loaderCallback;
    private MonetaryFormat shortMonetaryFormat;
    private CoinType type;
    private Unbinder unbinder;

    static class MyAccountContractListener extends ThrottlingAccountContractChangeListener {
        private final MyHandler handler;

        public MyAccountContractListener(MyHandler handler) {
            this.handler = handler;
        }

        public void onThrottledContractChanged() {
            this.handler.sendEmptyMessage(0);
        }
    }

    private static class MyHandler extends WeakHandler<ContractDetailsFragment> {
        public MyHandler(ContractDetailsFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(ContractDetailsFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.updateView();
                    return;
                case 1:
                    ref.lastRate = (ExchangeRate) msg.obj;
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

    public static ContractDetailsFragment newInstance(String accountId, ERC20Token subType) {
        ContractDetailsFragment fragment = new ContractDetailsFragment();
        Bundle args = new Bundle();
        args.putString("account_id", accountId);
        args.putString("contract_id", subType.getAddress());
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.accountId = getArguments().getString("account_id");
            this.contractId = getArguments().getString("contract_id");
        }
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
       // ((ContractDetailsActivity) getActivity()).setActionBarTitle(this.contract.getName());
        this.fullMonetaryFormat = this.type.getMoneyFormat();
        this.shortMonetaryFormat = this.type.getMoneyFormat().minDecimals(2).optionalDecimals(2);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contract_details, container, false);
        addHeaderAndFooterToList(inflater, container, view);
        this.unbinder = ButterKnife.bind((Object) this, view);
        try {
            contractAddress.setText(GenericUtils.addressSplitToGroups(new EthAddress(this.type, this.contractId)));
        } catch (AddressMalformedException e) {
            e.printStackTrace();
        }
        this.contractDescription.setText(this.contract.getDescription());
        this.contractUrl.setText(this.contract.getOfficialSite());
        this.contractBalance.setSymbol(this.type.getSymbol());
        this.exchangedBalance.setSymbol(getWalletApplication().getConfiguration().getExchangeCurrencyCode());
        setupAdapter(inflater);
        return view;
    }

    public void onDestroyView() {
        this.unbinder.unbind();
        super.onDestroyView();
    }

    private void setupAdapter(LayoutInflater inflater) {
        if (!isRemoving() && !isDetached()) {
            if (this.contractId == null) {
                cannotShowTxDetails();
            } else if (this.account.getAllContracts().containsKey(this.contractId)) {
                this.functionsAdapter = new ContractFunctionsListAdapter(inflater.getContext(), this.contract);
                this.contractFunctions.setAdapter(this.functionsAdapter);
            } else {
                cannotShowTxDetails();
            }
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

    @OnItemClick({2131689693})
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

    private void cannotShowTxDetails() {
        Toast.makeText(getActivity(), getString(R.string.get_tx_info_error), Toast.LENGTH_LONG).show();
        getActivity().finish();
    }

    WalletApplication getWalletApplication() {
        return (WalletApplication) getActivity().getApplication();
    }
}
