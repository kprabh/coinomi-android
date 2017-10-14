package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.eth.CallTransaction.Function;
import com.coinomi.core.coins.eth.Transaction;
import com.coinomi.core.coins.eth.crypto.SHA3Helper;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.TransactionConfidence.Source;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

public class EthTransaction implements AbstractTransaction {
    String blockHash;
    BigInteger blockNumber;
    BigInteger cumulativeGasUsed;
    int depth;
    EthAddress from;
    BigInteger gasLimit;
    Value gasPrice;
    BigInteger gasUsed;
    String hash;
    String input;
    JSONArray logs;
    BigInteger nonce;
    Long timestamp;
    EthAddress to;
    BigInteger transactionIndex;
    final Transaction tx;
    final JSONObject txJson;
    final CoinType type;
    Value value;

    public EthTransaction(EthAddress from, EthAddress to, Value amount, BigInteger nonce, Value gasPrice, BigInteger gasLimit, byte[] data) {
        Preconditions.checkState(to.getType().equals(from.getType()));
        this.to = to;
        this.from = from;
        CoinType t = from.getType();
        if (t.isSubType()) {
            this.type = t.getParentType();
        } else {
            this.type = t;
        }
        this.gasLimit = gasLimit;
        this.value = amount;
        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.tx = Transaction.create(to.getHexString(), amount.getBigInt(), nonce, gasPrice.getBigInt(), gasLimit, data);
        this.txJson = new JSONObject();
        this.logs = new JSONArray();
    }

    public EthTransaction(CoinType type, JSONObject txJson) throws JSONException {
        this.type = type;
        this.txJson = txJson;
        if (txJson.has("timestamp")) {
            this.blockHash = txJson.getString("blockHash").replace("0x", "");
            this.timestamp = Long.valueOf(txJson.getString("timestamp"));
            this.blockNumber = new BigInteger(txJson.getString("blockNumber"));
            this.transactionIndex = new BigInteger(txJson.getString("transactionIndex").replace("0x", ""), 16);
        }
        try {
            this.to = new EthAddress(type, txJson.getString("to"));
            this.from = new EthAddress(type, txJson.getString("from"));
        this.hash = txJson.getString("hash").replace("0x", "");
        this.input = txJson.getString("input").replace("0x", "");
        this.gasLimit = new BigInteger(txJson.getString("gas").replace("0x", ""), 16);
        this.nonce = new BigInteger(txJson.getString("nonce").replace("0x", ""), 16);
            this.value = type.value(new BigInteger(txJson.getString("value").replace("0x", ""), 16));
            this.gasPrice = type.value(new BigInteger(txJson.getString("gasPrice").replace("0x", ""), 16));
        if (txJson.has("cumulativeGasUsed")) {
            this.cumulativeGasUsed = new BigInteger(txJson.getString("cumulativeGasUsed").replace("0x", ""), 16);
        }
        if (txJson.has("gasUsed")) {
            this.gasUsed = new BigInteger(txJson.getString("gasUsed").replace("0x", ""), 16);
        }
        if (txJson.has("logs")) {
            this.logs = txJson.getJSONArray("logs");
        } else {
            this.logs = new JSONArray();
        }
        this.tx = null;
        } catch (AddressMalformedException e) {
            throw new JSONException(e);
        }
    }

    public static EthTransaction fromJSON(CoinType type, String jsonString) throws JSONException {
        return new EthTransaction(type, new JSONObject(jsonString));
    }

    public BigInteger getNonce() {
        return this.nonce;
    }

    public CoinType getType() {
        return this.type;
    }

    public Sha256Hash getHash() {
        return new Sha256Hash(this.hash);
    }

    public String getHashAsString() {
        return this.hash;
    }

    public byte[] getHashBytes() {
        return Hex.decode(this.hash);
    }

    public ConfidenceType getConfidenceType() {
        return this.timestamp == null ? ConfidenceType.PENDING : ConfidenceType.BUILDING;
    }

    @Override
    public void setConfidenceType(ConfidenceType type) {

    }

    public int getAppearedAtChainHeight() {
        return this.blockNumber.intValue();
    }

    @Override
    public void setAppearedAtChainHeight(int appearedAtChainHeight) {

    }

    public Source getSource() {
        return Source.NETWORK;
    }

    @Override
    public void setSource(Source source) {

    }

    public boolean hasLogs() {
        return this.logs.length() > 0;
    }

    public JSONArray getLogs(EthFamilyWallet pocket) {
        JSONArray logsArray = new JSONArray();
        try {
            EthContract contract = pocket.getContract(this.to);
            if (contract == null) {
                logsArray = logsArray.put(new JSONObject().put("parsed", false).put("logs", this.logs));
            } else if (this.logs.length() > 0) {
                    for (int j = 0; j < this.logs.length(); j++) {
                    StringBuilder logsPretify = new StringBuilder();
                    JSONObject obj = new JSONObject();
                        JSONObject log = this.logs.getJSONObject(j);
                        JSONArray topics = log.getJSONArray("topics");
                        Function f = contract.getContract().getByTopic(topics.getString(0).replace("0x", ""));
                        if (f != null) {
                            for (int i = 1; i < topics.length(); i++) {
                                logsPretify.append(topics.getString(i).replace("0x", ""));
                            }
                            logsPretify.append(log.getString("data").replace("0x", ""));
                            obj.put("logs", f.resultToJSON(logsPretify.toString(), f.inputs));
                            obj.put("topic", f.name);
                            obj.put("parsed", true);
                        logsArray.put(obj);
                        }
                    }
                }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return logsArray;
    }

    public String getJSONString() {
        return this.txJson.toString();
    }

    public int getDepthInBlocks() {
        return this.depth;
    }

    public void setDepthInBlocks(int lastSeenBlock) {
        if (getTimestamp() > 0) {
            this.depth = lastSeenBlock - getAppearedAtChainHeight();
            this.depth = this.depth < 0 ? 0 : this.depth;
        }
    }

    public long getTimestamp() {
        return this.timestamp == null ? 0 : this.timestamp.longValue();
    }

    @Override
    public void setTimestamp(long timestamp) {

    }

    public Value getValue(AbstractWallet wallet) {
        Preconditions.checkState(this.type.equals(wallet.getCoinType()));
        if (wallet.isAddressMine(this.to)) {
            return this.value;
        }
        if (!wallet.isAddressMine(this.from)) {
            return this.type.zeroCoin();
        }
        Value fee = getFee();
        Value v = this.value.negate();
        if (fee != null) {
            return v.subtract(fee);
        }
        return v;
    }

    String getValueHex() {
        return "0x" + this.value.getBigInt().toString(16);
    }

    public Value getFee() {
        if (this.gasUsed != null) {
            return this.gasPrice.multiply(this.gasUsed);
        }
        return this.gasPrice.multiply(this.gasLimit);
    }

    public TxMessage getMessage() {
        return null;
    }

    public List<AbstractAddress> getReceivedFrom() {
        return ImmutableList.of((AbstractAddress)this.from);
    }

    public List<AbstractOutput> getSentTo() {
        return ImmutableList.of(new AbstractOutput(this.to, this.value));
    }

    public boolean isGenerated() {
        return false;
    }

    @Override
    public boolean isTrimmed() {
        return false;
    }

    public void sign(byte[] privKey) {
        if (this.tx != null) {
            this.tx.sign(privKey);
            this.hash = Hex.toHexString(SHA3Helper.sha3(this.tx.getEncoded()));
        }
    }

    public String getData() {
        if (this.input != null || this.tx.getData() == null) {
            return this.input;
        }
        return Hex.toHexString(this.tx.getData());
    }

    public byte[] getRawTransaction() {
        return this.tx.getEncoded();
    }
}
