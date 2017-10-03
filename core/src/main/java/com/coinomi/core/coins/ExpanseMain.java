package com.coinomi.core.coins;

import com.coinomi.core.coins.families.EthFamily;

public class ExpanseMain extends EthFamily {
    private static ExpanseMain instance = new ExpanseMain();

    private ExpanseMain() {
        this.id = "expanse.main";
        this.name = "Expanse";
        this.symbol = "EXP";
        this.uriScheme = "expanse";
        this.bip44Index = Integer.valueOf(40);
        this.unitExponent = Integer.valueOf(18);
        this.feeValue = value(20000000000L);
        this.minNonDust = value(1);
        this.feePolicy = FeePolicy.FEE_GAS_PRICE;
    }

    public static synchronized CoinType get() {
        CoinType coinType;
        synchronized (ExpanseMain.class) {
            coinType = instance;
        }
        return coinType;
    }
}
