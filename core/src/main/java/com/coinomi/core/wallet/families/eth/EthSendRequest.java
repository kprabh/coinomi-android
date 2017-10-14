package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.FeePolicy;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.eth.Transaction;
import com.coinomi.core.coins.families.EthFamily;
import com.coinomi.core.exceptions.InsufficientMoneyException;
import com.coinomi.core.util.TypeUtils;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.WalletAccount.WalletAccountException;
import java.math.BigInteger;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

public class EthSendRequest extends SendRequest<EthTransaction, EthAddress> {
    private Value walletFullBalance;

    protected void resetImpl() {
    }

    protected EthSendRequest(CoinType type) {
        super(type);
    }

    public static EthSendRequest to(EthFamilyWallet from, EthAddress destination, Value amount, byte[] contractData) {
        return to(from, destination, amount, contractData, null);
    }

    private static EthSendRequest to(EthFamilyWallet from, EthAddress destination, Value amount, byte[] contractData, EthSendRequest req) {
        CoinType t = from.getCoinType();
        Preconditions.checkState(t.equals(destination.getType()), "Incompatible destination address coin type");
        Preconditions.checkState(TypeUtils.is(destination.getType(), amount.type), "Incompatible sending amount type");
        Preconditions.checkState(t.getFeePolicy() == FeePolicy.FEE_GAS_PRICE, "Fee policy must be: " + FeePolicy.FEE_GAS_PRICE);
        checkTypeCompatibility(destination.getType());
        if (req == null) {
            req = new EthSendRequest(destination.getType());
        } else {
            Preconditions.checkState(t.equals(req.type));
        }
        req.walletFullBalance = from.getBalance();
        req.setTransaction(new EthTransaction(from.getAddress(), destination, amount, getNonce(from), req.feePerTxSize, BigInteger.ZERO, contractData));
        req.setCompleted(true);
        return req;
    }

    public static EthSendRequest emptyWallet(EthFamilyWallet from, EthAddress destination, byte[] contractData) {
        CoinType t = from.getCoinType();
        Preconditions.checkState(t.equals(destination.getType()), "Incompatible destination address coin type");
        Preconditions.checkState(t.getFeePolicy() == FeePolicy.FEE_GAS_PRICE, "Fee policy must be: " + FeePolicy.FEE_GAS_PRICE);
        EthSendRequest req = new EthSendRequest(destination.getType());
        req.emptyWallet = true;
        return to(from, destination, from.getBalance(), contractData, req);
    }

    private static void checkTypeCompatibility(CoinType type) {
        if (!(type instanceof EthFamily)) {
            throw new RuntimeException("Unsupported type: " + type);
        }
    }

    public static BigInteger getNonce(EthFamilyWallet from) {
        return from.getNonce();
    }

    public void updateGasLimit(BigInteger newGas) throws WalletAccountException {
        if (newGas.compareTo(Transaction.MINIMUM_GAS_LIMIT) < 0) {
            throw new WalletAccountException("Invalid gas limit");
        }
        Value v;
        EthTransaction tx = (EthTransaction) com.google.common.base.Preconditions.checkNotNull(getTx());
        byte[] data = null;
        if (tx.getData() != null) {
            data = Hex.decode(tx.getData());
        }
        Value gasPrice = this.feePerTxSize;
        Value newFees = gasPrice.multiply(newGas);
        if (this.emptyWallet) {
            v = this.walletFullBalance.subtract(newFees);
            if (v.signum() < 0) {
                throw new WalletAccountException(new InsufficientMoneyException(v.negate()));
            }
        }
        v = tx.value;
        Value remaining = this.walletFullBalance.subtract(v.add(newFees));
        if (remaining.signum() < 0) {
            throw new WalletAccountException(new InsufficientMoneyException(remaining.negate()));
        }
        setTransaction(new EthTransaction(tx.from, tx.to, v, tx.nonce, gasPrice, newGas, data));
    }

    public JSONObject getJsonDetails() throws JSONException {
        EthTransaction tx = (EthTransaction) com.google.common.base.Preconditions.checkNotNull(getTx());
        JSONObject details = new JSONObject();
        details.put("from", tx.from);
        details.put("to", tx.to);
        details.put("value", tx.getValueHex());
        if (tx.getData() != null) {
            details.put("data", tx.getData());
        }
        return details;
    }
}
