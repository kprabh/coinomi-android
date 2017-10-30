package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.sponsor.Sponsors.Sponsor;
import com.coinomi.sponsor.TokenSaleProxy.SaleDeposit;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.coinomi.wallet.ui.MakeTransactionFragment.Listener;
import com.coinomi.wallet.ui.dialogs.ConfirmAddCoinUnlockWalletDialog;
import com.coinomi.wallet.util.WalletUtils;
import java.util.List;
import org.acra.ACRA;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.KeyCrypterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenSaleActivity extends BaseWalletActivity implements Listener, TokenSaleFragment.Listener, ConfirmAddCoinUnlockWalletDialog.Listener {
    private static final Logger log = LoggerFactory.getLogger(TokenSaleActivity.class);
    Sponsor sponsor;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.sponsor = (Sponsor) Preconditions.checkNotNull(getIntent().getExtras().getSerializable("sponsor"));
        setContentView((int) R.layout.activity_fragment_wrapper);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, TokenSaleFragment.getInstance(this.sponsor), "token_sale_fragment_tag").commit();
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(this.sponsor.primary);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
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

    public void addCoin(CoinType type, String description, CharSequence password, List<ChildNumber> customPath) {
        Fragment f = getFM().findFragmentByTag("token_sale_fragment_tag");
        if (f != null && f.isVisible() && (f instanceof AbstractSendFragment)) {
            ((AbstractSendFragment) f).maybeStartAddCoinAndProceedTask(type, description, password, customPath);
        }
    }

    public void onParticipate(WalletAccount fromAccount, SaleDeposit deposit, Value amount) {
        if (fromAccount.getCoinType().equals(deposit.address.getType())) {
            Bundle args = new Bundle();
            args.putString("account_id", fromAccount.getId());
            args.putSerializable("send_to_address", deposit.address);
            if (amount.type.equals(fromAccount.getCoinType())) {
                if (amount.compareTo(fromAccount.getBalance()) == 0) {
                    args.putSerializable("empty_wallet", Boolean.valueOf(true));
                } else {
                    args.putSerializable("send_value", amount);
                }
                if (deposit.extraData != null) {
                    args.putSerializable("contract_data", deposit.extraData);
                }
                replaceFragment(MakeTransactionFragment.newInstance(args), R.id.container);
                return;
            }
            throw new IllegalStateException("Amount does not have the expected type: " + amount.type);
        }
        throw new IllegalStateException("Destination type does not match the source account");
    }

    public void onSignResult(Exception error, ExchangeEntry exchangeEntry) {
        if (error != null) {
            getSupportFragmentManager().popBackStack();
            if (!(error instanceof KeyCrypterException)) {
                CharSequence errorMessage = WalletUtils.getErrorMessage(this, error);
                if (errorMessage == null) {
                    if (ACRA.isInitialised()) {
                        ACRA.getErrorReporter().handleSilentException(error);
                    }
                    log.error("An unknown error occurred while sending coins", (Throwable) error);
                    errorMessage = getString(R.string.send_coins_error, new Object[]{error.getMessage()});
                }
                DialogBuilder builder = new DialogBuilder(this);
                builder.setMessage(errorMessage);
                builder.setPositiveButton((int) R.string.button_ok, null).create().show();
                return;
            }
            return;
        }
        getSupportFragmentManager().popBackStack();
        DialogBuilder builder = DialogBuilder.info(this, this.sponsor.primary);
        builder.setMessage((int) R.string.token_sale_final);
        builder.setPositiveButton((int) R.string.button_ok, null).create().show();
    }
}
