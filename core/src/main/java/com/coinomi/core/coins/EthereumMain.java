package com.coinomi.core.coins;

import com.coinomi.core.coins.families.EthFamily;

public class EthereumMain extends EthFamily {
    private static EthereumMain instance = new EthereumMain();

    private EthereumMain() {
        this.id = "ethereum.main";
        this.name = "Ethereum";
        this.symbol = "ETH";
        this.uriScheme = "ethereum";
        this.bip44Index = Integer.valueOf(60);
        this.unitExponent = Integer.valueOf(18);
        this.feeValue = value(20000000000L);
        this.minNonDust = value(1);
        this.feePolicy = FeePolicy.FEE_GAS_PRICE;
    }

    public static synchronized CoinType get() {
        CoinType coinType;
        synchronized (EthereumMain.class) {
            coinType = instance;
        }
        return coinType;
    }
}
