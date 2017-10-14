package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.WalletAccount;
import com.google.common.collect.ImmutableList;
import java.util.List;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.TransactionConfidence.Source;
import org.json.JSONArray;
import org.json.JSONObject;

public class ERC20Transaction implements AbstractTransaction {
    private AbstractAddress from;
    private final JSONArray logs;
    private AbstractOutput to;
    private final CoinType tokenType;
    private final EthTransaction tx;
    private Value value;
    private final WalletAccount wallet;

    public ERC20Transaction(EthTransaction tx, CoinType tokenType, WalletAccount wallet) {
        this.tokenType = tokenType;
        this.tx = tx;
        this.logs = tx.getLogs((EthFamilyWallet) wallet);
        this.wallet = wallet;
    }

    public CoinType getType() {
        return this.tx.getType();
    }

    @Override
    public Sha256Hash getHash() {
        return null;
    }

    public String getHashAsString() {
        return this.tx.getHashAsString();
    }

    public byte[] getHashBytes() {
        return this.tx.getHashBytes();
    }

    public ConfidenceType getConfidenceType() {
        return this.tx.getConfidenceType();
    }

    @Override
    public void setConfidenceType(ConfidenceType type) {

    }

    public int getAppearedAtChainHeight() {
        return this.tx.getAppearedAtChainHeight();
    }

    @Override
    public void setAppearedAtChainHeight(int appearedAtChainHeight) {

    }

    public Source getSource() {
        return this.tx.getSource();
    }

    @Override
    public void setSource(Source source) {

    }

    public int getDepthInBlocks() {
        return this.tx.getDepthInBlocks();
    }

    @Override
    public void setDepthInBlocks(int depthInBlocks) {

    }

    public long getTimestamp() {
        return this.tx.getTimestamp();
    }

    @Override
    public void setTimestamp(long timestamp) {

    }

    public Value getValue(AbstractWallet wallet) {
        if (this.value != null) {
            return this.value;
        }
        int i = 0;
        while (i < this.logs.length()) {
            try {
                JSONObject logEntry = this.logs.getJSONObject(i);
                if (logEntry.getBoolean("parsed") && isMine(logEntry)) {
                    JSONObject log = logEntry.getJSONObject("logs");
                    Value v = Value.valueOf(this.tokenType, log.has("value") ? log.getString("value") : log.getString("_value"));
                    if (logEntry.getString("topic").equalsIgnoreCase("Transfer")) {
                        if ((log.has("from") ? log.getString("from") : log.getString("_from")).equalsIgnoreCase(wallet.getChangeAddress().toString().replace("0x", ""))) {
                            return v.negate();
                        }
                    } else if (logEntry.getString("topic").equalsIgnoreCase("Approval")) {
                        String key = "from";
                        if (log.has("from")) {
                            key = "from";
                        } else if (log.has("_from")) {
                            key = "_from";
                        } else if (log.has("owner")) {
                            key = "owner";
                        } else if (log.has("_owner")) {
                            key = "_owner";
                        }
                        if (log.getString(key).equalsIgnoreCase(wallet.getChangeAddress().toString().replace("0x", ""))) {
                            return v.negate();
                        }
                    }
                    this.value = v;
                    return this.value;
                }
                i++;
            } catch (Exception e) {
            }
        }
        return this.tx.getValue(wallet);
    }

    public Value getFee() {
        return this.tx.getFee();
    }

    public TxMessage getMessage() {
        return this.tx.getMessage();
    }

    public List<AbstractOutput> getSentTo() {
        if (this.to != null) {
            return ImmutableList.of(this.to);
        }
        int i = 0;
        while (i < this.logs.length()) {
            try {
                JSONObject logEntry = this.logs.getJSONObject(i);
                if (logEntry.getBoolean("parsed") && isMine(logEntry)) {
                    JSONObject log = logEntry.getJSONObject("logs");
                    Value v = Value.valueOf(this.tokenType, log.has("value") ? log.getString("value") : log.getString("_value"));
                    if (logEntry.getString("topic").equalsIgnoreCase("Transfer")) {
                        return ImmutableList.of(new AbstractOutput(new EthAddress(this.tx.getType(), log.has("to") ? log.getString("to") : log.getString("_to")), v));
                    } else if (logEntry.getString("topic").equalsIgnoreCase("Approval")) {
                        String key = "to";
                        if (log.has("to")) {
                            key = "to";
                        } else if (log.has("_to")) {
                            key = "_to";
                        } else if (log.has("spender")) {
                            key = "spender";
                        } else if (log.has("_spender")) {
                            key = "_spender";
                        }
                        this.to = new AbstractOutput(new EthAddress(this.tx.getType(), log.getString(key)), v);
                        return ImmutableList.of(this.to);
                    }
                }
                i++;
            } catch (Exception e) {
            }
        }
        return this.tx.getSentTo();
    }

    public boolean isGenerated() {
        return this.tx.isGenerated();
    }

    @Override
    public boolean isTrimmed() {
        return false;
    }

    public List<AbstractAddress> getReceivedFrom() {
        if (this.from != null) {
            return ImmutableList.of(this.from);
        }
        int i = 0;
        while (i < this.logs.length()) {
            try {
                JSONObject logEntry = this.logs.getJSONObject(i);
                if (logEntry.getBoolean("parsed") && isMine(logEntry)) {
                    JSONObject log = logEntry.getJSONObject("logs");
                    if (logEntry.getString("topic").equalsIgnoreCase("Transfer")) {
                        return ImmutableList.of(this.tx.getType().newAddress(log.has("from") ? log.getString("from") : log.getString("_from")));
                    } else if (logEntry.getString("topic").equalsIgnoreCase("Approval")) {
                        String key = "from";
                        if (log.has("from")) {
                            key = "from";
                        } else if (log.has("_from")) {
                            key = "_from";
                        } else if (log.has("owner")) {
                            key = "owner";
                        } else if (log.has("_owner")) {
                            key = "_owner";
                        }
                        this.from = this.tx.getType().newAddress(log.getString(key));
                        return ImmutableList.of(this.from);
                    }
                }
                i++;
            } catch (Exception e) {
            }
        }
        return this.tx.getReceivedFrom();
    }

    public boolean isApproval() {
        int i = 0;
        while (i < this.logs.length()) {
            try {
                JSONObject logEntry = this.logs.getJSONObject(i);
                if (logEntry.getBoolean("parsed") && isMine(logEntry) && logEntry.getString("topic").equalsIgnoreCase("Approval")) {
                    return true;
                }
                i++;
            } catch (Exception e) {
            }
        }
        return false;
    }

    public boolean isMine(JSONObject log) {
        return log.toString().contains(this.wallet.getReceiveAddress().toString().replace("0x", ""));
    }
}
