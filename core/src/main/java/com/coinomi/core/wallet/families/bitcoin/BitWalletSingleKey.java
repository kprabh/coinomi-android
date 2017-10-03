package com.coinomi.core.wallet.families.bitcoin;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.exceptions.ResetKeyException;
import com.coinomi.core.wallet.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.RedeemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.List;

import javax.annotation.Nullable;

import static com.coinomi.core.Preconditions.checkNotNull;

/**
 * @author John L. Jegutanis
 */
public class BitWalletSingleKey extends BitWalletBase {
    private static final Logger log = LoggerFactory.getLogger(WalletPocketHD.class);
    private static final ImmutableList<ChildNumber> EMPTY_PATH = ImmutableList.of();
    @VisibleForTesting
    protected SimpleKeyChain keys;

    public BitWalletSingleKey(CoinType coinType, ECKey key) {
        super(checkNotNull(coinType), Wallet.generateRandomId());
        keys = new SimpleKeyChain();
        keys.importKey(key);
    }

    @Override
    public boolean isWatchedScript(Script script) {
        return false;
    }

    @Override
    public boolean isPayToScriptHashMine(byte[] payToScriptHash) {
        return false;
    }

    @Override
    public byte[] getPublicKey() {
        lock.lock();
        try {
            return keys.getKey(null).getPubKey();
        } finally {
            lock.unlock();
        }
    }
    public boolean isStandardPath() {
        return false;
    }
    public int getAccountIndex() {
        return 0;
    }
    public ImmutableList<ChildNumber> getDeterministicRootKeyPath() {
        return EMPTY_PATH;
    }
    public BitAddress getAddress() {
        lock.lock();
        try {
            return BitAddress.from(type, keys.getKey(null));
        } finally {
            lock.unlock();
        }
    }
    public boolean hasPrivKey() {
        for (ECKey k : this.keys.getKeys()) {
            if (k.hasPrivKey()) {
                return true;
            }
        }
        return false;
    }

    public DeterministicKey getDeterministicRootKey() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(com.coinomi.core.wallet.families.bitcoin.BitWalletSingleKey.class + " does not support deterministic keys");
    }

    public void resetRootKey(DeterministicKey key) throws UnsupportedOperationException, ResetKeyException {
        throw new UnsupportedOperationException(com.coinomi.core.wallet.families.bitcoin.BitWalletSingleKey.class + " does not support deterministic keys");
    }
    @Override
    public AbstractAddress getChangeAddress() {
        return getReceiveAddress();
    }

    @Override
    public AbstractAddress getReceiveAddress() {
        lock.lock();
        try {
            return BitAddress.from(type, keys.getKey(null));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public AbstractAddress getRefundAddress(boolean isManualAddressManagement) {
        return getReceiveAddress();
    }

    @Override
    public AbstractAddress getReceiveAddress(boolean isManualAddressManagement) {
        return getReceiveAddress();
    }

    @Override
    public boolean hasUsedAddresses() {
        return false;
    }

    @Override
    public boolean canCreateNewAddresses() {
        return false;
    }

    @Override
    public List<AbstractAddress> getActiveAddresses() {
        lock.lock();
        try {
            ImmutableList.Builder<AbstractAddress> activeAddresses = ImmutableList.builder();
            for (ECKey key : keys.getKeys()) {
                activeAddresses.add(BitAddress.from(type, key));
            }
            return activeAddresses.build();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void markAddressAsUsed(AbstractAddress address) { }

    @Override
    public boolean isEncryptable() {
        return false;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public KeyCrypter getKeyCrypter() {
        return null;
    }

    @Override
    public void encrypt(KeyCrypter keyCrypter, KeyParameter aesKey) {

    }

    @Override
    public void decrypt(KeyParameter aesKey) {

    }

    @Override
    public void maybeInitializeAllKeys() {

    }

    @Override
    public String getPublicKeyMnemonic() {
        return null;
    }

    @Override
    public String getPublicKeySerialized() {
        return null;
    }

    @Nullable
    @Override
    public ECKey findKeyFromPubHash(byte[] pubkeyHash) {
        lock.lock();
        try {
            return keys.findKeyFromPubHash(pubkeyHash);
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    @Override
    public ECKey findKeyFromPubKey(byte[] pubkey) {
        lock.lock();
        try {
            return keys.findKeyFromPubKey(pubkey);
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    @Override
    public RedeemData findRedeemDataFromScriptHash(byte[] scriptHash) {
        return null;
    }
}
