package com.coinomi.core.wallet;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.ValueType;
import com.coinomi.core.exceptions.UnsupportedCoinTypeException;
import com.coinomi.core.util.TypeUtils;
import com.coinomi.core.wallet.WalletAccount.WalletAccountException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.utils.Threading;

import javax.annotation.Nullable;

/**
 * @author John L. Jegutanis
 */
public abstract class AbstractWallet<T extends AbstractTransaction, A extends AbstractAddress>
        implements WalletAccount<T, A> {
    protected final String id;
    private static final List<CoinType> EMPTY_LIST = new ArrayList(0);
    protected String description;
    protected final CoinType type;
    protected final ReentrantLock lock = Threading.lock("AbstractWallet");
    private Runnable saveLaterRunnable = new Runnable() {
        public void run() {
            Wallet wallet = AbstractWallet.this.getWallet();
            if (wallet != null) {
                wallet.saveLater();
            }
        }
    };
    private Runnable saveNowRunnable = new Runnable() {
        public void run() {
            Wallet wallet = AbstractWallet.this.getWallet();
            if (wallet != null) {
                wallet.saveNow();
            }
        }
    };
    protected WeakReference<Wallet> walletRef = new WeakReference(null);

    public AbstractWallet(CoinType coinType, String id) {
        this.type = coinType;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public CoinType getCoinType() {
        return type;
    }

    public CoinType getCoinType(String coinId) throws UnsupportedCoinTypeException {
        checkCoinId(coinId);
        return this.type;
    }

    private void checkCoinId(String coinId) {
        if (!this.type.getId().equals(coinId)) {
            throw new UnsupportedCoinTypeException("Wallet with type " + this.type.getId() + " does not support type" + coinId);
        }
    }

    public Value getBalance(CoinType type) throws UnsupportedCoinTypeException {
        checkCoinId(type.getId());
        return getBalance();
    }

    @Override
    public List<AbstractTransaction> getTransactionList(CoinType type) throws UnsupportedCoinTypeException {
        checkCoinId(type.getId());
        return (List<AbstractTransaction>)getTransactionList();
    }

    public void setWallet(Wallet wallet) {
        this.lock.lock();
        if (wallet != null) {
            try {
                this.walletRef.clear();
                this.walletRef = new WeakReference(wallet);
            } catch (Throwable th) {
                this.lock.unlock();
            }
        } else {
            this.walletRef.clear();
        }
        this.lock.unlock();
    }

    public Wallet getWallet() {
        this.lock.lock();
        try {
            Wallet wallet = (Wallet) this.walletRef.get();
            return wallet;
        } finally {
            this.lock.unlock();
        }
    }

    public void walletSaveLater() {
        Threading.USER_THREAD.execute(this.saveLaterRunnable);
    }

    public void walletSaveNow() {
        Threading.USER_THREAD.execute(this.saveNowRunnable);
    }
    /**
     * Set the description of the wallet.
     * This is a Unicode encoding string typically entered by the user as descriptive text for the wallet.
     */
    @Override
    public void setDescription(String description) {
        lock.lock();
        this.description = description;
        lock.unlock();
        walletSaveNow();
    }

    /**
     * Get the description of the wallet. See
     */
    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Get the description or the coin type of the wallet.
     */
    @Override
    public String getDescriptionOrCoinName() {
        if (description == null || description.trim().isEmpty()) {
            return type.getName();
        } else {
            return description;
        }
    }
    public String getDefaultAccountName() {
        String extraPart = isStandardPath() ? getAccountIndex() > 0 ? " #" + (getAccountIndex() + 1) : "" : " " + HDUtils.formatPath(getDeterministicRootKeyPath());
        return this.type.getName() + extraPart;
    }

    public boolean isStandardPath() {
        return getDeterministicRootKeyPath().equals(this.type.getBip44Path(getAccountIndex()));
    }
    @Override
    public void completeAndSignTx(SendRequest request) throws WalletAccountException {
        if (request.isCompleted()) {
            signTransaction(request);
        } else {
            completeTransaction(request);
        }
    }

    @Override
    public boolean isType(WalletAccount other) {
        return TypeUtils.is(type, other);
    }

    @Override
    public boolean isType(ValueType otherType) {
        return TypeUtils.is(type, otherType);
    }

    @Override
    public boolean isType(AbstractAddress address) {
        return TypeUtils.is(type, address);
    }

    public WalletConnectivityStatus getConnectivityStatus() {
        if (!isConnected()) {
            return WalletConnectivityStatus.DISCONNECTED;
        } else {
            if (isLoading()) {
                return WalletConnectivityStatus.LOADING;
            } else {
                return WalletConnectivityStatus.CONNECTED;
            }
        }
    }

    @Override
    public boolean equals(WalletAccount other) {
        return other != null &&
                getId().equals(other.getId()) &&
                getCoinType().equals(other.getCoinType());
    }

    public List<CoinType> availableSubTypes() {
        return EMPTY_LIST;
    }

    public List<CoinType> favoriteSubTypes() {
        return EMPTY_LIST;
    }
}
