package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.coinomi.wallet.R;

public class ContractFunctionActivity extends BaseWalletActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        if (savedInstanceState == null) {
            Fragment fragment = new ContractFunctionFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add((int) R.id.container, fragment).commit();
        }
    }
}
