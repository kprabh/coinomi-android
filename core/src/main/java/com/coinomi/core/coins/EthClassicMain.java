package com.coinomi.core.coins;

import com.coinomi.core.coins.families.EthFamily;

public class EthClassicMain extends EthFamily {
    private static EthClassicMain instance = new EthClassicMain();

    private EthClassicMain() {
        this.id = "ethclassic.main";
        this.name = "Ethereum Classic";
        this.symbol = "ETC";
        this.uriScheme = "ethclassic";
        this.bip44Index = Integer.valueOf(61);
        this.unitExponent = Integer.valueOf(18);
        this.feeValue = value(20000000000L);
        this.minNonDust = value(1);
        this.feePolicy = FeePolicy.FEE_GAS_PRICE;
    }

    public static synchronized CoinType get() {
        CoinType coinType;
        synchronized (EthClassicMain.class) {
            coinType = instance;
        }
        return coinType;
    }
}
