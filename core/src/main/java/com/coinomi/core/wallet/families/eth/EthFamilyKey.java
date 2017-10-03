package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.coins.eth.crypto.cryptohash.Keccak256;
import com.coinomi.core.exceptions.ResetKeyException;
import com.coinomi.core.protos.Protos.Key;
import com.coinomi.core.protos.Protos.Key.Builder;
import com.coinomi.core.util.KeyUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import org.bitcoinj.core.BloomFilter;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.EncryptableKeyChain;
import org.bitcoinj.wallet.KeyBag;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.RedeemData;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;
import org.spongycastle.crypto.params.KeyParameter;

public class EthFamilyKey implements Serializable, EncryptableKeyChain, KeyBag {
    public static final ChildNumber ACCOUNT_CHILD_NUM = ChildNumber.ZERO;
    private DeterministicKey accountKey;
    private final ReentrantLock lock = Threading.lock("EthKeyChain");
    private DeterministicKey rootKey;

    public EthFamilyKey(DeterministicKey rootKey, KeyCrypter keyCrypter, KeyParameter key) {
        if (keyCrypter == null || rootKey.isEncrypted()) {
            this.rootKey = rootKey;
        } else {
            this.rootKey = rootKey.encrypt(keyCrypter, key, null);
        }
        this.accountKey = HDKeyDerivation.deriveChildKey(rootKey, ACCOUNT_CHILD_NUM).dropPrivateBytes();
    }

    private EthFamilyKey(DeterministicKey rootKey, DeterministicKey accountKey) {
        this.rootKey = rootKey;
        this.accountKey = accountKey.dropPrivateBytes();
    }

    public boolean isEncrypted() {
        return this.rootKey.isEncrypted();
    }

    public byte[] getPublicKey() {
        return this.rootKey.getPubKey();
    }

    public byte[] getAccountPrivateKey() {
        return this.accountKey.getPrivKeyBytes();
    }

    public boolean hasPrivKey() {
        return this.rootKey.hasPrivKey();
    }

    public EncryptableKeyChain toEncrypted(CharSequence password) {
        return null;
    }

    public EthFamilyKey toEncrypted(KeyCrypter keyCrypter, KeyParameter aesKey) {
        Preconditions.checkState(!this.rootKey.isEncrypted(), "Attempt to encrypt a key that is already encrypted.");
        DeterministicKey encryptedRootKey = this.rootKey.encrypt(keyCrypter, aesKey, null);
        return new EthFamilyKey(encryptedRootKey, new DeterministicKey(this.accountKey.dropPrivateBytes(), encryptedRootKey));
    }

    public EncryptableKeyChain toDecrypted(CharSequence password) {
        return null;
    }

    public EthFamilyKey toDecrypted(KeyParameter aesKey) {
        Preconditions.checkState(getKeyCrypter() != null, "Key chain not encrypted");
        Preconditions.checkState(this.rootKey.isEncrypted(), "Root key not encrypted");
        DeterministicKey rootKeyDecrypted = this.rootKey.decrypt(getKeyCrypter(), aesKey);
        EthFamilyKey decryptedKey = new EthFamilyKey(rootKeyDecrypted, new DeterministicKey(this.accountKey.dropPrivateBytes(), rootKeyDecrypted));
        if (decryptedKey.rootKey.getPubKeyPoint().equals(this.rootKey.getPubKeyPoint())) {
            return decryptedKey;
        }
        throw new KeyCrypterException("Provided AES key is wrong");
    }

    public boolean checkPassword(CharSequence password) {
        return false;
    }

    public boolean checkAESKey(KeyParameter aesKey) {
        return false;
    }

    public KeyCrypter getKeyCrypter() {
        return this.rootKey.getKeyCrypter();
    }

    public ECKey findKeyFromPubHash(byte[] bytes) {
        throw new RuntimeException("Not implemented");
    }

    public ECKey findKeyFromPubKey(byte[] bytes) {
        throw new RuntimeException("Not implemented");
    }

    public RedeemData findRedeemDataFromScriptHash(byte[] bytes) {
        throw new RuntimeException("Not implemented");
    }

    public byte[] getAddress() {
        byte[] pubBytes = this.accountKey.getPubKeyPoint().getEncoded(false);
        return sha3omit12(Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
    }

    public static byte[] sha3omit12(byte[] input) {
        byte[] hash = sha3(input);
        return Arrays.copyOfRange(hash, 12, hash.length);
    }

    public static byte[] sha3(byte[] input) {
        Keccak256 digest = new Keccak256();
        digest.update(input);
        return digest.digest();
    }

    List<Key> toProtobuf() {
        LinkedList<Key> entries = Lists.newLinkedList();
        for (Builder proto : toEditableProtobuf()) {
            entries.add(proto.build());
        }
        return entries;
    }

    List<Builder> toEditableProtobuf() {
        this.lock.lock();
        try {
            LinkedList<Builder> entries = Lists.newLinkedList();
            entries.add(KeyUtils.serializeKey(this.rootKey));
            entries.add(KeyUtils.serializeKey(this.accountKey));
            return entries;
        } finally {
            this.lock.unlock();
        }
    }

    public static EthFamilyKey fromProtobuf(List<Key> keys) throws UnreadableWalletException {
        return fromProtobuf(keys, null);
    }

    public static EthFamilyKey fromProtobuf(List<Key> keys, KeyCrypter crypter) throws UnreadableWalletException {
        DeterministicKey rootKey = KeyUtils.getDeterministicKey((Key) keys.get(0), null, crypter);
        return new EthFamilyKey(rootKey, KeyUtils.getDeterministicKey((Key) keys.get(1), rootKey, crypter));
    }

    public int getAccountIndex() {
        return this.rootKey.getChildNumber().num();
    }

    public DeterministicKey getRootKey() {
        return this.rootKey;
    }

    public void resetRootKey(DeterministicKey key) throws ResetKeyException {
        this.lock.lock();
        try {
            if (Arrays.equals(this.rootKey.getPubKey(), key.getPubKey())) {
                this.rootKey = key;
                this.accountKey = HDKeyDerivation.deriveChildKey(this.rootKey, ACCOUNT_CHILD_NUM).dropPrivateBytes();
                return;
            }
            throw new ResetKeyException("Provided key does not match the current root key");
        } finally {
            this.lock.unlock();
        }
    }

    public boolean hasKey(ECKey key) {
        return false;
    }

    public List<? extends ECKey> getKeys(KeyPurpose purpose, int numberOfKeys) {
        return null;
    }

    public ECKey getKey(KeyPurpose purpose) {
        return null;
    }

    public List<Protos.Key> serializeToProtobuf() {
        return null;
    }

    public void addEventListener(KeyChainEventListener listener) {

    }

    public void addEventListener(KeyChainEventListener listener, Executor executor) {

    }

    public boolean removeEventListener(KeyChainEventListener listener) {
        return false;
    }

    public int numKeys() {
        return 0;
    }

    public int numBloomFilterEntries() {
        return 0;
    }

    public long getEarliestKeyCreationTime() {
        return 0;
    }

    public BloomFilter getFilter(int size, double falsePositiveRate, long tweak) {
        return null;
    }
}
