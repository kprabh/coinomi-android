package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.coinomi.wallet.R;

public class AddContractActivity extends BaseWalletActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        if (savedInstanceState == null) {
            Fragment fragment = AddContractFragment.newInstance();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add((int) R.id.container, fragment).commit();
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
    }
}
