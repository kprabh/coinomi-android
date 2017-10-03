package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.eth.CallTransaction.Function;
import com.coinomi.core.coins.eth.Transaction;
import com.coinomi.core.coins.eth.crypto.SHA3Helper;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
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
    String from;
    BigInteger gasLimit;
    BigInteger gasPrice;
    BigInteger gasUsed;
    String hash;
    String input;
    JSONArray logs;
    BigInteger nonce;
    Long timestamp;
    String to;
    BigInteger transactionIndex;
    final Transaction tx;
    final JSONObject txJson;
    final CoinType type;
    BigInteger value;

    public EthTransaction(CoinType type, Transaction transaction, String sender) {
        this.type = type;
        this.tx = (Transaction) Preconditions.checkNotNull(transaction);
        this.gasLimit = this.tx.getGasLimit().length > 0 ? BigIntegers.fromUnsignedByteArray(this.tx.getGasLimit()) : BigInteger.ZERO;
        this.value = this.tx.getValue().length > 0 ? BigIntegers.fromUnsignedByteArray(this.tx.getValue()) : BigInteger.ZERO;
        this.nonce = this.tx.getNonce().length > 0 ? BigIntegers.fromUnsignedByteArray(this.tx.getNonce()) : BigInteger.ZERO;
        this.gasPrice = BigIntegers.fromUnsignedByteArray(this.tx.getGasPrice());
        this.to = Hex.toHexString(this.tx.getReceiveAddress());
        this.from = sender;
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
        this.to = txJson.getString("to").replace("0x", "");
        this.from = txJson.getString("from").replace("0x", "");
        this.hash = txJson.getString("hash").replace("0x", "");
        this.input = txJson.getString("input").replace("0x", "");
        this.gasLimit = new BigInteger(txJson.getString("gas").replace("0x", ""), 16);
        this.nonce = new BigInteger(txJson.getString("nonce").replace("0x", ""), 16);
        this.value = new BigInteger(txJson.getString("value").replace("0x", ""), 16);
        this.gasPrice = new BigInteger(txJson.getString("gasPrice").replace("0x", ""), 16);
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

    public JSONObject getLogs(EthFamilyWallet pocket) {
        JSONObject obj = new JSONObject();
        try {
            if (pocket.getAllContracts().containsKey("0x" + this.to)) {
                EthContract contract = (EthContract) pocket.getAllContracts().get("0x" + this.to);
                if (this.logs.length() > 0) {
                    StringBuilder logsPretify = new StringBuilder();
                    for (int j = 0; j < this.logs.length(); j++) {
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
                        }
                    }
                }
            } else {
                obj.put("parsed", false);
                obj.put("logs", this.logs);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
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
        if (wallet.getReceiveAddress().toString().replace("0x", "").equals(this.to)) {
            return Value.valueOf(wallet.getCoinType(), this.value);
        }
        Value fee = getFee();
        Value v = Value.valueOf(wallet.getCoinType(), this.value.negate());
        if (fee != null) {
            return v.subtract(fee);
        }
        return v;
    }

    public BigInteger getValueRaw() {
        return this.value;
    }

    public Value getFee() {
        if (this.gasUsed != null) {
            return Value.valueOf(this.type, this.gasUsed.multiply(this.gasPrice));
        }
        return Value.valueOf(this.type, this.gasLimit.multiply(this.gasPrice));
    }

    public TxMessage getMessage() {
        return null;
    }

    public List<AbstractAddress> getReceivedFrom() {
        return ImmutableList.of((AbstractAddress)new EthAddress(this.type, this.from));
    }

    public List<AbstractOutput> getSentTo() {
        return ImmutableList.of(new AbstractOutput(new EthAddress(this.type, this.to), Value.valueOf(this.type, this.value)));
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
