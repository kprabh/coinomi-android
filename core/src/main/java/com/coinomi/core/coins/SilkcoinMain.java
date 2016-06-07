package com.coinomi.core.coins;

import com.coinomi.core.coins.families.PeerFamily;

/**
 * @author Ahmed Bodiwala
 */
public class SilkcoinMain extends PeerFamily {
    private SilkcoinMain() {
        id = "Silkcoin.main";

        addressHeader = 25;
        p2shHeader = 85;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 500;
        dumpedPrivateKeyHeader = 153;

        name = "Silkcoin";
        symbol = "SILK";
        uriScheme = "Silkcoin";
        bip44Index = 82;
        unitExponent = 8;
        feeValue = value(10000); // 0.0001 SILK
        minNonDust = value(1);
        softDustLimit = value(1000000); // 0.01 SILK
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT;
        signedMessageHeader = toBytes("Silkcoin Signed Message:\n");
    }

    private static SilkcoinMain instance = new SilkcoinMain();
    public static synchronized CoinType get() {
        return instance;
    }
}
