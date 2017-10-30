package com.coinomi.wallet.ui;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.eth.CallTransaction;
import com.coinomi.core.coins.families.EthFamily;
import com.coinomi.core.exceptions.NoSuchPocketException;
import com.coinomi.core.exceptions.TransactionBroadcastException;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftAmountTx;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftNormalTx;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftTime;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.families.bitcoin.TransactionWatcherWallet;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletAccount.WalletAccountException;
import com.coinomi.core.wallet.families.bitcoin.TransactionWatcherWallet;
import com.coinomi.core.wallet.families.bitcoin.TxFeeEventListener;
import com.coinomi.core.wallet.families.eth.ERC20Token;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.ExchangeHistoryProvider;
import com.coinomi.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.widget.SendOutput;
import com.coinomi.wallet.ui.widget.TransactionAmountVisualizer;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.PoweredByUtil;
import com.coinomi.wallet.util.WalletUtils;
import com.coinomi.wallet.util.WeakHandler;
import java.util.HashMap;
import org.acra.ACRA;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.HashMap;

import javax.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.Unbinder;

import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.core.Preconditions.checkState;
import static com.coinomi.wallet.Constants.ARG_ACCOUNT_ID;
import static com.coinomi.wallet.Constants.ARG_EMPTY_WALLET;
import static com.coinomi.wallet.Constants.ARG_SEND_REQUEST;
import static com.coinomi.wallet.Constants.ARG_SEND_TO_ACCOUNT_ID;
import static com.coinomi.wallet.Constants.ARG_SEND_TO_ADDRESS;
import static com.coinomi.wallet.Constants.ARG_SEND_VALUE;
import static com.coinomi.wallet.Constants.ARG_TX_MESSAGE;

/**
 * This fragment displays a busy message and makes the transaction in the background
 *
 */
public class MakeTransactionFragment extends Fragment implements TxFeeEventListener {
    private static final Logger log = LoggerFactory.getLogger(MakeTransactionFragment.class);

    private static final int START_TRADE_TIMEOUT = 0;
    private static final int UPDATE_TRADE_TIMEOUT = 1;
    private static final int TRADE_EXPIRED = 2;
    private static final int STOP_TRADE_TIMEOUT = 3;

    private static final int SAFE_TIMEOUT_MARGIN_SEC = 60;

    // Loader IDs
    private static final int ID_RATE_LOADER = 0;

    private static final String TRANSACTION_BROADCAST = "transaction_broadcast";
    private static final String ERROR = "error";
    private static final String EXCHANGE_ENTRY = "exchange_entry";
    private static final String DEPOSIT_ADDRESS = "deposit_address";
    private static final String DEPOSIT_AMOUNT = "deposit_amount";
    private static final String WITHDRAW_ADDRESS = "withdraw_address";
    private static final String WITHDRAW_AMOUNT = "withdraw_amount";

    private static final String PREPARE_TRANSACTION_BUSY_DIALOG_TAG = "prepare_transaction_busy_dialog_tag";
    private static final String SIGNING_TRANSACTION_BUSY_DIALOG_TAG = "signing_transaction_busy_dialog_tag";


    private ContentResolver contentResolver;
    private CreateTransactionTask createTransactionTask;
    private WalletApplication application;
    private Configuration config;
    private String contractData;
    boolean emptyWallet;
    @Nullable private Exception error;
    private CountDownTimer countDownTimer;

    @BindView(R.id.transaction_info) TextView transactionInfo;
    @BindView(R.id.password) EditText passwordView;
    @BindView(R.id.transaction_amount_visualizer) TransactionAmountVisualizer txVisualizer;
    boolean sendingToAccount;
    private boolean staticRequest = false;
    @BindView(R.id.transaction_trade_withdraw) SendOutput tradeWithdrawSendOutput;
    @BindView(2131689709)
    View changeFeesView;
    @BindView(2131689710)
    Spinner feePriority;
    private String exchange;
    private ExchangeEntry exchangeEntry;
    private Handler handler = new MyHandler(this);
    private Value lastFee;
    private Listener listener;
    private HashMap<String, ExchangeRate> localRates = new HashMap();
    private ShapeShiftMarketInfo marketInfo;
    private String password;
    @BindView(2131689820)
    TextView poweredBy;
    private SendRequest request;
    private Value sendAmount;
    AbstractAddress sendToAddress;
    private CoinType sendToType;
    private SignAndBroadcastTask signAndBroadcastTask;
    private AbstractWallet sourceAccount;
    private CoinType sourceType;
    private AbstractAddress tradeDepositAddress;
    private Value tradeDepositAmount;
    private AbstractAddress tradeWithdrawAddress;
    private Value tradeWithdrawAmount;
    private boolean transactionBroadcast = false;
    private TxMessage txMessage;
    private Unbinder unbinder;

    public static MakeTransactionFragment newInstance(Bundle args) {
        MakeTransactionFragment fragment = new MakeTransactionFragment();
        fragment.setArguments(args);
        return fragment;
    }
    public MakeTransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        signAndBroadcastTask = null;

        setRetainInstance(true); // To handle async tasks

        Bundle args = getArguments();
        checkNotNull(args, "Must provide arguments");

        try {
            if (args.containsKey(ARG_SEND_REQUEST)) {
                request = (SendRequest) checkNotNull(args.getSerializable(ARG_SEND_REQUEST));
                checkState(request.isCompleted(), "Only completed requests are currently supported.");
                checkState(request.tx.getSentTo().size() == 1, "Only one output is currently supported");
                sendToAddress = request.tx.getSentTo().get(0).getAddress();
                sourceType = request.type;
                staticRequest = true;
                preloadCachedRates();
                return;
            }

            String fromAccountId = args.getString(ARG_ACCOUNT_ID);
            sourceAccount = (AbstractWallet) checkNotNull(application.getAccount(fromAccountId));
            application.maybeConnectAccount(sourceAccount);
            if (args.containsKey("send_from_coin_type")) {
                this.sourceType = (CoinType) Preconditions.checkNotNull(args.getSerializable("send_from_coin_type"));
            } else {
                this.sourceType = this.sourceAccount.getCoinType();
            }
            this.emptyWallet = args.getBoolean("empty_wallet", false);
            this.sendAmount = (Value) args.getSerializable("send_value");
            if (!this.emptyWallet || this.sendAmount == null) {
                if (args.containsKey("send_to_account_id")) {
                    String toAccountId = args.getString("send_to_account_id");
                    this.sendToType = (CoinType) args.getSerializable("send_to_coin_type");
                    AbstractWallet toAccount = (AbstractWallet) Preconditions.checkNotNull(this.application.getAccount(toAccountId));
                    if (this.sendToType instanceof ERC20Token) {
                        this.sendToAddress = this.sendToType.newAddress(toAccount.getReceiveAddress(this.config.isManualAddressManagement()).toString());
                    } else {
                        this.sendToAddress = toAccount.getReceiveAddress(this.config.isManualAddressManagement());
                    }
                    this.sendingToAccount = true;
                } else {
                    this.sendToAddress = (AbstractAddress) Preconditions.checkNotNull(args.getSerializable("send_to_address"));
                    this.sendingToAccount = false;
                }

            txMessage = (TxMessage) args.getSerializable(ARG_TX_MESSAGE);
                if (args.containsKey("contract_data")) {
                    this.contractData = args.getString("contract_data");
                    if (this.contractData != null && this.contractData.startsWith("0x")) {
                        this.contractData = this.contractData.replace("0x", "");
                    }
                }
                if (args.containsKey("exchange_id")) {
                    this.exchange = args.getString("exchange_id");
                }
            if (savedState != null) {
                error = (Exception) savedState.getSerializable(ERROR);
                transactionBroadcast = savedState.getBoolean(TRANSACTION_BROADCAST);
                exchangeEntry = (ExchangeEntry) savedState.getSerializable(EXCHANGE_ENTRY);
                tradeDepositAddress = (AbstractAddress) savedState.getSerializable(DEPOSIT_ADDRESS);
                tradeDepositAmount = (Value) savedState.getSerializable(DEPOSIT_AMOUNT);
                tradeWithdrawAddress = (AbstractAddress) savedState.getSerializable(WITHDRAW_ADDRESS);
                tradeWithdrawAmount = (Value) savedState.getSerializable(WITHDRAW_AMOUNT);
            }
            preloadCachedRates();
            maybeStartCreateTransaction();
                return;
            }
            throw new IllegalArgumentException("Cannot set 'empty wallet' and 'send amount' at the same time");
        } catch (Exception e) {
            error = e;
            if (listener != null) {
                listener.onSignResult(e, null);
            }
        }

//        String localSymbol = config.getExchangeCurrencyCode();
//        for (ExchangeRatesProvider.ExchangeRate rate : getRates(getActivity(), localSymbol).values()) {
//            localRates.put(rate.currencyCodeId, rate.rate);
//        }
    }
    private void preloadCachedRates() {
        for (ExchangeRatesProvider.ExchangeRate rate : ExchangeRatesProvider.getRates(getActivity(), this.config.getExchangeCurrencyCode()).values()) {
            this.localRates.put(rate.currencyCodeId, rate.rate);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_make_transaction, container, false);
        unbinder = ButterKnife.bind(this, view);

        if (error != null) return view;
        if (!this.staticRequest && this.sourceType.hasSelectableFees()) {
            this.changeFeesView.setVisibility(View.VISIBLE);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.fee_priority_array, 17367048);
            adapter.setDropDownViewResource(17367049);
            this.feePriority.setAdapter(adapter);
            this.feePriority.setSelection(2);
        }
        transactionInfo.setVisibility(View.GONE);

        final TextView passwordLabelView = (TextView) view.findViewById(R.id.enter_password_label);
        if (sourceAccount != null && sourceAccount.isEncrypted()) {
            passwordView.requestFocus();
            passwordView.setVisibility(View.VISIBLE);
            passwordLabelView.setVisibility(View.VISIBLE);
        } else {
            passwordView.setVisibility(View.GONE);
            passwordLabelView.setVisibility(View.GONE);
        }

        tradeWithdrawSendOutput.setVisibility(View.GONE);
        showTransaction();

        if (isExchangeNeeded()) {
            if (this.exchange == null) {
                this.exchange = this.application.getShapeShift().getExchange();
            }
            PoweredByUtil.setup(getContext(), this.exchange, this.poweredBy);
            this.poweredBy.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
    @OnItemSelected({2131689710})
    public void onFeePrioritySelected(AdapterView<?> adapterView, View view, int pos, long id) {
        if (this.sourceAccount instanceof TransactionWatcherWallet) {
            TransactionWatcherWallet account = (TransactionWatcherWallet)this.sourceAccount;
            switch (pos) {
                case 0:
                    onSetFee(this.sourceType.getFeeValue());
                    return;
                case 1:
                    account.estimateFee(24, this);
                    return;
                case 3:
                    account.estimateFee(3, this);
                    return;
                default:
                    account.estimateFee(12, this);
                    return;
            }
        }
    }

    public void onFeeEstimate(Value feeValue) {
        this.handler.sendMessage(this.handler.obtainMessage(4, feeValue));
    }

    private void onSetFee(Value newFee) {
        Preconditions.checkState(!this.staticRequest);
        this.lastFee = newFee;
        if (this.lastFee.isNegative()) {
            Toast.makeText(getActivity(), R.string.estimating_fees_error, Toast.LENGTH_LONG).show();
        }
        if (this.request != null) {
            this.request.reset();
            this.request.setFeePerTxSize(newFee);
            try {
                this.sourceAccount.completeTransaction(this.request);
                if (isExchangeNeeded() && this.marketInfo != null) {
                    this.tradeWithdrawAmount = this.marketInfo.rate.convert(this.request.getTx(true).getValue(this.sourceAccount).negate().subtract(this.request.getTx(true).getFee()));
                }
                showTransaction();
            } catch (Throwable e) {
                String errorMessage = WalletUtils.getErrorMessage(getContext(), e);
                if (errorMessage == null) {
                    if (ACRA.isInitialised()) {
                        ACRA.getErrorReporter().handleSilentException(e);
                    }
                    log.error("An unknown error occurred while sending coins", e);
                    errorMessage = getString(R.string.send_coins_error, e.getMessage());
                }
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
    @OnClick(R.id.button_confirm)
    void onConfirmClick() {
        if (passwordView.isShown()) {
            Keyboard.hideKeyboard(getActivity());
            password = passwordView.getText().toString();
        }
        maybeStartSignAndBroadcast();
    }

    private void showTransaction() {
        if (request != null && txVisualizer != null) {
            if (this.sourceType instanceof ERC20Token) {
                txVisualizer.setContractTransaction(this.sourceAccount, this.request.getTx(), this.tradeDepositAmount);
            } else {
                this.txVisualizer.setTransaction(this.sourceAccount, this.request.getTx());
            }

            if (tradeWithdrawAmount != null && tradeWithdrawAddress != null) {
                tradeWithdrawSendOutput.setVisibility(View.VISIBLE);
                if (sendingToAccount) {
                    tradeWithdrawSendOutput.setSending(false);
                } else {
                    tradeWithdrawSendOutput.setSending(true);
                    tradeWithdrawSendOutput.setLabelAndAddress(tradeWithdrawAddress);
                }
                tradeWithdrawSendOutput.setAmount(GenericUtils.formatValue(tradeWithdrawAmount));
                tradeWithdrawSendOutput.setSymbol(tradeWithdrawAmount.type.getSymbol());
                txVisualizer.getOutputs().get(0).setSendLabel(getString(R.string.trade));
                txVisualizer.hideAddresses(); // Hide exchange address
            }
            updateLocalRates();
        }
    }

    boolean isExchangeNeeded() {
        return !sourceType.equals(sendToAddress.getType());
    }

    private void maybeStartCreateTransaction() {
        if (createTransactionTask == null && !transactionBroadcast && error == null) {
            createTransactionTask = new CreateTransactionTask();
            createTransactionTask.execute();
        } else if (createTransactionTask != null && createTransactionTask.getStatus() == AsyncTask.Status.FINISHED) {
            Dialogs.dismissAllowingStateLoss(getFragmentManager(), PREPARE_TRANSACTION_BUSY_DIALOG_TAG);
        }
    }

    private SendRequest generateSendRequest(AbstractAddress sendTo, boolean emptyWallet,
                                            @Nullable Value amount, @Nullable TxMessage txMessage)
            throws WalletAccount.WalletAccountException {

        SendRequest sendRequest;
        byte[] bArr = null;
        if (this.sourceType instanceof ERC20Token) {
            try {
                EthContract contract = (EthContract) ((EthFamilyWallet) this.sourceAccount).getAllContracts().get(((ERC20Token) this.sourceType).getAddress());
                this.contractData = Hex.toHexString(CallTransaction.createCallTransaction(0, 1, 1000000, ((ERC20Token) this.sourceType).getAddress().replace("0x", ""), 0, contract.getContract().getByName(contract.getContract().getByName("transfer").name), (Object[]) new String[]{sendTo.toString(), amount.getBigInt().toString()}).getData());
                sendTo = this.sourceAccount.getCoinType().newAddress(((ERC20Token) this.sourceType).getAddress());
                amount = this.sourceAccount.getCoinType().zeroCoin();
            } catch (Exception e) {
            }
        }
        if (emptyWallet) {
            sendRequest = sourceAccount.getEmptyWalletRequest(sendTo, contractData == null ? null : Hex.decode(this.contractData));
        } else {            AbstractWallet abstractWallet = this.sourceAccount;
            Value value = (Value) checkNotNull(amount);
            if (this.contractData != null) {
                bArr = Hex.decode(this.contractData);
            }
            sendRequest = sourceAccount.getSendToRequest(sendTo, value, bArr);
        }
        sendRequest.txMessage = txMessage;
        sendRequest.signTransaction = false;
        if (!this.sourceType.hasDynamicFees() || (this.sourceType instanceof EthFamily) || (this.sourceType instanceof ERC20Token)) {
            this.sourceAccount.completeTransaction(sendRequest);
        }
        if (this.sourceType.hasDynamicFees() && this.lastFee != null) {
            sendRequest.setFeePerTxSize(this.lastFee);
            this.sourceAccount.completeTransaction(sendRequest);
        }
        return sendRequest;
    }

    private boolean isSendingFromSourceAccount() {
        return isEmptyWallet() || (sendAmount != null && sourceType.equals(sendAmount.type));
    }

    private boolean isEmptyWallet() {
        return emptyWallet && sendAmount == null;
    }

    private void maybeStartSignAndBroadcast() {
        if (signAndBroadcastTask == null && !transactionBroadcast && request != null && error == null) {
            signAndBroadcastTask = new SignAndBroadcastTask();
            signAndBroadcastTask.execute();
        } else if (transactionBroadcast) {
            Dialogs.dismissAllowingStateLoss(getFragmentManager(), SIGNING_TRANSACTION_BUSY_DIALOG_TAG);
            Toast.makeText(getActivity(), R.string.tx_already_broadcast, Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onSignResult(error, exchangeEntry);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(TRANSACTION_BROADCAST, transactionBroadcast);
        outState.putSerializable(ERROR, error);
        if (isExchangeNeeded()) {
            outState.putSerializable(EXCHANGE_ENTRY, exchangeEntry);
            outState.putSerializable(DEPOSIT_ADDRESS, tradeDepositAddress);
            outState.putSerializable(DEPOSIT_AMOUNT, tradeDepositAmount);
            outState.putSerializable(WITHDRAW_ADDRESS, tradeWithdrawAddress);
            outState.putSerializable(WITHDRAW_AMOUNT, tradeWithdrawAmount);
        }
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            listener = (Listener) context;
            contentResolver = context.getContentResolver();
            application = (WalletApplication) context.getApplicationContext();
            config = application.getConfiguration();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(ID_RATE_LOADER, null, rateLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getLoaderManager().destroyLoader(ID_RATE_LOADER);
        listener = null;
        onStopTradeCountDown();
    }


    void onStartTradeCountDown(int secondsLeft) {
        if (countDownTimer != null) return;

        countDownTimer = new CountDownTimer(secondsLeft * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                handler.sendMessage(handler.obtainMessage(
                        UPDATE_TRADE_TIMEOUT, (int) (millisUntilFinished / 1000)));
            }

            public void onFinish() {
                handler.sendEmptyMessage(TRADE_EXPIRED);
            }
        };

        countDownTimer.start();
    }

    void onStopTradeCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            handler.removeMessages(START_TRADE_TIMEOUT);
            handler.removeMessages(UPDATE_TRADE_TIMEOUT);
            handler.removeMessages(TRADE_EXPIRED);
        }
    }

    private void onTradeExpired() {
        if (transactionBroadcast) { // Transaction already sent, so the trade is not expired
            return;
        }
        if (transactionInfo.getVisibility() != View.VISIBLE) {
            transactionInfo.setVisibility(View.VISIBLE);
        }
        String errorString = getString(R.string.trade_expired);
        transactionInfo.setText(errorString);

        if (listener != null) {
            error = new Exception(errorString);
            listener.onSignResult(error, null);
        }
    }


    private void onUpdateTradeCountDown(int secondsRemaining) {
        if (transactionInfo.getVisibility() != View.VISIBLE) {
            transactionInfo.setVisibility(View.VISIBLE);
        }

        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;

        Resources res = getResources();
        String timeLeft;

        if (minutes > 0) {
            timeLeft = res.getQuantityString(R.plurals.tx_confirm_timer_minute,
                    minutes, String.format("%d:%02d", minutes, seconds));
        } else {
            timeLeft = res.getQuantityString(R.plurals.tx_confirm_timer_second,
                    seconds, seconds);
        }

        String message = getString(R.string.tx_confirm_timer_message, timeLeft);
        transactionInfo.setText(message);
    }

    /**
     * Makes a call to ShapeShift about the time left for the trade
     *
     * Note: do not call this from the main thread!
     */
    @Nullable
    private static ShapeShiftTime getTimeLeftSync(ShapeShift shapeShift, AbstractAddress address) {
        // Try 3 times
        for (int tries = 1; tries <= 3; tries++) {
            try {
                log.info("Getting time left for: {}", address);
                return shapeShift.getTime(address);
            } catch (Exception e) {
                log.info("Will retry: {}", e.getMessage());
                    /* ignore and retry, with linear backoff */
                try {
                    Thread.sleep(1000 * tries);
                } catch (InterruptedException ie) { /*ignored*/ }
            }
        }
        return null;
    }

    private void updateLocalRates() {
        if (localRates != null) {
            if (txVisualizer != null && localRates.containsKey(sourceType.getSymbol())) {
                ExchangeRate baseRate = null;
                if (this.sourceAccount != null) {
                    baseRate = (ExchangeRate) this.localRates.get(this.sourceAccount.getCoinType().getSymbol());
            }
                this.txVisualizer.setExchangeRate((ExchangeRate) this.localRates.get(this.sourceType.getSymbol()), baseRate);
            }
            if (tradeWithdrawAmount != null && localRates.containsKey(tradeWithdrawAmount.type.getSymbol())) {
                ExchangeRate rate = localRates.get(tradeWithdrawAmount.type.getSymbol());
                Value fiatAmount = rate.convert(tradeWithdrawAmount);
                tradeWithdrawSendOutput.setAmountLocal(GenericUtils.formatFiatValue(fiatAmount));
                tradeWithdrawSendOutput.setSymbolLocal(fiatAmount.type.getSymbol());
            }
        }
    }

    private void updateLocalRates(HashMap<String, ExchangeRate> rates) {
        localRates = rates;
        updateLocalRates();
    }


    public interface Listener {
        void onSignResult(@Nullable Exception error, @Nullable ExchangeEntry exchange);
    }

    private final LoaderCallbacks<Cursor> rateLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
            String localSymbol = config.getExchangeCurrencyCode();
            return new ExchangeRateLoader(getActivity(), config, localSymbol);
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
            if (data != null && data.getCount() > 0) {
                HashMap<String, ExchangeRate> rates = new HashMap<>(data.getCount());
                data.moveToFirst();
                do {
                    ExchangeRatesProvider.ExchangeRate rate = ExchangeRatesProvider.getExchangeRate(data);
                    rates.put(rate.currencyCodeId, rate.rate);
                } while (data.moveToNext());

                updateLocalRates(rates);
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
        }
    };

    /**
     * The fragment handler
     */
    private static class MyHandler extends WeakHandler<MakeTransactionFragment> {
        public MyHandler(MakeTransactionFragment referencingObject) { super(referencingObject); }

        @Override
        protected void weakHandleMessage(MakeTransactionFragment ref, Message msg) {
            switch (msg.what) {
                case START_TRADE_TIMEOUT:
                    ref.onStartTradeCountDown((int) msg.obj);
                    break;
                case UPDATE_TRADE_TIMEOUT:
                    ref.onUpdateTradeCountDown((int) msg.obj);
                    break;
                case TRADE_EXPIRED:
                    ref.onTradeExpired();
                    break;
                case STOP_TRADE_TIMEOUT:
                    ref.onStopTradeCountDown();
                    break;
                case 4:
                    ref.onSetFee((Value) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    private class CreateTransactionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            // Show dialog as we need to make network connections
            if (isExchangeNeeded()) {
                Dialogs.ProgressDialogFragment.show(getFragmentManager(),
                        getString(R.string.contacting_exchange),
                        PREPARE_TRANSACTION_BUSY_DIALOG_TAG);
            } else if (MakeTransactionFragment.this.sourceType.hasDynamicFees()) {
                Dialogs.ProgressDialogFragment.show(MakeTransactionFragment.this.getFragmentManager(), MakeTransactionFragment.this.getString(R.string.estimating_fees), "prepare_transaction_busy_dialog_tag");
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (isExchangeNeeded()) {

                    ShapeShift shapeShift = application.getShapeShift();
                    AbstractAddress refundAddress =
                            sourceAccount.getRefundAddress(config.isManualAddressManagement());

                    // If emptying wallet or the amount is the same type as the source account
                    if (isSendingFromSourceAccount()) {
                        ShapeShiftMarketInfo marketInfo = shapeShift.getMarketInfo(
                                sourceType, sendToAddress.getType());

                        // If no values set, make the call
                        if (tradeDepositAddress == null || tradeDepositAmount == null ||
                                tradeWithdrawAddress == null || tradeWithdrawAmount == null) {
                            ShapeShiftNormalTx normalTx =
                                    shapeShift.exchange(sendToAddress, refundAddress);
                            // TODO, show a retry message
                            if (normalTx.isError) throw new Exception(normalTx.errorMessage);
                            tradeDepositAddress = normalTx.deposit;
                            tradeDepositAmount = sendAmount;
                            tradeWithdrawAddress = sendToAddress;
                            // set tradeWithdrawAmount after we generate the send tx
                        }

                        request = generateSendRequest(tradeDepositAddress, isEmptyWallet(),
                                tradeDepositAmount, txMessage);

                        // The amountSending could be equal to sendAmount or the actual amount if
                        // emptying the wallet
                        Value amountSending = request.tx.getValue(sourceAccount).negate().subtract(request.tx.getFee());
                        tradeWithdrawAmount = marketInfo.rate.convert(amountSending);
                    } else {
                        // If no values set, make the call
                        if (tradeDepositAddress == null || tradeDepositAmount == null ||
                                tradeWithdrawAddress == null || tradeWithdrawAmount == null) {
                            ShapeShiftAmountTx fixedAmountTx =
                                    shapeShift.exchangeForAmount(sendAmount, sendToAddress, refundAddress);
                            // TODO, show a retry message
                            if (fixedAmountTx.isError) throw new Exception(fixedAmountTx.errorMessage);
                            tradeDepositAddress = fixedAmountTx.deposit;
                            tradeDepositAmount = fixedAmountTx.depositAmount;
                            tradeWithdrawAddress = fixedAmountTx.withdrawal;
                            tradeWithdrawAmount = fixedAmountTx.withdrawalAmount;
                        }

                        ShapeShiftTime time = getTimeLeftSync(shapeShift, tradeDepositAddress);
                        if (time != null && !time.isError) {
                            int secondsLeft = time.secondsRemaining - SAFE_TIMEOUT_MARGIN_SEC;
                            handler.sendMessage(handler.obtainMessage(
                                    START_TRADE_TIMEOUT, secondsLeft));
                        } else {
                            throw new Exception(time == null ? "Error getting trade expiration time" : time.errorMessage);
                        }
                        request = generateSendRequest(tradeDepositAddress, false,
                                tradeDepositAmount, txMessage);
                    }
                } else {
                    request = generateSendRequest(sendToAddress, isEmptyWallet(),
                            sendAmount, txMessage);
                }
            } catch (Exception e) {
                error = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (Dialogs.dismissAllowingStateLoss(getFragmentManager(), PREPARE_TRANSACTION_BUSY_DIALOG_TAG)) return;

            if (error != null && listener != null) {
                listener.onSignResult(error, null);
            } else if (error == null) {
                showTransaction();
            } else {
                log.warn("Error occurred while creating transaction", error);
            }
        }
    }

    private class SignAndBroadcastTask extends AsyncTask<Void, Void, Exception> {
        @Override
        protected void onPreExecute() {
            Dialogs.ProgressDialogFragment.show(getFragmentManager(),
                    getString(R.string.preparing_transaction),
                    SIGNING_TRANSACTION_BUSY_DIALOG_TAG);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            Wallet wallet = MakeTransactionFragment.this.application.getWallet();
            if (wallet == null) {
                return new NoSuchPocketException("No wallet found.");
            }
            try {
                if (MakeTransactionFragment.this.sourceAccount != null) {
                    if (wallet.isEncrypted()) {
                        KeyCrypter crypter = (KeyCrypter) Preconditions.checkNotNull(wallet.getKeyCrypter());
                        MakeTransactionFragment.this.request.aesKey = crypter.deriveKey(MakeTransactionFragment.this.password);
                    }
                    MakeTransactionFragment.this.request.signTransaction = true;
                    MakeTransactionFragment.this.sourceAccount.completeAndSignTx(MakeTransactionFragment.this.request);
                }
                if (MakeTransactionFragment.this.error != null) {
                    throw MakeTransactionFragment.this.error;
                }
                AbstractTransaction tx = (AbstractTransaction) Preconditions.checkNotNull(MakeTransactionFragment.this.request.getTx());
                if (MakeTransactionFragment.this.sourceAccount != null) {
                    if (!MakeTransactionFragment.this.sourceAccount.broadcastTxSync(tx)) {
                        throw new TransactionBroadcastException("Error broadcasting transaction: " + tx.getHashAsString());
                    }
                } else if (!((WalletAccount) wallet.getAccounts(((AbstractOutput) tx.getSentTo().get(0)).getAddress()).get(0)).broadcastTxSync(tx)) {
                    throw new TransactionBroadcastException("Error broadcasting transaction: " + tx.getHashAsString());
                }
                MakeTransactionFragment.this.transactionBroadcast = true;
                if (MakeTransactionFragment.this.isExchangeNeeded() && MakeTransactionFragment.this.tradeDepositAddress != null) {
                    Value amountDeposited = null;
                    if (MakeTransactionFragment.this.request.isEmptyWallet()) {
                        for (AbstractOutput out : MakeTransactionFragment.this.request.getTx().getSentTo()) {
                            if (out.getAddress().equals(MakeTransactionFragment.this.tradeDepositAddress)) {
                                amountDeposited = out.getValue();
                            }
                        }
                    } else {
                        amountDeposited = MakeTransactionFragment.this.tradeDepositAmount;
                    }
                    MakeTransactionFragment.this.exchangeEntry = new ExchangeEntry(MakeTransactionFragment.this.tradeDepositAddress, (Value) Preconditions.checkNotNull(amountDeposited), tx.getHashAsString(), MakeTransactionFragment.this.exchange);
                    MakeTransactionFragment.this.contentResolver.insert(ExchangeHistoryProvider.contentUri(MakeTransactionFragment.this.application.getPackageName(), MakeTransactionFragment.this.tradeDepositAddress), MakeTransactionFragment.this.exchangeEntry.getContentValues());
                }
                MakeTransactionFragment.this.handler.sendEmptyMessage(3);
                return MakeTransactionFragment.this.error;
            } catch (Exception e) {
                MakeTransactionFragment.this.error = e;
                return error;
            }
        }

        protected void onPostExecute(final Exception e) {
            if (Dialogs.dismissAllowingStateLoss(getFragmentManager(), SIGNING_TRANSACTION_BUSY_DIALOG_TAG)) return;

            if (e instanceof KeyCrypterException) {
                DialogBuilder.warn(getActivity(), R.string.unlocking_wallet_error_title)
                        .setMessage(R.string.unlocking_wallet_error_detail)
                        .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                listener.onSignResult(e, exchangeEntry);
                            }
                        })
                        .setPositiveButton(R.string.button_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                password = null;
                                passwordView.setText(null);
                                signAndBroadcastTask = null;
                                error = null;
                            }
                        })
                        .create().show();
            } else if (listener != null) {
                listener.onSignResult(e, exchangeEntry);
            }
        }
    }
}
