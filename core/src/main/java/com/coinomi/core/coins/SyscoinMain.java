package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;

public class SyscoinMain extends BitFamily {
    private static SyscoinMain instance = new SyscoinMain();

    private SyscoinMain() {
        this.id = "syscoin.main";
        this.addressHeader = 63;
        this.p2shHeader = 5;
        this.acceptableAddressCodes = new int[]{this.addressHeader, this.p2shHeader};
        this.spendableCoinbaseDepth = 100;
        this.dumpedPrivateKeyHeader = 191;
        this.name = "Syscoin";
        this.symbol = "SYS";
        this.uriScheme = "syscoin";
        this.bip44Index = Integer.valueOf(57);
        this.unitExponent = Integer.valueOf(8);
        this.feeValue = value(1000);
        this.minNonDust = value(546);
        this.softDustLimit = this.minNonDust;
        this.softDustPolicy = SoftDustPolicy.NO_POLICY;
        this.signedMessageHeader = CoinType.toBytes("Syscoin Signed Message:\n");
    }

    public static synchronized CoinType get() {
        CoinType coinType;
        synchronized (SyscoinMain.class) {
            coinType = instance;
        }
        return coinType;
    }
}
