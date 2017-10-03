package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.ChangePasswordIntroFragment.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResetPasswordActivity extends BaseWalletActivity implements Listener, RestoreFragment.Listener, SetPasswordFragment.Listener {
    private static final Logger log = LoggerFactory.getLogger(ResetPasswordActivity.class);
    private boolean forceSeedRestore;
    private Wallet wallet;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        this.forceSeedRestore = getIntent().getBooleanExtra("force_seed_restore", false);
        this.wallet = getWalletApplication().getWallet();
        if (this.wallet == null) {
            Toast.makeText(this, R.string.error_generic, 1).show();
            finish();
        } else if (savedInstanceState == null) {
            FragmentTransaction t = getSupportFragmentManager().beginTransaction();
            if (this.forceSeedRestore || !this.wallet.isEncrypted()) {
                t.add((int) R.id.container, RestoreFragment.newInstanceForResettingEncryption());
            } else {
                t.add((int) R.id.container, ChangePasswordIntroFragment.newInstance());
            }
            t.commit();
        }
    }

    private void replaceFragment(Fragment fragment) {
        replaceFragment(fragment, R.id.container);
    }

    public void onForgotPassword() {
        replaceFragment(RestoreFragment.newInstanceForResettingEncryption());
    }

    public void onCurrentPasswordVerified(Bundle args) {
        replaceFragment(SetPasswordFragment.newInstance(args));
    }

    public void onSeedVerified(Bundle args) {
        replaceFragment(SetPasswordFragment.newInstance(args));
    }

    public void onPasswordSet(Bundle args) {
        replaceFragment(FinalizeWalletPasswordResetFragment.newInstance(args));
    }
}
