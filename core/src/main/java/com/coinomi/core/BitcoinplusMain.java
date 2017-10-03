package com.coinomi.core;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.SoftDustPolicy;
import com.coinomi.core.coins.families.PeerFamily;

public class BitcoinplusMain extends PeerFamily {
    private static BitcoinplusMain instance = new BitcoinplusMain();

    private BitcoinplusMain() {
        this.id = "bitcoinplus.main";
        this.addressHeader = 25;
        this.p2shHeader = 85;
        this.acceptableAddressCodes = new int[]{this.addressHeader, this.p2shHeader};
        this.spendableCoinbaseDepth = 100;
        this.dumpedPrivateKeyHeader = 153;
        this.name = "Bitcoinplus";
        this.symbol = "XBC";
        this.uriScheme = "bitcoinplus";
        this.bip44Index = Integer.valueOf(65);
        this.unitExponent = Integer.valueOf(8);
        this.feeValue = value(100000);
        this.minNonDust = value(1000);
        this.softDustLimit = value(10000);
        this.softDustPolicy = SoftDustPolicy.BASE_FEE_FOR_EACH_SOFT_DUST_TXO;
        this.signedMessageHeader = CoinType.toBytes("Bitcoinplus Signed Message:\n");
    }

    public static synchronized CoinType get() {
        CoinType coinType;
        synchronized (BitcoinplusMain.class) {
            coinType = instance;
        }
        return coinType;
    }
}
