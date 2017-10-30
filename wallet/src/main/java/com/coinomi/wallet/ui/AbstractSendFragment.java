package com.coinomi.wallet.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.widget.Toast;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.ValueType;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.R;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.tasks.AddCoinTask;
import com.coinomi.wallet.tasks.AddCoinTask.Listener;
import com.coinomi.wallet.ui.Dialogs.ProgressDialogFragment;
import com.coinomi.wallet.ui.dialogs.ConfirmAddCoinUnlockWalletDialog;
import com.coinomi.wallet.util.WeakHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.acra.ACRA;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.KeyCrypterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSendFragment extends WalletFragment implements Listener {
    private static final Logger log = LoggerFactory.getLogger(AbstractSendFragment.class);
    protected AddCoinTask addCoinAndProceedTask;
    protected CurrencyCalculatorLink amountCalculatorLink;
    protected WalletApplication application;
    protected Configuration config;
    protected final MyHandler handler = new MyHandler(this);
    protected Value lastBalance;
    protected Map<String, ExchangeRate> localRates = new HashMap();
    protected ShapeShiftMarketInfo marketInfo;
    private final LoaderCallbacks<Cursor> rateLoaderCallbacks = new C03701();
    protected State state = State.INPUT;
    protected Value validSendAmount;
    protected Wallet wallet;

    class C03701 implements LoaderCallbacks<Cursor> {
        C03701() {
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new ExchangeRateLoader(AbstractSendFragment.this.getActivity(), AbstractSendFragment.this.config, AbstractSendFragment.this.config.getExchangeCurrencyCode());
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.getCount() > 0) {
                HashMap<String, ExchangeRate> rates = new HashMap(data.getCount());
                data.moveToFirst();
                do {
                    ExchangeRatesProvider.ExchangeRate rate = ExchangeRatesProvider.getExchangeRate(data);
                    rates.put(rate.currencyCodeId, rate.rate);
                } while (data.moveToNext());
                AbstractSendFragment.this.handler.sendMessage(AbstractSendFragment.this.handler.obtainMessage(-1, rates));
            }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    protected static class MyHandler extends WeakHandler<AbstractSendFragment> {
        public MyHandler(AbstractSendFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(AbstractSendFragment ref, Message msg) {
            switch (msg.what) {
                case -1:
                    ref.onLocalExchangeRatesUpdate((HashMap) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    protected enum State {
        INPUT,
        PREPARATION,
        SENDING,
        SENT,
        FAILED
    }

    protected abstract void accountAdded(WalletAccount walletAccount);

    protected abstract Value getPrimaryAmount();

    protected abstract void hideAmountError();

    protected abstract void hideAmountWarning();

    protected abstract void setExchangeRate(ExchangeRate exchangeRate);

    protected abstract void setPrimaryAmount(Value value);

    protected abstract void showAmountError(int i);

    protected abstract void showAmountError(String str);

    protected abstract void showAmountWarning(int i);

    public void onAttach(Context context) {
        super.onAttach(context);
        this.application = (WalletApplication) context.getApplicationContext();
        this.wallet = this.application.getWallet();
        this.config = this.application.getConfiguration();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(-1, null, this.rateLoaderCallbacks);
    }

    public void onDetach() {
        getLoaderManager().destroyLoader(-1);
        super.onDetach();
    }

    protected void updateBalance() {
        WalletAccount currentAccount = getAccount();
        if (currentAccount != null) {
            this.lastBalance = currentAccount.getBalance();
        }
    }

    protected boolean isAmountValid(Value amount) {
        return amount != null && isAmountWithinLimits(amount);
    }

    protected boolean isAmountWithinLimits(Value amount) {
        boolean isWithinLimits;
        if (amount == null || !amount.isPositive() || amount.isDust()) {
            isWithinLimits = false;
        } else {
            isWithinLimits = true;
        }
        if (isWithinLimits && this.marketInfo != null && Value.canCompare(this.marketInfo.limit, amount)) {
            isWithinLimits = amount.within(this.marketInfo.minimum, this.marketInfo.limit);
        }
        if (!isWithinLimits || !Value.canCompare(this.lastBalance, amount)) {
            return isWithinLimits;
        }
        if (amount.compareTo(this.lastBalance) <= 0) {
            return true;
        }
        return false;
    }

    protected boolean isAmountTooSmall(Value amount) {
        return amount.compareTo(getLowestAmount(amount.type)) < 0;
    }

    private Value getLowestAmount(ValueType type) {
        return type.getMinNonDust();
    }

    protected void validateAmount() {
        validateAmount(false);
    }

    protected void validateAmount(boolean isTyping) {
        if (!isRemoving()) {
            Value amountParsed = getPrimaryAmount();
            if (isAmountValid(amountParsed)) {
                this.validSendAmount = amountParsed;
                hideAmountError();
                if (Value.canCompare(this.validSendAmount, this.lastBalance) && this.validSendAmount.compareTo(this.lastBalance) == 0) {
                    showAmountWarning(R.string.amount_warn_fees_apply);
                } else {
                    hideAmountWarning();
                }
            } else {
                hideAmountWarning();
                if (shouldShowErrors(isTyping, amountParsed)) {
                    this.validSendAmount = null;
                    if (amountParsed == null) {
                        showAmountError((int) R.string.amount_error);
                    } else if (amountParsed.isNegative()) {
                        showAmountError((int) R.string.amount_error_negative);
                    } else if (isAmountWithinLimits(amountParsed)) {
                        showAmountError((int) R.string.amount_error);
                    } else {
                        String message = getString(R.string.error_generic);
                        if (isAmountTooSmall(amountParsed)) {
                            message = getString(R.string.amount_error_too_small, getLowestAmount(amountParsed.type).toFriendlyString());
                        } else {
                            if (Value.canCompare(this.lastBalance, amountParsed) && amountParsed.compareTo(this.lastBalance) > 0) {
                                message = getString(R.string.amount_error_not_enough_money, this.lastBalance.toFriendlyString());
                            }
                            if (this.marketInfo != null && Value.canCompare(this.marketInfo.limit, amountParsed) && amountParsed.compareTo(this.marketInfo.limit) > 0) {
                                message = getString(R.string.trade_error_max_limit, this.marketInfo.limit.toFriendlyString());
                            }
                        }
                        showAmountError(message);
                    }
                } else {
                    hideAmountError();
                }
            }
            updateView();
        }
    }

    private boolean shouldShowErrors(boolean isTyping, Value amount) {
        if (amount != null && !amount.isZero() && !isAmountWithinLimits(amount)) {
            return true;
        }
        if (isTyping) {
            return false;
        }
        if (this.amountCalculatorLink.isEmpty()) {
            return false;
        }
        if (amount == null || !amount.isZero()) {
            return true;
        }
        return false;
    }

    protected void setAmountForEmptyWallet() {
        updateBalance();
        if (this.state == State.INPUT && getAccount() != null && this.lastBalance != null) {
            if (this.lastBalance.isZero()) {
                Toast.makeText(getActivity(), getResources().getString(R.string.amount_error_not_enough_money_plain), 1).show();
                return;
            }
            setPrimaryAmount(this.lastBalance);
            validateAmount();
        }
    }

    private void onLocalExchangeRatesUpdate(HashMap<String, ExchangeRate> rates) {
        this.localRates = rates;
        setExchangeRate(getCurrentRate());
    }

    protected ExchangeRate getCurrentRate() {
        WalletAccount currentAccount = getAccount();
        if (currentAccount != null) {
            return (ExchangeRate) this.localRates.get(currentAccount.getCoinType().getSymbol());
        }
        return null;
    }

    protected void createAccount(CoinType type) {
        if (type == null) {
            Toast.makeText(getActivity(), R.string.error_generic, 1).show();
        } else {
            ConfirmAddCoinUnlockWalletDialog.getInstance(type, this.wallet.isEncrypted()).show(getFragmentManager(), "add_coin_dialog_tag");
        }
    }

    protected void maybeStartAddCoinAndProceedTask(CoinType type, String description, CharSequence password, List<ChildNumber> customPath) {
        if (this.addCoinAndProceedTask == null && type != null) {
            this.addCoinAndProceedTask = new AddCoinTask(this, type, this.wallet, description, password, customPath);
            this.addCoinAndProceedTask.execute(new Void[0]);
        }
    }

    public void onAddCoinTaskStarted() {
        ProgressDialogFragment.show(getFragmentManager(), getResources().getString(R.string.adding_coin_working, new Object[]{this.addCoinAndProceedTask.type.getName()}), "add_coin_task_busy_dialog_tag");
    }

    public void onAddCoinTaskFinished(Exception error, WalletAccount newAccount) {
        if (!Dialogs.dismissAllowingStateLoss(getFragmentManager(), "add_coin_task_busy_dialog_tag")) {
            CoinType type = this.addCoinAndProceedTask.type;
            this.addCoinAndProceedTask = null;
            if (error == null) {
                accountAdded(newAccount);
            } else if (error instanceof KeyCrypterException) {
                showPasswordRetryDialog(type);
            } else {
                if (ACRA.isInitialised()) {
                    ACRA.getErrorReporter().handleSilentException(error);
                }
                Toast.makeText(getActivity(), R.string.error_generic, 1).show();
            }
        }
    }

    private void showPasswordRetryDialog(final CoinType type) {
        DialogBuilder.warn(getActivity(), R.string.unlocking_wallet_error_title).setMessage((int) R.string.unlocking_wallet_error_detail).setNegativeButton((int) R.string.button_cancel, null).setPositiveButton((int) R.string.button_retry, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                AbstractSendFragment.this.createAccount(type);
            }
        }).create().show();
    }
}
