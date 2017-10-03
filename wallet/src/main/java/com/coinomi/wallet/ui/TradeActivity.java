package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.coinomi.wallet.R;
import com.coinomi.wallet.tasks.AddCoinTask;
import com.coinomi.wallet.ui.dialogs.ConfirmAddCoinUnlockWalletDialog;
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

        if (savedInstanceState == null) {if (getConfiguration().getShapeshiftTermsAccepted()) {
            getSupportFragmentManager().beginTransaction()
                    .add(containerRes, new TradeSelectFragment(), TRADE_SELECT_FRAGMENT_TAG)
                    .commit();} else {
            ShapeshiftTermsOfUseDialog.newInstance().show(getFM(), "shapeshift_terms_of_use");
        }
        }
    }

    @Override
    public void onMakeTrade(WalletAccount fromAccount, WalletAccount toAccount, Value amount) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_ACCOUNT_ID, fromAccount.getId());
        args.putString(Constants.ARG_SEND_TO_ACCOUNT_ID, toAccount.getId());
        if (amount.type.equals(fromAccount.getCoinType())) {
            // TODO set the empty wallet flag in the fragment
            // Decide if emptying wallet or not
            Value lastBalance = fromAccount.getBalance();
            if (amount.compareTo(lastBalance) == 0) {
                args.putSerializable(Constants.ARG_EMPTY_WALLET, true);
            } else {
                args.putSerializable(Constants.ARG_SEND_VALUE, amount);
            }
        } else if (amount.type.equals(toAccount.getCoinType())) {
            args.putSerializable(Constants.ARG_SEND_VALUE, amount);
        } else {
            throw new IllegalStateException("Amount does not have the expected type: " + amount.type);
        }

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

    public void onTermsAgree() {
        getConfiguration().setShapeshiftTermAccepted(true);
        getSupportFragmentManager().beginTransaction().add(this.containerRes, new TradeSelectFragment(), "trade_select_fragment_tag").commit();
    }

    public void onTermsDisagree() {
        getConfiguration().setShapeshiftTermAccepted(false);
        finish();
    }
}
