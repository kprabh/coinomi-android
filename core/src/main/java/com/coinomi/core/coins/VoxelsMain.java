package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.coins.families.ReddFamily;

import org.bitcoinj.core.Coin;

/**
 * @author Ahmed Bodiwala
 */
public class VoxelsMain extends BitFamily {
    private VoxelsMain() {
        id = "Voxels.main";

        addressHeader = 70;
        p2shHeader = 5;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 100;
        dumpedPrivateKeyHeader = 128;

        name = "Voxels (beta)";
        symbol = "VOX";
        uriScheme = "Voxels";
        bip44Index = 129;
        unitExponent = 8;
        feeValue = value(100000);
        minNonDust = value(1000000);
        softDustLimit = value(100000000);
        softDustPolicy = SoftDustPolicy.BASE_FEE_FOR_EACH_SOFT_DUST_TXO;
        signedMessageHeader = toBytes("Voxels Signed Message:\n");
        
    }

    private static VoxelsMain instance = new VoxelsMain();
    public static synchronized VoxelsMain get() {
        return instance;
    }
}
