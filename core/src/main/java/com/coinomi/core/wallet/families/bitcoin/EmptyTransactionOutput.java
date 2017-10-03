package com.coinomi.core.wallet.families.bitcoin;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.CoinID;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.spongycastle.util.encoders.Hex;

import java.util.HashMap;
import java.util.List;

/**
 * @author John L. Jegutanis
 */
public class EmptyTransactionOutput extends TransactionOutput {
    private static List<CoinType> types = CoinID.getSupportedCoins();
    private static HashMap<CoinType, EmptyTransactionOutput> instances = new HashMap(types.size());
    private static byte[] fakeScript = Hex.decode("76a914000000000000000000000000000000000000000088ac");

    static {
        for (CoinType type : types) {
            instances.put(type, new EmptyTransactionOutput(type, null, Coin.ZERO, fakeScript));
        }
    }

    private EmptyTransactionOutput(NetworkParameters params, Transaction parent, Coin value, byte[] scriptBytes) {
        super(params, parent, value, scriptBytes);
    }

    public static synchronized EmptyTransactionOutput get(CoinType type) {
        EmptyTransactionOutput emptyTransactionOutput;
        synchronized (EmptyTransactionOutput.class) {
            emptyTransactionOutput = (EmptyTransactionOutput) instances.get(type);
        }
        return emptyTransactionOutput;
    }

    @Override
    public int getIndex() {
        throw new IllegalArgumentException("Empty outputs don't have indexes");
    }
}
