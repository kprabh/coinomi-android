package com.coinomi.wallet.ui;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.coinomi.wallet.R;

public class ContractDetailsActivity extends BaseWalletActivity {
    protected void onCreate(Bundle savedInstanceState) {
        boolean showDefault = true;
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (VERSION.SDK_INT >= 17 && !extras.getBoolean("show_default", true)) {
                showDefault = true;
            }
            Fragment fragment;
            if (showDefault) {
                fragment = new ContractDetailsFragment();
                fragment.setArguments(extras);
                getSupportFragmentManager().beginTransaction().add((int) R.id.container, fragment).commit();
                return;
            }
            fragment = new SmartInFragment();
            fragment.setArguments(extras);
            getSupportFragmentManager().beginTransaction().add((int) R.id.container, fragment).commit();
        }
    }

    public void setActionBarTitle(String actionBarTitle) {
        getSupportActionBar().setTitle((CharSequence) actionBarTitle);
    }
}
