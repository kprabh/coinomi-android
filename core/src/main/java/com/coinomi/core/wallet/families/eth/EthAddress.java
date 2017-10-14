package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.eth.crypto.HashUtil;
import com.coinomi.core.coins.eth.crypto.SHA3Helper;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.wallet.AbstractAddress;

import org.spongycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

public final class EthAddress implements AbstractAddress {
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^0x[0-9a-fA-F]{40}$");
    private static final Pattern HASH_PATTERN_L = Pattern.compile("^[0-9a-f]{40}$");
    private static final Pattern HASH_PATTERN_U = Pattern.compile("^[0-9A-F]{40}$");
    private final String address;
    private final CoinType type;

    private enum Variant {
        ADDRESS,
        HASH_U,
        HASH_L
    }

    public EthAddress(CoinType type, String address) throws AddressMalformedException {
        Variant variant;
        if (ADDRESS_PATTERN.matcher(address).matches()) {
            variant = Variant.ADDRESS;
        } else if (HASH_PATTERN_U.matcher(address).matches()) {
            variant = Variant.HASH_U;
        } else if (HASH_PATTERN_L.matcher(address).matches()) {
            variant = Variant.HASH_L;
        } else {
            throw new AddressMalformedException("Address " + address + " is not a valid " + EthAddress.class);
        }
        switch (variant) {
            case ADDRESS:
                if (hasChecksum(address) && !validateChecksum(address)) {
                    throw new AddressMalformedException("Checksum is not correct");
                }
            case HASH_U:
            case HASH_L:
                break;
            default:
                throw new AddressMalformedException("Unknown address variant");
        }
        this.address = toChecksumAddress(address);
        this.type = type;
        //this.address = address.replace("0x", "").toLowerCase();
    }

    public CoinType getType() {
        return this.type;
    }

    public long getId() {
        return ByteBuffer.wrap(getHash160()).getLong();
    }

    public String toString() {
        return address.toLowerCase();
    }

    public String getHexString() {
        return this.address.substring(2).toLowerCase();
    }

    private static String toChecksumAddress(String address) {
        address = address.replace("0x", "").toLowerCase();
        Preconditions.checkState(address.length() == 40);
        char[] addressChar = address.toCharArray();
        char[] addressHash = Hex.toHexString(SHA3Helper.sha3(address.getBytes())).toCharArray();
        StringBuilder sb = new StringBuilder(42);
        sb.append("0x");
        for (int i = 0; i < address.length(); i++) {
            if (Character.digit(addressHash[i], 16) >= 8) {
                sb.append(Character.toUpperCase(addressChar[i]));
            } else {
                sb.append(addressChar[i]);
            }
        }
        return sb.toString();
    }

    private static boolean validateChecksum(String address) {
        return toChecksumAddress(address).equals(address);
    }

    public byte[] getHash160() {
        return Hex.decode(getHexString());
    }

    public boolean equals(AbstractAddress o) {
        if (this == o) {
            return true;
        }
        if (o != null && getClass() == o.getClass() && this.type.equals(o.getType())) {
            return toString().equals(o.toString());
        }
        return false;
    }

    public static boolean hasChecksum(String address) {
        return (address.toLowerCase().equals(address) || address.toUpperCase().replace("0X", "0x").equals(address)) ? false : true;
    }
}
