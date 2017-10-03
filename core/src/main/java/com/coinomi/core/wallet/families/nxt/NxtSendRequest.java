package com.coinomi.core.wallet.families.nxt;

import com.coinomi.core.coins.BurstMain;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.FeePolicy;
import com.coinomi.core.coins.NxtMain;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.coins.nxt.Appendix.PublicKeyAnnouncement;
import com.coinomi.core.coins.nxt.Attachment;
import com.coinomi.core.coins.nxt.Convert;
import com.coinomi.core.coins.nxt.NxtException.NotValidException;
import com.coinomi.core.coins.nxt.TransactionImpl.BuilderImpl;
import com.coinomi.core.util.TypeUtils;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.SendRequest.Destination;

import static com.coinomi.core.Preconditions.checkState;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author John L. Jegutanis
 */
public class NxtSendRequest extends SendRequest<NxtTransaction, NxtAddress> {
    public BuilderImpl nxtTxBuilder;
    private final byte[] fromPubKey;

    protected NxtSendRequest(CoinType type, byte[] fromPubKey) {
        super(type);
        this.fromPubKey = fromPubKey;
    }

    public static NxtSendRequest to(NxtFamilyWallet from, NxtAddress destination, Value amount) {
        checkNotNull(destination.getType(), "Address is for an unknown network");
        checkState(from.getCoinType() == destination.getType(), "Incompatible destination address coin type");
        checkState(TypeUtils.is(destination.getType(), amount.type), "Incompatible sending amount type");
        checkTypeCompatibility(destination.getType());

        NxtSendRequest req = new NxtSendRequest(destination.getType(), from.getPublicKey());
        req.destinations.add(new Destination(destination, amount));
        req.reset();
        return req;
    }

    public static NxtSendRequest emptyWallet(NxtFamilyWallet from, NxtAddress destination) {
        checkNotNull(destination.getType(), "Address is for an unknown network");
        checkState(destination.getType().getFeePolicy() == FeePolicy.FLAT_FEE, "Only flat fee is supported");

        Value allFundsMinusFee = from.getBalance().subtract(destination.getType().getFeeValue());

        return to(from, destination, allFundsMinusFee);
    }

    private static void checkTypeCompatibility(CoinType type) {
        // Only Nxt family coins are supported
        if (!(type instanceof NxtFamily)) {
            throw new RuntimeException("Unsupported type: " + type);
        }
    }

    protected void resetImpl() {
        int timestamp;
        boolean z = true;
        if (this.destinations.size() != 1) {
            z = false;
        }
        com.coinomi.core.Preconditions.checkState(z, "NXT family can send to a single destination");
        if (this.type instanceof NxtMain) {
            timestamp = Convert.toNxtEpochTime(System.currentTimeMillis());
        } else if (this.type instanceof BurstMain) {
            timestamp = Convert.toBurstEpochTime(System.currentTimeMillis());
        } else {
            throw new RuntimeException("Unexpected NXT family type: " + this.type.toString());
        }
        Destination<NxtAddress> destination = (Destination) this.destinations.get(0);
        BuilderImpl builder = new BuilderImpl((byte) 1, this.fromPubKey, destination.amount.getValue(), this.fee.getValue(), timestamp, (short) 1440, Attachment.ORDINARY_PAYMENT);
        builder.recipientId(((NxtAddress) destination.to).getAccountId());
        if (((NxtAddress) destination.to).getPublicKey() != null) {
            builder.publicKeyAnnouncement(new PublicKeyAnnouncement(((NxtAddress) destination.to).getPublicKey()));
        }
        this.nxtTxBuilder = builder;
    }

    public void setEcBlock(long lastEcBlockId, int lastEcBlockHeight) {
        this.nxtTxBuilder.ecBlockId(lastEcBlockId);
        this.nxtTxBuilder.ecBlockHeight(lastEcBlockHeight);
    }

    public void buildTx() throws NotValidException {
        setTransaction(new NxtTransaction(this.type, this.nxtTxBuilder.build()));
    }
}
