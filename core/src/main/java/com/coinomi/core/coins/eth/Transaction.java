package com.coinomi.core.coins.eth;

import com.coinomi.core.coins.eth.crypto.ECKey;
import com.coinomi.core.coins.eth.crypto.ECKey.ECDSASignature;
import com.coinomi.core.coins.eth.crypto.ECKey.MissingPrivateKeyException;
import com.coinomi.core.coins.eth.crypto.HashUtil;
import com.coinomi.core.coins.eth.util.ByteUtil;
import com.coinomi.core.coins.eth.util.RLP;
import com.coinomi.core.coins.eth.util.RLPElement;
import com.coinomi.core.coins.eth.util.RLPList;
import java.math.BigInteger;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

public class Transaction {
    public static final BigInteger MINIMUM_GAS_LIMIT = new BigInteger("21000");
    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);
    private byte[] data;
    private byte[] gasLimit;
    private byte[] gasPrice;
    private byte[] hash;
    private byte[] nonce;
    private boolean parsed = false;
    private byte[] receiveAddress;
    protected byte[] rlpEncoded;
    private byte[] rlpRaw;
    private ECDSASignature signature;
    private byte[] value;

    public Transaction(byte[] nonce, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data) {
        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.receiveAddress = receiveAddress;
            this.value = value;
        this.data = data;
        if (receiveAddress == null) {
            this.receiveAddress = ByteUtil.EMPTY_BYTE_ARRAY;
        }
        this.parsed = true;
    }

    public void rlpParse() {
        RLPList transaction = (RLPList) RLP.decode2(this.rlpEncoded).get(0);
        this.nonce = ((RLPElement) transaction.get(0)).getRLPData();
        this.gasPrice = ((RLPElement) transaction.get(1)).getRLPData();
        this.gasLimit = ((RLPElement) transaction.get(2)).getRLPData();
        this.receiveAddress = ((RLPElement) transaction.get(3)).getRLPData();
        this.value = ((RLPElement) transaction.get(4)).getRLPData();
        this.data = ((RLPElement) transaction.get(5)).getRLPData();
        if (((RLPElement) transaction.get(6)).getRLPData() != null) {
            this.signature = ECDSASignature.fromComponents(((RLPElement) transaction.get(7)).getRLPData(), ((RLPElement) transaction.get(8)).getRLPData(), ((RLPElement) transaction.get(6)).getRLPData()[0]);
            } else {
                logger.debug("RLP encoded tx is not signed!");
            }
            this.parsed = true;
            this.hash = getHash();
    }

    public  byte[] getHash() {
        if (!this.parsed) {
        rlpParse();
        }
        return HashUtil.sha3(getEncoded());
    }

    public  byte[] getRawHash() {
        if (!this.parsed) {
        rlpParse();
    }
        return HashUtil.sha3(getEncodedRaw());
    }

    public  byte[] getNonce() {
        if (!this.parsed) {
        rlpParse();
    }
        return this.nonce == null ? ByteUtil.ZERO_BYTE_ARRAY : this.nonce;
    }

    public  byte[] getValue() {
        if (!this.parsed) {
        rlpParse();
    }
        return this.value == null ? ByteUtil.ZERO_BYTE_ARRAY : this.value;
    }

    public  byte[] getReceiveAddress() {
        if (!this.parsed) {
        rlpParse();
    }
        return this.receiveAddress;
    }

    public  byte[] getGasPrice() {
        if (!this.parsed) {
        rlpParse();
    }
        return this.gasPrice == null ? ByteUtil.ZERO_BYTE_ARRAY : this.gasPrice;
    }

    public  byte[] getGasLimit() {
        if (!this.parsed) {
        rlpParse();
    }
        return this.gasLimit;
    }

    public  byte[] getData() {
        if (!this.parsed) {
        rlpParse();
    }
        return this.data;
    }

    public  void sign(byte[] privKeyBytes) throws MissingPrivateKeyException {
        this.signature = ECKey.fromPrivate(privKeyBytes).decompress().sign(getRawHash());
        this.rlpEncoded = null;
    }

    public  String toString() {
        return toString(Integer.MAX_VALUE);
    }

    public  String toString(int maxDataSize) {
        String dataS;
        Object obj;
        String str;
        if (!this.parsed) {
            rlpParse();
        }
        if (this.data == null) {
            dataS = "";
        } else if (this.data.length < maxDataSize) {
            dataS = ByteUtil.toHexString(this.data);
        } else {
            dataS = ByteUtil.toHexString(Arrays.copyOfRange(this.data, 0, maxDataSize)) + "... (" + this.data.length + " bytes)";
        }
        StringBuilder append = new StringBuilder().append("TransactionData [hash=").append(ByteUtil.toHexString(this.hash)).append("  nonce=").append(ByteUtil.toHexString(this.nonce)).append(", gasPrice=").append(ByteUtil.toHexString(this.gasPrice)).append(", gas=").append(ByteUtil.toHexString(this.gasLimit)).append(", receiveAddress=").append(ByteUtil.toHexString(this.receiveAddress)).append(", value=").append(ByteUtil.toHexString(this.value)).append(", data=").append(dataS).append(", signatureV=");
        if (this.signature == null) {
            obj = "";
        } else {
            obj = Byte.valueOf(this.signature.v);
        }
        append = append.append(obj).append(", signatureR=");
        if (this.signature == null) {
            str = "";
        } else {
            str = ByteUtil.toHexString(BigIntegers.asUnsignedByteArray(this.signature.r));
        }
        append = append.append(str).append(", signatureS=");
        if (this.signature == null) {
            str = "";
        } else {
            str = ByteUtil.toHexString(BigIntegers.asUnsignedByteArray(this.signature.s));
        }
        return append.append(str).append("]").toString();
    }

    public  byte[] getEncodedRaw() {
        if (!this.parsed) {
        rlpParse();
        }
        if (this.rlpRaw != null) {
            return this.rlpRaw;
        }
        byte[] nonce;
        if (this.nonce == null || (this.nonce.length == 1 && this.nonce[0] == (byte) 0)) {
            nonce = RLP.encodeElement(null);
        } else {
            nonce = RLP.encodeElement(this.nonce);
        }
        byte[] gasPrice = RLP.encodeElement(this.gasPrice);
        byte[] gasLimit = RLP.encodeElement(this.gasLimit);
        byte[] receiveAddress = RLP.encodeElement(this.receiveAddress);
        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);
        this.rlpRaw = RLP.encodeList(nonce, gasPrice, gasLimit, receiveAddress, value, data);
        return this.rlpRaw;
    }

    public  byte[] getEncoded() {
        if (this.rlpEncoded != null) {
            return this.rlpEncoded;
        }
        byte[] nonce;
        byte[] v;
        byte[] r;
        byte[] s;
        if (this.nonce == null || (this.nonce.length == 1 && this.nonce[0] == (byte) 0)) {
            nonce = RLP.encodeElement(null);
        } else {
            nonce = RLP.encodeElement(this.nonce);
        }
        byte[] gasPrice = RLP.encodeElement(this.gasPrice);
        byte[] gasLimit = RLP.encodeElement(this.gasLimit);
        byte[] receiveAddress = RLP.encodeElement(this.receiveAddress);
        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);
        if (this.signature != null) {
            v = RLP.encodeByte(this.signature.v);
            r = RLP.encodeElement(BigIntegers.asUnsignedByteArray(this.signature.r));
            s = RLP.encodeElement(BigIntegers.asUnsignedByteArray(this.signature.s));
            } else {
            v = RLP.encodeElement(ByteUtil.EMPTY_BYTE_ARRAY);
            r = RLP.encodeElement(ByteUtil.EMPTY_BYTE_ARRAY);
            s = RLP.encodeElement(ByteUtil.EMPTY_BYTE_ARRAY);
        }

        this.rlpEncoded = RLP.encodeList(nonce, gasPrice, gasLimit,
                receiveAddress, value, data, v, r, s);

        this.hash = getHash();
        return this.rlpEncoded;
    }

    public  int hashCode() {
        byte[] hash = getHash();
        int hashCode = 0;
        for (int i = 0; i < hash.length; i++) {
            hashCode += hash[i] * i;
        }

        return hashCode;
    }

    public  boolean equals(Object obj) {
        if ((obj instanceof Transaction) && ((Transaction) obj).hashCode() == hashCode()) {
            return true;
    }
        return false;
    }

    public static Transaction create(String to, BigInteger amount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, byte[] data) {
        return new Transaction(BigIntegers.asUnsignedByteArray(nonce), BigIntegers.asUnsignedByteArray(gasPrice), BigIntegers.asUnsignedByteArray(gasLimit), Hex.decode(to), BigIntegers.asUnsignedByteArray(amount), data);
    }
    }
