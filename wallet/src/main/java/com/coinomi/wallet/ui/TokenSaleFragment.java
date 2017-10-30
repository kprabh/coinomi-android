package com.coinomi.wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.Unbinder;
import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.FiatType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.sponsor.Sponsors.Sponsor;
import com.coinomi.sponsor.Sponsors.TokenSale;
import com.coinomi.sponsor.TokenSaleProxy;
import com.coinomi.sponsor.TokenSaleProxy.SaleDeposit;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.ExchangeRatesProvider.ExchangeRate;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.Dialogs.ProgressDialogFragment;
import com.coinomi.wallet.ui.adaptors.AvailableAccountsAdaptor;
import com.coinomi.wallet.ui.adaptors.AvailableAccountsAdaptor.Entry;
import com.coinomi.wallet.ui.widget.AmountEditView;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.WeakHandler;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenSaleFragment extends AbstractSendFragment {
    private static final Logger log = LoggerFactory.getLogger(TokenSaleFragment.class);
    @BindView(2131689797)
    CheckBox agreeTerms;
    @BindView(2131689754)
    TextView amountError;
    @BindView(2131689755)
    TextView amountWarning;
    private final com.coinomi.wallet.ui.widget.AmountEditView.Listener amountsListener = new C05051();
    @BindView(2131689798)
    Button buttonParticipate;
    private CoinType createToType;
    private DepositInfoTask depositInfoTask;
    private AvailableAccountsAdaptor destinationAdapter;
    @BindView(2131689793)
    View destinationLayout;
    @BindView(2131689794)
    Spinner destinationSpinner;
    @BindView(2131689788)
    TextView details;
    private WalletAccount fromAccount;
    protected final MyHandler handler = new MyHandler(this);
    private Listener listener;
    private TokenSale sale;
    @BindView(2131689795)
    AmountEditView sendCoinAmountView;
    private boolean singleAccountSale;
    private AvailableAccountsAdaptor sourceAdapter;
    @BindView(2131689792)
    Spinner sourceSpinner;
    private Sponsor sponsor;
    private WalletAccount toAccount;
    private Unbinder unbinder;
    @BindView(2131689790)
    Button viewTems;
    @BindView(2131689789)
    Button visitSite;

    public interface Listener {
        void onParticipate(WalletAccount walletAccount, SaleDeposit saleDeposit, Value value);
    }

    class C05051 implements com.coinomi.wallet.ui.widget.AmountEditView.Listener {
        C05051() {
        }

        public void changed() {
            TokenSaleFragment.this.validateAmount(true);
        }

        public void focusChanged(boolean hasFocus) {
            if (!hasFocus) {
                TokenSaleFragment.this.validateAmount();
            }
        }
    }

    private static class DepositInfoTask extends AsyncTask<Void, Void, SaleDeposit> {
        final Handler handler;
        private final AbstractAddress receive;
        private final AbstractAddress refund;
        private final TokenSale sale;
        final TokenSaleProxy saleProxy;

        private DepositInfoTask(Handler handler, TokenSaleProxy saleProxy, TokenSale sale, AbstractAddress receive, AbstractAddress refund) {
            this.handler = handler;
            this.saleProxy = saleProxy;
            this.sale = sale;
            this.receive = receive;
            this.refund = refund;
        }

        protected void onPreExecute() {
            this.handler.sendEmptyMessage(2);
        }

        protected SaleDeposit doInBackground(Void... params) {
            try {
                return this.saleProxy.getDepositInfo(this.sale, this.receive, this.refund);
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(SaleDeposit deposit) {
            if (deposit != null) {
                this.handler.sendMessage(this.handler.obtainMessage(0, deposit));
            } else {
                this.handler.sendEmptyMessage(1);
            }
        }
    }

    protected static class MyHandler extends WeakHandler<TokenSaleFragment> {
        public MyHandler(TokenSaleFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(TokenSaleFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.onDepositAddressUpdate((SaleDeposit) msg.obj);
                    return;
                case 1:
                    ref.onDepositAddressUpdateError();
                    return;
                case 2:
                    ref.onDepositAddressStart();
                    return;
                default:
                    return;
            }
        }
    }

    private void onDepositAddressStart() {
        ProgressDialogFragment.show(getFragmentManager(), getString(R.string.token_sale_contacting), "deposit_address_work_dialog_tag");
    }

    private void onDepositAddressUpdate(SaleDeposit address) {
        if (!Dialogs.dismissAllowingStateLoss(getFragmentManager(), "deposit_address_work_dialog_tag")) {
            this.listener.onParticipate(this.fromAccount, address, this.validSendAmount);
            this.depositInfoTask = null;
            this.state = State.INPUT;
        }
    }

    private void onDepositAddressUpdateError() {
        if (!Dialogs.dismissAllowingStateLoss(getFragmentManager(), "deposit_address_work_dialog_tag")) {
            Toast.makeText(getContext(), R.string.stopped_network_error, Toast.LENGTH_LONG).show();
            this.depositInfoTask = null;
            this.state = State.INPUT;
        }
    }

    public void updateView() {
    }

    public WalletAccount getAccount() {
        return this.fromAccount;
    }

    public static TokenSaleFragment getInstance(Sponsor sponsor) {
        Preconditions.checkArgument(sponsor.tokenSale != null, "Sponsor must have a TokenSale object");
        TokenSaleFragment fragment = new TokenSaleFragment();
        fragment.setArguments(new Bundle());
        fragment.getArguments().putSerializable("sponsor", sponsor);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.sponsor = (Sponsor) getArguments().getSerializable("sponsor");
        }
        if (this.sponsor == null || this.sponsor.tokenSale == null) {
            Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
        this.sale = (TokenSale) Preconditions.checkNotNull(this.sponsor.tokenSale);
        this.singleAccountSale = this.sale.receiveTypes == null;
        for (ExchangeRate rate : ExchangeRatesProvider.getRates(getActivity(), this.config.getExchangeCurrencyCode()).values()) {
            this.localRates.put(rate.currencyCodeId, rate.rate);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_token_sale, container, false);
        this.unbinder = ButterKnife.bind((Object) this, view);
        if (this.sale.details != null) {
            this.details.setText(this.sale.details);
            this.details.setVisibility(View.VISIBLE);
        }
        if (this.sponsor.link != null) {
            this.visitSite.setVisibility(View.VISIBLE);
        } else {
            this.visitSite.setVisibility(View.INVISIBLE);
        }
        if (this.sale.terms != null) {
            this.viewTems.setVisibility(View.VISIBLE);
            this.agreeTerms.setVisibility(View.VISIBLE);
            this.buttonParticipate.setEnabled(this.agreeTerms.isChecked());
        } else {
            this.viewTems.setVisibility(View.INVISIBLE);
            this.agreeTerms.setVisibility(View.INVISIBLE);
            this.agreeTerms.setChecked(true);
            this.buttonParticipate.setEnabled(true);
        }
        this.sourceSpinner.setAdapter(getSourceSpinnerAdapter(this.sale.sendTypes));
        if (this.singleAccountSale) {
            this.destinationLayout.setVisibility(View.GONE);
        } else {
            this.destinationSpinner.setAdapter(getDestinationSpinnerAdapter(this.sale.receiveTypes));
        }
        AmountEditView sendLocalAmountView = (AmountEditView) ButterKnife.findById(view, (int) R.id.local_amount);
        sendLocalAmountView.resetType(FiatType.get(this.config.getExchangeCurrencyCode()), true);
        this.amountCalculatorLink = new CurrencyCalculatorLink(this.sendCoinAmountView, sendLocalAmountView);
        return view;
    }

    public void onDestroyView() {
        this.unbinder.unbind();
        super.onDestroyView();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    public void onResume() {
        super.onResume();
        this.amountCalculatorLink.setListener(this.amountsListener);
    }

    public void onPause() {
        this.amountCalculatorLink.setListener(null);
        super.onPause();
    }

    protected void showAmountError(String message) {
        this.amountError.setText(message);
        this.amountError.setVisibility(View.VISIBLE);
    }

    protected void showAmountError(int resId) {
        this.amountError.setText(resId);
        this.amountError.setVisibility(View.VISIBLE);
    }

    protected void showAmountWarning(int resId) {
        this.amountWarning.setText(resId);
        this.amountWarning.setVisibility(View.VISIBLE);
    }

    protected void hideAmountError() {
        this.amountError.setVisibility(View.GONE);
    }

    protected void hideAmountWarning() {
        this.amountWarning.setVisibility(View.GONE);
    }

    @OnClick({2131689798})
    void onParticipateClick() {
        if (this.state != State.INPUT) {
            log.warn("Cannot proceed as the state is not " + State.INPUT);
        } else if (!this.agreeTerms.isChecked()) {
            Toast.makeText(getActivity(), R.string.must_accept_terms, Toast.LENGTH_LONG).show();
        } else if (this.fromAccount == null) {
            Toast.makeText(getActivity(), R.string.no_wallet_pocket_selected, Toast.LENGTH_LONG).show();
        } else if (this.toAccount == null) {
            createAccount(this.createToType);
        } else {
            validateAmount();
            if (isAmountValid(this.validSendAmount)) {
                Keyboard.hideKeyboard(getActivity());
                this.state = State.PREPARATION;
                if (this.depositInfoTask == null) {
                    this.depositInfoTask = new DepositInfoTask(this.handler, this.application.getTokenSaleProxy(), this.sale, this.toAccount.getReceiveAddress(), this.fromAccount.getReceiveAddress());
                    this.depositInfoTask.execute(new Void[0]);
                    return;
                }
                return;
            }
            Toast.makeText(getActivity(), R.string.amount_error, Toast.LENGTH_LONG).show();
        }
    }

    @OnClick({2131689789})
    void onVisitSiteClick() {
        getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse(((URI) Preconditions.checkNotNull(this.sponsor.link)).toString())));
    }

    @OnClick({2131689790})
    void onViewTermsClick() {
        getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse(((URI) Preconditions.checkNotNull(this.sale.terms)).toString())));
    }

    @OnCheckedChanged({2131689797})
    void onAgreeChecked(boolean checked) {
        this.buttonParticipate.setEnabled(checked);
    }

    @OnItemSelected({2131689794})
    void onToSelected(AdapterView<?> parent) {
        Entry entry = (Entry) parent.getSelectedItem();
        this.toAccount = entry.getAccount();
        if (this.toAccount != null) {
            this.createToType = null;
        } else {
            this.createToType = entry.getType();
        }
    }

    @OnItemSelected({2131689792})
    void onFromSelected(AdapterView<?> parent) {
        this.fromAccount = (WalletAccount) Preconditions.checkNotNull(((Entry) parent.getSelectedItem()).getAccount());
        updateBalance();
        if (this.singleAccountSale) {
            this.toAccount = this.fromAccount;
        }
        this.sendCoinAmountView.resetType(this.fromAccount.getCoinType(), true);
        setExchangeRate(getCurrentRate());
    }

    @OnClick({2131689751})
    public void onEmptyWalletClick() {
        setAmountForEmptyWallet();
    }

    private AvailableAccountsAdaptor getDestinationSpinnerAdapter(List<CoinType> receiveTypes) {
        if (this.destinationAdapter == null) {
            this.destinationAdapter = new AvailableAccountsAdaptor(getActivity());
            this.destinationAdapter.update(((WalletApplication) getActivity().getApplication()).getAllAccounts(), receiveTypes, true);
        }
        return this.destinationAdapter;
    }

    private AvailableAccountsAdaptor getSourceSpinnerAdapter(List<CoinType> sendTypes) {
        if (this.sourceAdapter == null) {
            this.sourceAdapter = new AvailableAccountsAdaptor(getActivity());
            this.sourceAdapter.update(((WalletApplication) getActivity().getApplication()).getAllAccounts(), sendTypes, false);
        }
        return this.sourceAdapter;
    }

    protected void setPrimaryAmount(Value balance) {
        this.amountCalculatorLink.setPrimaryAmount(balance);
    }

    protected Value getPrimaryAmount() {
        return this.amountCalculatorLink.getPrimaryAmount();
    }

    protected void setExchangeRate(com.coinomi.core.util.ExchangeRate rate) {
        this.amountCalculatorLink.setExchangeRate(rate);
    }

    protected void accountAdded(WalletAccount newAccount) {
        this.toAccount = newAccount;
        this.createToType = null;
        this.destinationAdapter.updateAccount(newAccount);
        onParticipateClick();
    }
}
