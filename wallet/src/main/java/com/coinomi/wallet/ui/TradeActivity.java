package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.coinomi.wallet.R;
import com.coinomi.wallet.tasks.AddCoinTask;
import com.coinomi.wallet.ui.dialogs.ConfirmAddCoinUnlockWalletDialog;
import com.coinomi.wallet.ui.dialogs.ExchangeTermsOfUseDialog;
import com.coinomi.wallet.ui.dialogs.ShapeshiftTermsOfUseDialog;
import com.coinomi.wallet.util.WalletUtils;

import org.acra.ACRA;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.KeyCrypterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.annotation.Nullable;


public class TradeActivity extends BaseWalletActivity implements
        TradeSelectFragment.Listener, MakeTransactionFragment.Listener, TradeStatusFragment.Listener,
        ConfirmAddCoinUnlockWalletDialog.Listener {

    private static final String TRADE_SELECT_FRAGMENT_TAG = "trade_select_fragment_tag";
    private static final Logger log = LoggerFactory.getLogger(TradeActivity.class);
    private int containerRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_wrapper);

        containerRes = R.id.container;
        String exchangeId = getIntent().getExtras().getString("exchange_id");
        if (savedInstanceState == null) {
            if ("shapeshift".equals(exchangeId) && !getConfiguration().getShapeshiftTermsAccepted()) {
                ExchangeTermsOfUseDialog.newInstance(exchangeId).show(getFM(), "shapeshift_terms_of_use");
            } else if (!"changelly".equals(exchangeId) || getConfiguration().getChangellyTermsAccepted()) {
                startTradeFragment();
            } else {
                ExchangeTermsOfUseDialog.newInstance(exchangeId).show(getFM(), "shapeshift_terms_of_use");
            }
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
    }

    private void startTradeFragment() {
        Fragment exchange = new TradeSelectFragment();
        exchange.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(this.containerRes, exchange, "trade_select_fragment_tag").commit();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 16908332:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMakeTrade(WalletAccount fromAccount, CoinType fromType, WalletAccount toAccount, CoinType toType, Value amount, String exchange) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_ACCOUNT_ID, fromAccount.getId());
        args.putSerializable("send_from_coin_type", fromType);
        args.putString(Constants.ARG_SEND_TO_ACCOUNT_ID, toAccount.getId());
        args.putSerializable("send_to_coin_type", toType);
        if (!fromAccount.getCoinType().equals(fromType)) {
            args.putSerializable("send_value", amount);
        } else if (amount.type.equals(fromAccount.getCoinType())) {
            // TODO set the empty wallet flag in the fragment
            // Decide if emptying wallet or not
            Value lastBalance = fromAccount.getBalance();
            if (amount.compareTo(lastBalance) == 0) {
                args.putSerializable(Constants.ARG_EMPTY_WALLET, true);
            } else {
                args.putSerializable(Constants.ARG_SEND_VALUE, amount);
            }
        } else if (amount.type.equals(toType)) {
            args.putSerializable(Constants.ARG_SEND_VALUE, amount);
        } else {
            throw new IllegalStateException("Amount does not have the expected type: " + amount.type);
        }
        args.putString("exchange_id", exchange);
        replaceFragment(MakeTransactionFragment.newInstance(args), containerRes);
    }

    @Override
    public void onSignResult(@Nullable Exception error, ExchangeEntry exchangeEntry) {
        if (error != null) {
            getSupportFragmentManager().popBackStack();
            // Ignore wallet decryption errors
            if (!(error instanceof KeyCrypterException)) {
                CharSequence errorMessage = WalletUtils.getErrorMessage(this, error);
                if (errorMessage == null) {
                    if (ACRA.isInitialised()) {
                        ACRA.getErrorReporter().handleSilentException(error);
                    }
                    log.error("An unknown error occurred while sending coins", (Throwable) error);
                    errorMessage = getString(R.string.send_coins_error, new Object[]{error.getMessage()});
                }
                DialogBuilder builder = DialogBuilder.warn(this, R.string.trade_error);
                builder.setMessage(getString(R.string.trade_error_sign_tx_message, error.getMessage()));
                builder.setPositiveButton(R.string.button_ok, null)
                        .create().show();
            }
        } else if (exchangeEntry != null) {
            getSupportFragmentManager().popBackStack();
            replaceFragment(TradeStatusFragment.newInstance(exchangeEntry, true), containerRes);
        }
    }

    @Override
    public void onFinish() {
        finish();
    }

    @Override
    public void addCoin(CoinType type, String description, CharSequence password, List<ChildNumber> customPath) {
        Fragment f = getFM().findFragmentByTag(TRADE_SELECT_FRAGMENT_TAG);
        if (f != null && f.isVisible() && f instanceof TradeSelectFragment) {
            ((TradeSelectFragment) f).maybeStartAddCoinAndProceedTask(description, password, customPath);
        }
    }

    public void onTermsAgree(String exchange) {
        getConfiguration().setShapeshiftTermAccepted(true);
        switch (exchange.hashCode()) {
            case 750107009:
                if (exchange.equals("shapeshift")) {
                    getConfiguration().setShapeshiftTermAccepted(true);
                    break;
                }
                break;
            case 1455272265:
                if (exchange.equals("changelly")) {
                    getConfiguration().setChangellyTermAccepted(true);
                    break;
                }
                break;
            default:
                finish();
                return;
        }
        startTradeFragment();
    }

    public void onTermsDisagree(String exchange) {
        switch (exchange.hashCode()) {
            case 750107009:
                if (exchange.equals("shapeshift")) {
                    getConfiguration().setShapeshiftTermAccepted(false);
                    break;
                }
                break;
            case 1455272265:
                if (exchange.equals("changelly")) {
                    getConfiguration().setChangellyTermAccepted(false);
                    break;
                }
                break;
        }
        finish();
    }
}
