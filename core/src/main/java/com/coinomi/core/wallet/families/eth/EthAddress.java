package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.eth.crypto.HashUtil;
import com.coinomi.core.wallet.AbstractAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public final class EthAddress implements AbstractAddress {
    private final String address;
    private final CoinType type;

    public EthAddress(CoinType type, String address) {
        this.type = type;
        this.address = address.replace("0x", "").toLowerCase();
    }

    public CoinType getType() {
        return this.type;
    }

    public long getId() {
        return ByteBuffer.wrap(getHash160()).getLong();
    }

    public String toString() {
        return "0x" + this.address;
    }

    public String getHexString() {
        return this.address;
    }

    public byte[] getHash160() {
        return HashUtil.ripemd160(this.address.getBytes(Charset.forName("utf-8")));
    }
}
