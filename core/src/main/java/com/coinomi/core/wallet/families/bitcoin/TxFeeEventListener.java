package com.coinomi.core.wallet.families.bitcoin;

import com.coinomi.core.coins.Value;

public interface TxFeeEventListener {
    void onFeeEstimate(Value value);
}
