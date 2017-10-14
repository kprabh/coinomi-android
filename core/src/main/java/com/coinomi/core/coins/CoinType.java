package com.coinomi.core.coins;


import com.coinomi.core.coins.families.Families;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.messages.MessageFactory;
import com.coinomi.core.util.MonetaryFormat;
import com.coinomi.core.wallet.AbstractAddress;
import com.google.common.base.Charsets;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.params.AbstractBitcoinNetParams;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author John L. Jegutanis
 */
abstract public class CoinType extends AbstractBitcoinNetParams implements ValueType, Serializable {
    private static int[] EMPTY_HEADERS = new int[0];
    protected String[] altSymbols;
    protected boolean hasDynamicFees;
    protected boolean hasSelectableFees;
    protected String icon;
    protected Value minFeeValue;
    private transient Value zeroCoin;
    protected CoinType parentType;
    protected int[] p2shHeaderExtras;

    private static class DummyNetParams extends AbstractBitcoinNetParams {
        private static DummyNetParams instance = new DummyNetParams();

        public static synchronized DummyNetParams get() {
            DummyNetParams dummyNetParams;
            synchronized (DummyNetParams.class) {
                dummyNetParams = instance;
            }
            return dummyNetParams;
        }

        public String getPaymentProtocolId() {
            return null;
        }
    }

    static {
        Context.getOrCreate(DummyNetParams.get());
    }

    public CoinType() {
        this.parentType = null;
        this.altSymbols = new String[0];
        this.hasDynamicFees = false;
        this.hasSelectableFees = false;
        this.feePolicy = FeePolicy.FEE_PER_KB;
        this.bip32HeaderPub = 76067358;
        this.bip32HeaderPriv = 76066276;
    }

    public Value getMinFeeValue() {
        if (this.minFeeValue != null) {
            return this.minFeeValue;
        }
        return this.feeValue;
    }

    public Value zeroCoin() {
        if (this.zeroCoin == null) {
            this.zeroCoin = Value.valueOf((ValueType) this, 0);
        }
        return this.zeroCoin;
    }

    public Value value(byte[] units) {
        return Value.valueOf(this, units);
    }

    public Value value(BigInteger units) {
        return Value.valueOf((ValueType) this, units);
    }

    public Value value(double value) {
        return Value.parse(this, value);
    }

    public boolean hasMaxMoney() {
        return false;
    }

    public Sha256Hash parseTxId(String str) {
        return new Sha256Hash(str.replace("0x", ""));
    }


    public boolean hasDynamicFees() {
        return this.hasDynamicFees;
    }

    public boolean hasSelectableFees() {
        return this.hasSelectableFees;
    }

    //TODO
    private static final long serialVersionUID = 1L;

    private static final String BIP_44_KEY_PATH = "44H/%dH/%dH";

    protected String name;
    protected String symbol;
    protected String uriScheme;
    protected Integer bip44Index;
    protected Integer unitExponent;
    protected String addressPrefix;
    protected Value feeValue;
    protected Value minNonDust;
    protected Value softDustLimit;
    protected SoftDustPolicy softDustPolicy;
    protected FeePolicy feePolicy = FeePolicy.FEE_PER_KB;
    protected byte[] signedMessageHeader;

    private transient MonetaryFormat friendlyFormat;
    private transient MonetaryFormat plainFormat;
    private transient Value oneCoin;

    private static FeeProvider feeProvider = null;

    @Override
    public String getName() {
        return checkNotNull(name, "A coin failed to set a name");
    }

    public boolean isTestnet() {
        return id.endsWith("test");
    }

    @Override
    public String getSymbol() {
        return checkNotNull(symbol, "A coin failed to set a symbol");
    }

    public String getUriScheme() {
        return checkNotNull(uriScheme, "A coin failed to set a URI scheme").toLowerCase();
    }

    public int getBip44Index() {
        return checkNotNull(bip44Index, "A coin failed to set a BIP 44 index");
    }

    @Override
    public int getUnitExponent() {
        return checkNotNull(unitExponent, "A coin failed to set a unit exponent");
    }

    public Value getFeeValue() {
        if (feeProvider != null) {
            return feeProvider.getFeeValue(this);
        } else {
            return getDefaultFeeValue();
        }
    }

    public Value getDefaultFeeValue() {
        return checkNotNull(feeValue, "A coin failed to set a fee value");
    }

    @Override
    public Value getMinNonDust() {
        return checkNotNull(minNonDust, "A coin failed to set a minimum amount to be considered not dust");
    }

    public Value getSoftDustLimit() {
        return checkNotNull(softDustLimit, "A coin failed to set a soft dust limit");
    }

    public SoftDustPolicy getSoftDustPolicy() {
        return checkNotNull(softDustPolicy, "A coin failed to set a soft dust policy");
    }

    public FeePolicy getFeePolicy() {
        return checkNotNull(feePolicy, "A coin failed to set a fee policy");
    }

    public byte[] getSignedMessageHeader() {
        return checkNotNull(signedMessageHeader, "A coin failed to set signed message header bytes");
    }

    public boolean canSignVerifyMessages() {
        return signedMessageHeader != null;
    }

    public boolean canHandleMessages() {
        return getMessagesFactory() != null;
    }

    @Nullable
    public MessageFactory getMessagesFactory() {
        return null;
    }

    protected static byte[] toBytes(String str) {
        return str.getBytes(Charsets.UTF_8);
    }

    public List<ChildNumber> getBip44Path(int account) {
        String path = String.format(BIP_44_KEY_PATH, bip44Index, account);
        return HDUtils.parsePath(path);
    }

    /**
        Return an address prefix like NXT- or BURST-, otherwise and empty string
     */
    public String getAddressPrefix() {
        return checkNotNull(addressPrefix, "A coin failed to set the address prefix");
    }

    public abstract AbstractAddress newAddress(String addressStr) throws AddressMalformedException;

    /**
     * Returns a 1 coin of this type with the correct amount of units (satoshis)
     * Use {@link com.coinomi.core.coins.CoinType:oneCoin}
     */
    @Deprecated
    public Coin getOneCoin() {
        BigInteger units = BigInteger.TEN.pow(getUnitExponent());
        return Coin.valueOf(units.longValue());
    }

    @Override
    public Value oneCoin() {
        if (oneCoin == null) {
            BigInteger units = BigInteger.TEN.pow(getUnitExponent());
            oneCoin = Value.valueOf(this, units);
        }
        return oneCoin;
    }


    @Override
    public Value value(String string) {
        return Value.parse(this, string);
    }

    @Override
    public Value value(Coin coin) {
        return Value.valueOf(this, coin);
    }

    @Override
    public Value value(long units) {
        return Value.valueOf(this, units);
    }

    @Override
    public String getPaymentProtocolId() {
        throw new RuntimeException("Method not implemented");
    }

    @Override
    public String toString() {
        return "Coin{" +
                "name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", bip44Index=" + bip44Index +
                '}';
    }

    @Override
    public MonetaryFormat getMoneyFormat() {
        if (friendlyFormat == null) {
            friendlyFormat = new MonetaryFormat()
                    .shift(0).minDecimals(2).code(0, symbol).postfixCode();
            /*switch (unitExponent) {
                case 8:
                    friendlyFormat = friendlyFormat.optionalDecimals(2, 2, 2);
                    break;
                case 6:
                    friendlyFormat = friendlyFormat.optionalDecimals(2, 2);
                    break;
                case 4:
                    friendlyFormat = friendlyFormat.optionalDecimals(2);
                    break;
                default:
                    friendlyFormat = friendlyFormat.minDecimals(unitExponent);
            }*/
            if (this.unitExponent.intValue() <= 2 || this.unitExponent.intValue() % 2 != 0) {
                this.friendlyFormat = this.friendlyFormat.minDecimals(this.unitExponent.intValue());
            } else {
                this.friendlyFormat = this.friendlyFormat.repeatOptionalDecimals(2, (this.unitExponent.intValue() / 2) - 1);
            }
        }
        return friendlyFormat;
    }

    @Override
    public MonetaryFormat getPlainFormat() {
        if (plainFormat == null) {
            plainFormat = new MonetaryFormat().shift(0)
                    .minDecimals(0).repeatOptionalDecimals(1, unitExponent).noCode();
        }
        return plainFormat;
    }

    @Override
    public boolean equals(ValueType obj) {
        return super.equals(obj);
    }

    public static void setFeeProvider(FeeProvider feeProvider) {
        CoinType.feeProvider = feeProvider;
    }

    public interface FeeProvider {
        Value getFeeValue(CoinType type);
    }

    public String getIcon() {
        return this.icon;
    }

    public static String generateSubTypeId(String subTypeHash, CoinType parent) {
        return subTypeHash + "." + parent.getId();
    }

    public boolean isSubType() {
        return this.parentType != null;
    }

    public CoinType getParentType() {
        return this.parentType;
    }

    public boolean isFavorite() {
        return false;
    }

    public int[] getP2SHHeaderExtras() {
        if (this.p2shHeaderExtras == null) {
            return EMPTY_HEADERS;
        }
        return this.p2shHeaderExtras;
    }
}
