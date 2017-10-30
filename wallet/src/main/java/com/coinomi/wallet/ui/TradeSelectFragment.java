package com.coinomi.wallet.ui;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.EthereumMain;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exceptions.ExecutionException;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftCoins;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.families.eth.ERC20Token;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.tasks.AddCoinTask;
import com.coinomi.wallet.tasks.ExchangeCheckSupportedCoinsTask;
import com.coinomi.wallet.tasks.MarketInfoPollTask;
import com.coinomi.wallet.ui.Dialogs.ProgressDialogFragment;
import com.coinomi.wallet.ui.adaptors.AvailableAccountsAdaptor;
import com.coinomi.wallet.ui.adaptors.AvailableAccountsAdaptor.Entry;
import com.coinomi.wallet.ui.dialogs.ConfirmAddCoinUnlockWalletDialog;
import com.coinomi.wallet.ui.widget.AmountEditView;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.PoweredByUtil;
import com.coinomi.wallet.util.ThrottlingWalletChangeListener;
import com.coinomi.wallet.util.WeakHandler;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import org.acra.ACRA;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeSelectFragment extends Fragment implements com.coinomi.wallet.tasks.AddCoinTask.Listener, com.coinomi.wallet.tasks.ExchangeCheckSupportedCoinsTask.Listener {
    private static final Logger log = LoggerFactory.getLogger(TradeSelectFragment.class);
    private MenuItem actionSwapMenu;
    private AddCoinTask addCoinAndProceedTask;
    private CurrencyCalculatorLink amountCalculatorLink;
    @BindView(2131689754)
    TextView amountError;
    @BindView(2131689755)
    TextView amountWarning;
    private final AmountListener amountsListener = new AmountListener(this.handler);
    private WalletApplication application;
    private Entry destination;
    private AvailableAccountsAdaptor destinationAdapter;
    @BindView(2131689803)
    AmountEditView destinationAmountView;
    @BindView(2131689794)
    Spinner destinationSpinner;
    private String exchange;
    private final Handler handler = new MyHandler(this);
    private ExchangeCheckSupportedCoinsTask initialTask;
    private Value lastBalance;
    private ExchangeRate lastRate;
    private Listener listener;
    private MarketInfoTask marketTask;
    private Value maximumDeposit;
    private Value minimumDeposit;
    @BindView(2131689687)
    Button nextButton;
    private MyMarketInfoPollTask pollTask;
    @BindView(2131689820)
    TextView poweredBy;
    private Value sendAmount;
    private Entry source;
    private final AccountListener sourceAccountListener = new AccountListener(this.handler);
    private AvailableAccountsAdaptor sourceAdapter;
    @BindView(2131689802)
    AmountEditView sourceAmountView;
    @BindView(2131689792)
    Spinner sourceSpinner;
    private Timer timer;
    @BindView(2131689799)
    TextView tradeInfo;
    private Unbinder unbinder;
    private Wallet wallet;

    public interface Listener {
        void onMakeTrade(WalletAccount walletAccount, CoinType coinType, WalletAccount walletAccount2, CoinType coinType2, Value value, String str);
    }

    class C05131 implements OnClickListener {
        C05131() {
        }

        public void onClick(View v) {
            TradeSelectFragment.this.validateAmount();
            if (TradeSelectFragment.this.everythingValid()) {
                TradeSelectFragment.this.onHandleNext();
            } else if (TradeSelectFragment.this.amountCalculatorLink.isEmpty()) {
                TradeSelectFragment.this.amountError.setText(R.string.amount_error_empty);
                TradeSelectFragment.this.amountError.setVisibility(View.VISIBLE);
            }
        }
    }

    class C05142 implements DialogInterface.OnClickListener {
        C05142() {
        }

        public void onClick(DialogInterface dialog, int which) {
            TradeSelectFragment.this.initialTask = null;
            TradeSelectFragment.this.maybeStartInitialTask();
        }
    }

    class C05153 implements OnItemSelectedListener {
        C05153() {
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Entry newSource = (Entry) parent.getSelectedItem();
            if (!newSource.equals(TradeSelectFragment.this.source)) {
                if (TradeSelectFragment.this.destination.equals(newSource)) {
                    TradeSelectFragment.this.setDestinationSpinner(TradeSelectFragment.this.source);
                    TradeSelectFragment.this.setDestination(TradeSelectFragment.this.source, false);
                }
                TradeSelectFragment.this.setSource(newSource, true);
            }
        }

        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    class C05164 implements OnItemSelectedListener {
        C05164() {
        }

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Entry entry = (Entry) parent.getSelectedItem();
            if (!entry.equals(TradeSelectFragment.this.destination)) {
                if (TradeSelectFragment.this.source.equals(entry)) {
                    TradeSelectFragment.this.setSourceSpinner(TradeSelectFragment.this.destination);
                    TradeSelectFragment.this.setSource(TradeSelectFragment.this.destination, false);
                }
                TradeSelectFragment.this.setDestination(entry, true);
            }
        }

        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    class C05175 implements DialogInterface.OnClickListener {
        C05175() {
        }

        public void onClick(DialogInterface dialog, int which) {
            TradeSelectFragment.this.createToAccountAndProceed();
        }
    }

    private static class AccountListener extends ThrottlingWalletChangeListener {
        private final Handler handler;

        private AccountListener(Handler handler) {
            this.handler = handler;
        }

        public void onThrottledWalletChanged() {
            this.handler.sendEmptyMessage(2);
        }
    }

    private static class AmountListener implements com.coinomi.wallet.ui.widget.AmountEditView.Listener {
        private final Handler handler;

        private AmountListener(Handler handler) {
            this.handler = handler;
        }

        public void changed() {
            this.handler.sendMessage(this.handler.obtainMessage(3, Boolean.valueOf(true)));
        }

        public void focusChanged(boolean hasFocus) {
            if (!hasFocus) {
                this.handler.sendMessage(this.handler.obtainMessage(3, Boolean.valueOf(false)));
            }
        }
    }

    private static class MarketInfoTask extends AsyncTask<Void, Void, ShapeShiftMarketInfo> {
        final Handler handler;
        final String pair;
        final ShapeShift shapeShift;

        private MarketInfoTask(Handler handler, ShapeShift shift, String pair) {
            this.shapeShift = shift;
            this.handler = handler;
            this.pair = pair;
        }

        protected ShapeShiftMarketInfo doInBackground(Void... params) {
            return MarketInfoPollTask.getMarketInfoSync(this.shapeShift, this.pair);
        }

        protected void onPostExecute(ShapeShiftMarketInfo marketInfo) {
            if (marketInfo != null) {
                this.handler.sendMessage(this.handler.obtainMessage(0, marketInfo));
            } else {
                this.handler.sendEmptyMessage(1);
            }
        }
    }

    private static class MyHandler extends WeakHandler<TradeSelectFragment> {
        public MyHandler(TradeSelectFragment referencingObject) {
            super(referencingObject);
        }

        protected void weakHandleMessage(TradeSelectFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.onMarketUpdate((ShapeShiftMarketInfo) msg.obj);
                    return;
                case 1:
                    Toast.makeText(ref.getActivity(), ref.getString(R.string.trade_error_market_info, ref.source.getType().getName(), ref.destination.getType().getName()), Toast.LENGTH_LONG).show();
                    return;
                case 2:
                    ref.onWalletUpdate();
                    return;
                case 3:
                    ref.validateAmount(((Boolean) msg.obj).booleanValue());
                    return;
                case 4:
                    ref.showInitialTaskErrorDialog((String) msg.obj);
                    return;
                case 5:
                    ref.updateAvailableCoins((ShapeShiftCoins) msg.obj);
                    ref.startMarketInfoTask();
                    return;
                default:
                    return;
            }
        }
    }

    private static class MyMarketInfoPollTask extends MarketInfoPollTask {
        private final Handler handler;

        MyMarketInfoPollTask(Handler handler, ShapeShift shapeShift, String pair) {
            super(shapeShift, pair);
            this.handler = handler;
        }

        public void onHandleMarketInfo(ShapeShiftMarketInfo marketInfo) {
            this.handler.sendMessage(this.handler.obtainMessage(0, marketInfo));
        }
    }

    @OnClick({2131689751})
    public void onEmptyWalletClick() {
        setAmountForEmptyWallet();
    }

    @OnClick({2131689801})
    public void onPreciseClick() {
        if (this.exchange.equals("shapeshift")) {
            if (this.amountCalculatorLink.getExchangeDirection()) {
                Value preciseAmount = this.amountCalculatorLink.getSecondaryAmount();
                this.amountCalculatorLink.setExchangeDirection(false);
                this.amountCalculatorLink.setSecondaryAmount(preciseAmount);
            }
            this.destinationAmountView.requestFocus();
        }
    }

    private void setAmountForEmptyWallet() {
        updateBalance();
        if (this.lastBalance != null) {
            if (this.lastBalance.isZero()) {
                Toast.makeText(getActivity(), getResources().getString(R.string.amount_error_not_enough_money_plain), Toast.LENGTH_LONG).show();
                return;
            }
            this.amountCalculatorLink.setPrimaryAmount(this.lastBalance);
            validateAmount();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        WalletAccount sourceAccount = this.application.getAccount(this.application.getConfiguration().getLastAccountId());
        if (sourceAccount == null) {
            List<WalletAccount> accounts = this.application.getAllAccounts();
            if (accounts.size() == 0) {
                getActivity().finish();
                return;
            }
            sourceAccount = (WalletAccount) accounts.get(0);
        }
        this.source = new Entry(sourceAccount);
        if (savedInstanceState != null) {
            this.exchange = savedInstanceState.containsKey("exchange_id") ? savedInstanceState.getString("exchange_id") : "shapeshift";
        } else if (getArguments() != null) {
            Bundle args = getArguments();
            this.exchange = args.containsKey("exchange_id") ? args.getString("exchange_id") : "shapeshift";
        } else {
            this.exchange = "shapeshift";
        }
        for (CoinType type : Constants.SUPPORTED_COINS) {
            if (!type.equals(this.source.getType())) {
                this.destination = new Entry(type);
                break;
            }
        }
        updateBalance();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trade_select, container, false);
        this.unbinder = ButterKnife.bind((Object) this, view);
        String exchangeName = getString(R.string.default_exchange);
        if (this.exchange.equals("shapeshift")) {
            exchangeName = getString(R.string.shapeshift);
            this.tradeInfo.setText(R.string.trade_info_rates);
            this.tradeInfo.setVisibility(View.VISIBLE);
            PoweredByUtil.setup(getContext(), "shapeshift", this.poweredBy);
            this.poweredBy.setVisibility(View.VISIBLE);
        } else if (this.exchange.equals("changelly")) {
            this.destinationAmountView.setEnabledAndFocusable(false);
            ButterKnife.findById(view, (int) R.id.precise).setVisibility(View.GONE);
            exchangeName = getString(R.string.changelly);
            this.tradeInfo.setText(getString(R.string.trade_info_rates_no_tx_fee, exchangeName));
            this.tradeInfo.setVisibility(View.VISIBLE);
            PoweredByUtil.setup(getContext(), "changelly", this.poweredBy);
            this.poweredBy.setVisibility(View.VISIBLE);
        }
        FragmentActivity activity = getActivity();
        if (activity != null && (activity instanceof BaseWalletActivity)) {
            ((BaseWalletActivity) activity).setActionBarTitle(exchangeName);
        }
        this.sourceSpinner.setAdapter(getSourceSpinnerAdapter());
        this.sourceSpinner.setOnItemSelectedListener(getSourceSpinnerListener());
        this.destinationSpinner.setAdapter(getDestinationSpinnerAdapter());
        this.destinationSpinner.setOnItemSelectedListener(getDestinationSpinnerListener());
        this.amountCalculatorLink = new CurrencyCalculatorLink(this.sourceAmountView, this.destinationAmountView);
        this.amountError.setVisibility(View.GONE);
        this.amountWarning.setVisibility(View.GONE);
        this.nextButton.setOnClickListener(new C05131());
        if (this.application.isConnected()) {
            maybeStartInitialTask();
        } else {
            showInitialTaskErrorDialog(null);
        }
        return view;
    }

    private AvailableAccountsAdaptor getDestinationSpinnerAdapter() {
        if (this.destinationAdapter == null) {
            this.destinationAdapter = new AvailableAccountsAdaptor(getActivity());
        }
        return this.destinationAdapter;
    }

    private AvailableAccountsAdaptor getSourceSpinnerAdapter() {
        if (this.sourceAdapter == null) {
            this.sourceAdapter = new AvailableAccountsAdaptor(getActivity());
        }
        return this.sourceAdapter;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.application = (WalletApplication) context.getApplicationContext();
            this.wallet = this.application.getWallet();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass() + " must implement " + Listener.class);
        }
    }

    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }

    public void onDestroyView() {
        this.amountCalculatorLink = null;
        this.unbinder.unbind();
        super.onDestroyView();
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.trade, menu);
        this.actionSwapMenu = menu.findItem(R.id.action_swap_coins);
    }

    public void onPause() {
        stopPolling();
        removeSourceListener();
        this.amountCalculatorLink.setListener(null);
        super.onPause();
    }

    public void onResume() {
        super.onResume();
        startPolling();
        this.amountCalculatorLink.setListener(this.amountsListener);
        addSourceListener();
        updateNextButtonState();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshStartInitialTask();
                return true;
            case R.id.action_swap_coins:
                swapAccounts();
                return true;
            case R.id.action_exchange_history:
                Intent exchangeHistoryIntent = new Intent(getActivity(), ExchangeHistoryActivity.class);
                exchangeHistoryIntent.putExtra("exchange_id", this.exchange);
                startActivity(exchangeHistoryIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onHandleNext() {
        if (this.listener == null) {
            return;
        }
        if (this.destination.getAccount() == null) {
            createToAccountAndProceed();
        } else if (everythingValid()) {
            Keyboard.hideKeyboard(getActivity());
            this.listener.onMakeTrade(this.source.getAccount(), this.source.getType(), this.destination.getAccount(), this.destination.getType(), this.sendAmount, this.exchange);
        } else {
            Toast.makeText(getActivity(), R.string.amount_error, Toast.LENGTH_LONG).show();
        }
    }

    private void createToAccountAndProceed() {
        if (this.destination.getType() == null) {
            Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_SHORT).show();
        } else {
            ConfirmAddCoinUnlockWalletDialog.getInstance(this.destination.getType(), this.wallet.isEncrypted()).show(getFragmentManager(), "add_coin_dialog_tag");
        }
    }

    void maybeStartAddCoinAndProceedTask(String description, CharSequence password, List<ChildNumber> customPath) {
        if (this.addCoinAndProceedTask == null) {
            this.addCoinAndProceedTask = new AddCoinTask(this, this.destination.getType(), this.wallet, description, password, customPath);
            this.addCoinAndProceedTask.execute(new Void[0]);
        }
    }

    public void onAddCoinTaskStarted() {
        ProgressDialogFragment.show(getFragmentManager(), getResources().getString(R.string.adding_coin_working, new Object[]{this.destination.getType().getName()}), "add_coin_task_busy_dialog_tag");
    }

    public void onAddCoinTaskFinished(Exception error, WalletAccount newAccount) {
        if (!Dialogs.dismissAllowingStateLoss(getFragmentManager(), "add_coin_task_busy_dialog_tag")) {
            this.addCoinAndProceedTask = null;
            if (error == null) {
                this.destination = new Entry(newAccount);
                this.destinationAdapter.updateAccount(newAccount);
                onHandleNext();
            } else if (error instanceof KeyCrypterException) {
                showPasswordRetryDialog();
            } else {
                if (ACRA.isInitialised()) {
                    ACRA.getErrorReporter().handleSilentException(error);
                }
                Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addSourceListener() {
        WalletAccount account = this.source.getAccount();
        if (account != null) {
            account.addEventListener(this.sourceAccountListener, Threading.SAME_THREAD);
            onWalletUpdate();
        }
    }

    private void removeSourceListener() {
        WalletAccount account = this.source.getAccount();
        if (account != null) {
            account.removeEventListener(this.sourceAccountListener);
            this.sourceAccountListener.removeCallbacks();
        }
    }

    private void startPolling() {
        if (this.timer == null) {
            this.pollTask = new MyMarketInfoPollTask(this.handler, this.application.getShapeShift(this.exchange), getPair());
            this.timer = new Timer();
            this.timer.schedule(this.pollTask, 0, 30000);
        }
    }

    private void stopPolling() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
            this.pollTask.cancel();
            this.pollTask = null;
        }
    }

    private void updateAvailableCoins(ShapeShiftCoins availableCoins) {
        List<CoinType> supportedTypes = getSupportedTypes(availableCoins.availableCoinTypes, false);
        List<CoinType> favoriteTypes = getSupportedTypes(availableCoins.availableCoinTypes, true);
        List<WalletAccount> allAccounts = this.application.getAllAccounts();
        this.sourceAdapter.update(allAccounts, favoriteTypes, false);
        List<CoinType> sourceTypes = this.sourceAdapter.getTypes();
        if (sourceTypes.size() == 0) {
            new Builder(getActivity()).setTitle(R.string.trade_error).setMessage(R.string.trade_error_no_supported_source_accounts).setPositiveButton(R.string.button_ok, null).create().show();
            return;
        }
        if (this.sourceSpinner.getSelectedItemPosition() == -1) {
            if (this.sourceAdapter.getPosition(this.source) != -1) {
                this.sourceSpinner.setSelection(this.sourceAdapter.getPosition(this.source));
            } else {
                this.sourceSpinner.setSelection(0);
            }
        }
        CoinType sourceType = ((Entry) this.sourceSpinner.getSelectedItem()).getType();
        if (sourceTypes.size() == 1) {
            ArrayList<CoinType> typesWithoutSourceType = Lists.newArrayList(supportedTypes);
            typesWithoutSourceType.remove(sourceType);
            this.destinationAdapter.update(allAccounts, typesWithoutSourceType, true);
        } else {
            this.destinationAdapter.update(allAccounts, supportedTypes, true);
        }
        if (this.destinationSpinner.getSelectedItemPosition() == -1) {
            for (Entry entry : this.destinationAdapter.getEntries()) {
                if (!sourceType.equals(entry.getType())) {
                    this.destinationSpinner.setSelection(this.destinationAdapter.getPosition(entry));
                    return;
                }
            }
        }
    }

    private void showInitialTaskErrorDialog(String error) {
        if (getActivity() != null) {
            DialogBuilder builder;
            if (error == null) {
                builder = DialogBuilder.warn(getActivity(), R.string.trade_warn_no_connection_title);
                builder.setMessage((int) R.string.trade_warn_no_connection_message);
            } else {
                builder = DialogBuilder.warn(getActivity(), R.string.trade_error);
                builder.setMessage((int) R.string.trade_error_service_not_available);
                if (ACRA.isInitialised()) {
                    ACRA.getErrorReporter().putCustomData("trade-error", error);
                }
            }
            builder.setNegativeButton((int) R.string.button_dismiss, null);
            builder.setPositiveButton((int) R.string.button_retry, new C05142());
            builder.create().show();
        }
    }

    private List<CoinType> getSupportedTypes(List<CoinType> availableCoins, boolean favorites) {
        ImmutableList.Builder<CoinType> builder = ImmutableList.builder();
        for (CoinType supportedType : Constants.SUPPORTED_COINS) {
            if (availableCoins.contains(supportedType)) {
                builder.add(supportedType);
            }
        }
        for (WalletAccount wallet : this.application.getAccounts(EthereumMain.get())) {
            if (wallet instanceof EthFamilyWallet) {
                Collection<ERC20Token> tokens = new ArrayList();
                if (favorites) {
                    tokens = ((EthFamilyWallet) wallet).getERC20Favorites().values();
                } else {
                    tokens = ((EthFamilyWallet) wallet).getAllERC20Tokens().values();
                }
                for (ERC20Token token : tokens) {
                    if (availableCoins.contains(token)) {
                        builder.add(token);
                    }
                }
            }
        }
        return builder.build();
    }

    private void refreshStartInitialTask() {
        if (this.initialTask != null) {
            this.initialTask.cancel(true);
            this.initialTask = null;
        }
        maybeStartInitialTask();
    }

    private void maybeStartInitialTask() {
        if (this.initialTask == null) {
            this.initialTask = new ExchangeCheckSupportedCoinsTask(this, this.application, this.exchange);
            this.initialTask.execute(new Void[0]);
        }
    }

    public void onExchangeCheckSupportedCoinsTaskStarted() {
        ProgressDialogFragment.show(getFragmentManager(), getString(R.string.contacting_exchange), "initial_task_busy_dialog_tag");
    }

    public void onExchangeCheckSupportedCoinsTaskFinished(Exception error, ShapeShiftCoins shapeShiftCoins) {
        if (!Dialogs.dismissAllowingStateLoss(getFragmentManager(), "initial_task_busy_dialog_tag")) {
            if (error != null) {
                log.warn("Could not get ShapeShift coins", (Throwable) error);
                this.handler.sendMessage(this.handler.obtainMessage(4, error.getMessage()));
            } else if (shapeShiftCoins.isError) {
                log.warn("Could not get " + this.exchange + " coins: {}", shapeShiftCoins.errorMessage);
                this.handler.sendMessage(this.handler.obtainMessage(4, shapeShiftCoins.errorMessage));
            } else {
                this.handler.sendMessage(this.handler.obtainMessage(5, shapeShiftCoins));
            }
        }
    }

    private void startMarketInfoTask() {
        if (this.marketTask != null) {
            this.marketTask.cancel(true);
            this.marketTask = null;
        }
        if (getActivity() != null) {
            this.marketTask = new MarketInfoTask(this.handler, this.application.getShapeShift(this.exchange), getPair());
            this.marketTask.execute(new Void[0]);
        }
    }

    private String getPair() {
        return ShapeShift.getPair(this.source.getType(), this.destination.getType());
    }

    private void onMarketUpdate(ShapeShiftMarketInfo marketInfo) {
        if (marketInfo.isPair(this.source.getType(), this.destination.getType())) {
            this.maximumDeposit = marketInfo.limit;
            this.minimumDeposit = marketInfo.minimum;
            this.lastRate = marketInfo.rate;
            this.amountCalculatorLink.setExchangeRate(this.lastRate);
            if (this.amountCalculatorLink.isEmpty() && this.lastRate != null) {
                Value hintValue = this.source.getType().oneCoin();
                Value exchangedValue = this.lastRate.convert(hintValue);
                Value minerFee100 = marketInfo.rate.minerFee.multiply(100);
                for (int tries = 8; tries > 0 && (exchangedValue.isZero() || exchangedValue.compareTo(minerFee100) < 0); tries--) {
                    hintValue = hintValue.multiply(10);
                    exchangedValue = this.lastRate.convert(hintValue);
                }
                this.amountCalculatorLink.setExchangeRateHints(hintValue);
            }
        }
    }

    private OnItemSelectedListener getSourceSpinnerListener() {
        return new C05153();
    }

    private OnItemSelectedListener getDestinationSpinnerListener() {
        return new C05164();
    }

    private void setSourceSpinner(Entry source) {
        int newPosition = this.sourceAdapter.getPosition(source);
        if (newPosition >= 0) {
            OnItemSelectedListener cb = this.sourceSpinner.getOnItemSelectedListener();
            this.sourceSpinner.setOnItemSelectedListener(null);
            this.sourceSpinner.setSelection(newPosition);
            this.sourceSpinner.setOnItemSelectedListener(cb);
        }
    }

    private void setDestinationSpinner(Entry destination) {
        int newPosition = this.destinationAdapter.getPosition(destination);
        if (newPosition >= 0) {
            OnItemSelectedListener cb = this.destinationSpinner.getOnItemSelectedListener();
            this.destinationSpinner.setOnItemSelectedListener(null);
            this.destinationSpinner.setSelection(newPosition);
            this.destinationSpinner.setOnItemSelectedListener(cb);
        }
    }

    private void setSource(Entry source, boolean startNetworkTask) {
        removeSourceListener();
        this.source = source;
        addSourceListener();
        this.sourceAmountView.reset();
        this.sourceAmountView.setType(source.getType());
        this.amountCalculatorLink.setExchangeRate(null);
        this.minimumDeposit = null;
        this.maximumDeposit = null;
        updateOptionsMenu();
        if (startNetworkTask) {
            startMarketInfoTask();
            if (this.pollTask != null) {
                this.pollTask.updatePair(getPair());
            }
            this.application.maybeConnectAccount(source.getAccount());
        }
    }

    private void setDestination(Entry destination, boolean startNetworkTask) {
        this.destination = destination;
        this.destinationAmountView.reset();
        this.destinationAmountView.setType(destination.getType());
        this.amountCalculatorLink.setExchangeRate(null);
        this.minimumDeposit = null;
        this.maximumDeposit = null;
        updateOptionsMenu();
        if (startNetworkTask) {
            startMarketInfoTask();
            if (this.pollTask != null) {
                this.pollTask.updatePair(getPair());
            }
        }
    }

    private void swapAccounts() {
        if (isSwapAccountPossible()) {
            Entry newSource = this.destination;
            Entry newDestination = this.source;
            setSourceSpinner(newSource);
            setDestinationSpinner(newDestination);
            setSource(newSource, false);
            setDestination(newDestination, true);
            return;
        }
        Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_SHORT).show();
    }

    private boolean isSwapAccountPossible() {
        return this.destination.getAccount() != null;
    }

    private void updateOptionsMenu() {
        if (this.actionSwapMenu != null) {
            this.actionSwapMenu.setEnabled(isSwapAccountPossible());
        }
    }

    private void updateBalance() {
        WalletAccount sourceAccount = this.source.getAccount();
        if (sourceAccount == null) {
            log.warn("Source account was null");
            this.lastBalance = this.source.getType().zeroCoin();
        } else if (this.source.getType() instanceof ERC20Token) {
            try {
                this.lastBalance = ((ERC20Token) this.source.getType()).getBalance((EthFamilyWallet) sourceAccount);
            } catch (ExecutionException e) {
                Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            this.lastBalance = sourceAccount.getBalance();
        }
    }

    private void onWalletUpdate() {
        updateBalance();
        validateAmount();
    }

    private boolean isAmountWithinLimits(Value amount) {
        boolean isWithinLimits;
        if (amount.isDust()) {
            isWithinLimits = false;
        } else {
            isWithinLimits = true;
        }
        if (isWithinLimits && this.minimumDeposit != null && this.maximumDeposit != null && this.minimumDeposit.isOfType(amount) && this.maximumDeposit.isOfType(amount)) {
            isWithinLimits = amount.within(this.minimumDeposit, this.maximumDeposit);
        }
        if (!isWithinLimits || !Value.canCompare(this.lastBalance, amount)) {
            return isWithinLimits;
        }
        if (amount.compareTo(this.lastBalance) <= 0) {
            return true;
        }
        return false;
    }

    private boolean isAmountTooSmall(Value amount) {
        return amount.compareTo(getLowestAmount(amount)) < 0;
    }

    private Value getLowestAmount(Value amount) {
        Value min = amount.type.getMinNonDust();
        if (this.minimumDeposit == null) {
            return min;
        }
        if (this.minimumDeposit.isOfType(min)) {
            return Value.max(this.minimumDeposit, min);
        }
        if (this.lastRate == null || !this.lastRate.canConvert(amount.type, this.minimumDeposit.type)) {
            return min;
        }
        return Value.max(this.lastRate.convert(this.minimumDeposit), min);
    }

    private boolean isAmountValid(Value amount) {
        boolean isValid = (amount == null || amount.isDust()) ? false : true;
        if (isValid && amount.isOfType(this.source.getType())) {
            return isAmountWithinLimits(amount);
        }
        return isValid;
    }

    private void validateAmount() {
        validateAmount(false);
    }

    private void validateAmount(boolean isTyping) {
        Value depositAmount = this.amountCalculatorLink.getPrimaryAmount();
        Value withdrawAmount = this.amountCalculatorLink.getSecondaryAmount();
        Value requestedAmount = this.amountCalculatorLink.getRequestedAmount();
        if (isAmountValid(depositAmount) && isAmountValid(withdrawAmount)) {
            this.sendAmount = requestedAmount;
            this.amountError.setVisibility(View.GONE);
            if (Value.canCompare(this.lastBalance, depositAmount) && this.lastBalance.compareTo(depositAmount) == 0) {
                this.amountWarning.setText(R.string.amount_warn_fees_apply);
                this.amountWarning.setVisibility(View.VISIBLE);
            } else {
                this.amountWarning.setVisibility(View.GONE);
            }
        } else {
            this.amountWarning.setVisibility(View.GONE);
            this.sendAmount = null;
            if (shouldShowErrors(isTyping, depositAmount, withdrawAmount)) {
                if (depositAmount == null || withdrawAmount == null) {
                    this.amountError.setText(R.string.amount_error);
                } else if (depositAmount.isNegative() || withdrawAmount.isNegative()) {
                    this.amountError.setText(R.string.amount_error_negative);
                } else if (isAmountWithinLimits(depositAmount) && isAmountWithinLimits(withdrawAmount)) {
                    this.amountError.setText(R.string.amount_error);
                } else {
                    String message = getString(R.string.error_generic);
                    if (isAmountTooSmall(depositAmount) || isAmountTooSmall(withdrawAmount)) {
                        Value minimumDeposit = getLowestAmount(depositAmount);
                        Value minimumWithdraw = getLowestAmount(withdrawAmount);
                        message = getString(R.string.trade_error_min_limit, minimumDeposit.toFriendlyString() + " (" + minimumWithdraw.toFriendlyString() + ")");
                    } else {
                        if (Value.canCompare(this.lastBalance, depositAmount) && depositAmount.compareTo(this.lastBalance) > 0) {
                            message = getString(R.string.amount_error_not_enough_money, this.lastBalance.toFriendlyString());
                        }
                        if (Value.canCompare(this.maximumDeposit, depositAmount) && depositAmount.compareTo(this.maximumDeposit) > 0) {
                            String maxDepositString = this.maximumDeposit.toFriendlyString();
                            if (this.lastRate != null && this.lastRate.canConvert(this.maximumDeposit.type, this.destination.getType())) {
                                maxDepositString = maxDepositString + " (" + this.lastRate.convert(this.maximumDeposit).toFriendlyString() + ")";
                            }
                            message = getString(R.string.trade_error_max_limit, maxDepositString);
                        }
                    }
                    this.amountError.setText(message);
                }
                this.amountError.setVisibility(View.VISIBLE);
            } else {
                this.amountError.setVisibility(View.GONE);
            }
        }
        updateNextButtonState();
    }

    private boolean isOutputsValid() {
        return true;
    }

    private boolean everythingValid() {
        return isOutputsValid() && isAmountValid(this.sendAmount);
    }

    private void updateNextButtonState() {
    }

    private boolean shouldShowErrors(boolean isTyping, Value sending, Value receiving) {
        if (Value.canCompare(sending, this.lastBalance) && sending.compareTo(this.lastBalance) > 0) {
            return true;
        }
        if (isTyping) {
            return false;
        }
        if (this.amountCalculatorLink.isEmpty()) {
            return false;
        }
        if (sending == null || sending.isZero()) {
            return false;
        }
        if (receiving == null || receiving.isZero()) {
            return false;
        }
        return true;
    }

    private void showPasswordRetryDialog() {
        DialogBuilder.warn(getActivity(), R.string.unlocking_wallet_error_title).setMessage((int) R.string.unlocking_wallet_error_detail).setNegativeButton((int) R.string.button_cancel, null).setPositiveButton((int) R.string.button_retry, new C05175()).create().show();
    }
}
