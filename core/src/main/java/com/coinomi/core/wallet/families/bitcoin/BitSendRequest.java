package com.coinomi.core.wallet.families.bitcoin;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.util.TypeUtils;
import com.coinomi.core.wallet.SendRequest;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Address;

import static com.coinomi.core.Preconditions.checkState;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Iterator;

/**
 * @author John L. Jegutanis
 */
public class BitSendRequest extends SendRequest<BitTransaction, BitAddress> {
    @Override
    protected void resetImpl() {
        Transaction tx = new Transaction(this.type);
        Iterator it = this.destinations.iterator();
        while (it.hasNext()) {
            Destination<BitAddress> destination = (Destination) it.next();
            tx.addOutput(destination.amount.toCoin(), (Address) destination.to);
        }
        setTransaction(new BitTransaction(tx));
    }

    public BitSendRequest(CoinType type) {
        super(type);
    }

    /**
     * <p>Creates a new SendRequest to the given address for the given value.</p>
     *
     * <p>Be very careful when value is smaller than {@link Transaction#MIN_NONDUST_OUTPUT} as the transaction will
     * likely be rejected by the network in this case.</p>
     */

    public static BitSendRequest to(BitAddress destination, Value amount) {
        checkNotNull(destination.getType(), "Address is for an unknown network");
        checkState(TypeUtils.is(destination.getType(), amount.type), "Incompatible sending amount type");
        checkTypeCompatibility(destination.getType());

        BitSendRequest req = new BitSendRequest(destination.getType());
        req.destinations.add(new Destination(destination, amount));
        req.reset();

        return req;
    }

    public static BitSendRequest emptyWallet(BitAddress destination) {
        checkNotNull(destination.getType(), "Address is for an unknown network");
        checkTypeCompatibility(destination.getType());

        BitSendRequest req = new BitSendRequest(destination.getType());
        req.emptyWallet = true;
        req.destinations.add(new Destination(destination, destination.getType().zeroCoin()));
        req.reset();
        return req;
    }

    private static void checkTypeCompatibility(CoinType type) {
        // Only Bitcoin family coins are supported
        if (!(type instanceof BitFamily)) {
            throw new RuntimeException("Unsupported type: " + type);
        }
    }
}
