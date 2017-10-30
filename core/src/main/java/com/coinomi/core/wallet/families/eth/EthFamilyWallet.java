package com.coinomi.core.wallet.families.eth;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.eth.CallTransaction;
import com.coinomi.core.coins.eth.Transaction;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.exceptions.ExecutionException;
import com.coinomi.core.exceptions.ResetKeyException;
import com.coinomi.core.exceptions.TransactionBroadcastException;
import com.coinomi.core.exceptions.UnsupportedCoinTypeException;
import com.coinomi.core.network.AddressStatus;
import com.coinomi.core.network.BlockHeader;
import com.coinomi.core.network.EthServerClient;
import com.coinomi.core.network.ServerClient.HistoryTx;
import com.coinomi.core.network.interfaces.BlockchainConnection;
import com.coinomi.core.network.interfaces.TransactionEventListener;
import com.coinomi.core.protos.Protos.Key;
import com.coinomi.core.util.KeyUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.AccountContractEventListener;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.SignedMessage;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount.WalletAccountException;
import com.coinomi.core.wallet.WalletAccountEventListener;
import com.coinomi.core.wallet.WalletConnectivityStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.utils.ListenerRegistration;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.RedeemData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

public class EthFamilyWallet extends AbstractWallet<EthTransaction, EthAddress> implements TransactionEventListener<EthTransaction>, AccountContractEventListener {
    private static final Logger log = LoggerFactory.getLogger(EthFamilyWallet.class);
    EthAddress address;
    final HashMap<AbstractAddress, String> addressesStatus;
    Value balance;
    private EthServerClient blockchainConnection;
    private ConcurrentHashMap<String, List<ListenerRegistration<AccountContractEventListener>>> contractListeners;
    private DB db;
    File dbAccountFolder;
    HTreeMap<String, String> dbBalance;
    HTreeMap<String, String> dbContracts;
    Set<String> dbFavorites;
    File dbFile;
    HTreeMap<String, byte[]> dbHeight;
    HTreeMap<String, String> dbTransactions;
    protected final ConcurrentHashMap<String, ERC20Token> erc20Tokens;
    protected final ConcurrentHashMap<String, EthContract> ethContracts;
    protected final ConcurrentHashMap<String, ERC20Token> favoritesERC20Tokens;
    EthFamilyKey keys;
    private Integer lastBlockSeenHeight;
    private Long lastBlockSeenTimeSecs;
    private List<ListenerRegistration<WalletAccountEventListener>> listeners;
    BigInteger nonce;
    protected final ConcurrentHashMap<Sha256Hash, EthTransaction> pendingTransactions;
    protected final ConcurrentHashMap<Sha256Hash, EthTransaction> rawTransactions;
    private boolean subscribed;

    class C03558 extends ArrayList<CoinType> {
        C03558() {
            addAll(EthFamilyWallet.this.erc20Tokens.values());
        }
    }

    class C03569 extends ArrayList<CoinType> {
        C03569() {
            addAll(EthFamilyWallet.this.favoritesERC20Tokens.values());
        }
    }

    public int getLastBlockSeenHeight() {
        return this.lastBlockSeenHeight.intValue();
    }

    public void setLastBlockSeenHeight(int lastBlockSeenHeight) {
        this.lastBlockSeenHeight = Integer.valueOf(lastBlockSeenHeight);
    }

    public void setLastBlockSeenTimeSecs(long lastBlockSeenTimeSecs) {
        this.lastBlockSeenTimeSecs = Long.valueOf(lastBlockSeenTimeSecs);
    }

    public EthFamilyWallet(DeterministicKey rootKey, CoinType type, KeyCrypter keyCrypter, KeyParameter key) {
        super(type, KeyUtils.getPublicKeyId(type, rootKey.getPubKey()));
        this.subscribed = false;
        this.lastBlockSeenHeight = Integer.valueOf(-1);
        this.lastBlockSeenTimeSecs = Long.valueOf(0);
        this.keys = new EthFamilyKey(rootKey, keyCrypter, key);
        this.balance = type.value(0);
        this.rawTransactions = new ConcurrentHashMap();
        this.pendingTransactions = new ConcurrentHashMap();
        this.addressesStatus = new HashMap();
        this.listeners = new CopyOnWriteArrayList();
        this.contractListeners = new ConcurrentHashMap();
        try {
            this.address = new EthAddress(type, Hex.toHexString(this.keys.getAddress()));
            this.ethContracts = new ConcurrentHashMap();
            this.erc20Tokens = new ConcurrentHashMap();
            this.favoritesERC20Tokens = new ConcurrentHashMap();
        } catch (AddressMalformedException e) {
            throw new RuntimeException(e);
        }
    }

    public EthFamilyWallet(String id, EthFamilyKey keys, CoinType coinType) {
        super((CoinType) Preconditions.checkNotNull(coinType), id);
        this.subscribed = false;
        this.lastBlockSeenHeight = Integer.valueOf(-1);
        this.lastBlockSeenTimeSecs = Long.valueOf(0);
        this.balance = this.type.value(0);
        this.keys = (EthFamilyKey) Preconditions.checkNotNull(keys);
        this.rawTransactions = new ConcurrentHashMap();
        this.pendingTransactions = new ConcurrentHashMap();
        this.addressesStatus = new HashMap();
        this.listeners = new CopyOnWriteArrayList();
        this.contractListeners = new ConcurrentHashMap();
        try {
            this.address = new EthAddress(this.type, Hex.toHexString(keys.getAddress()));
            this.ethContracts = new ConcurrentHashMap();
            this.erc20Tokens = new ConcurrentHashMap();
            this.favoritesERC20Tokens = new ConcurrentHashMap();
        } catch (AddressMalformedException e) {
            throw new RuntimeException(e);
        }
    }

    public EthFamilyWallet(EthFamilyKey keys, CoinType type) {
        this(KeyUtils.getPublicKeyId(type, keys.getPublicKey()), keys, type);
    }

    public CoinType getCoinType(String coinId) throws UnsupportedCoinTypeException {
        CoinType otherType = CoinID.typeFromId(coinId);
        if ((otherType instanceof ERC20Token) && this.erc20Tokens.containsKey(((ERC20Token) otherType).getAddress())) {
            return (CoinType) this.erc20Tokens.get(((ERC20Token) otherType).getAddress());
        }
        throw new UnsupportedCoinTypeException("Wallet with type " + this.type.getId() + " does not support type" + otherType.getId());
    }

    public byte[] getPublicKey() {
        return this.keys.getPublicKey();
    }

    public String getDefaultAccountName() {
        return null;
    }

    public boolean hasPrivKey() {
        return this.keys.hasPrivKey();
    }

    public DeterministicKey getDeterministicRootKey() throws UnsupportedOperationException {
        return this.keys.getRootKey();
    }

    public void resetRootKey(DeterministicKey key) throws UnsupportedOperationException, ResetKeyException {
        this.lock.lock();
        try {
            this.keys.resetRootKey(key);
        } finally {
            this.lock.unlock();
        }
    }

    public boolean isNew() {
        return this.rawTransactions.size() == 0;
    }

    public Value getBalance() {
        return this.balance;
    }

    public Value getBalance(CoinType otherType) throws UnsupportedCoinTypeException {
        if (this.type.equals(otherType)) {
            return getBalance();
        }
        if (otherType instanceof ERC20Token) {
            try {
                return ((ERC20Token) otherType).getBalance(this);
            } catch (Exception e) {
                throw new UnsupportedCoinTypeException(e);
            }
        }
        throw new UnsupportedCoinTypeException("Wallet with id " + getId() + " does not support type" + otherType.getId());
    }

    public boolean isStandardPath() {
        return false;
    }

    public void refresh() {
        this.lock.lock();
        try {
            log.info("Refreshing wallet pocket {}", this.type);
            this.lastBlockSeenHeight = Integer.valueOf(0);
            this.lastBlockSeenTimeSecs = Long.valueOf(0);
            this.rawTransactions.clear();
            this.addressesStatus.clear();
        } finally {
            this.lock.unlock();
        }
    }

    public boolean isConnected() {
        return this.blockchainConnection != null && this.blockchainConnection.isActivelyConnected();
    }

    public boolean isLoading() {
        return false;
    }

    public void disconnect() {
        if (this.blockchainConnection != null) {
            this.blockchainConnection.deleteNetworkClient();
        }
    }

    public EthAddress getAddress() {
        return this.address;
    }

    public AbstractAddress getChangeAddress() {
        return getAddress();
    }

    public AbstractAddress getReceiveAddress() {
        return getAddress();
    }

    public AbstractAddress getRefundAddress(boolean isManualAddressManagement) {
        return getAddress();
    }

    public AbstractAddress getReceiveAddress(boolean isManualAddressManagement) {
        return getAddress();
    }

    public boolean hasUsedAddresses() {
        return false;
    }

    public boolean broadcastTxSync(AbstractTransaction tx) throws TransactionBroadcastException {
        if (tx instanceof EthTransaction) {
            return broadcastEthTxSync((EthTransaction) tx);
        }
        throw new TransactionBroadcastException("Unsupported transaction class: " + tx.getClass().getName() + ", need: " + EthTransaction.class.getName());
    }

    public void broadcastTx(AbstractTransaction tx) throws TransactionBroadcastException {

    }

    public boolean broadcastEthTxSync(EthTransaction tx) throws TransactionBroadcastException {
        if (isConnected()) {
            if (log.isInfoEnabled()) {
                log.info("Broadcasting tx {}", Utils.HEX.encode(tx.getRawTransaction()));
            }
            boolean success = this.blockchainConnection.broadcastTxSync(tx);
            if (success) {
                onTransactionBroadcast(tx);
            } else {
                onTransactionBroadcastError(tx);
            }
            return success;
        }
        throw new TransactionBroadcastException("No connection available");
    }

    public boolean canCreateNewAddresses() {
        return false;
    }

    public EthTransaction getTransaction(String transactionId) {
        return (EthTransaction) this.rawTransactions.get(new Sha256Hash(transactionId));
    }

    public Map<Sha256Hash, EthTransaction> getPendingTransactions() {
        return null;
    }

    public Map<Sha256Hash, EthTransaction> getTransactions() {
        this.lock.lock();
        try {
            Map<Sha256Hash, EthTransaction> copyOf = ImmutableMap.copyOf(this.rawTransactions);
            return copyOf;
        } finally {
            this.lock.unlock();
        }
    }

    public List<EthTransaction> getTransactionList() {
        this.lock.lock();
        try {
            List<EthTransaction> copyOf = ImmutableList.copyOf(this.rawTransactions.values());
            return copyOf;
        } finally {
            this.lock.unlock();
        }
    }

    public List<AbstractTransaction> getTransactionList(CoinType otherType) throws UnsupportedCoinTypeException {
        this.lock.lock();
        List<AbstractTransaction> build;
        try {
            Builder<AbstractTransaction> builder;
            if (this.type.equals(otherType)) {
                builder = ImmutableList.builder();
                for (AbstractTransaction tx : this.rawTransactions.values()) {
                    builder.add(tx);
                }
                build = builder.build();
                return build;
            } else if (otherType instanceof ERC20Token) {
                this.lock.lock();
                EthContract contract = (EthContract) this.ethContracts.get(((ERC20Token) otherType).getAddress());
                if (contract == null) {
                    throw new UnsupportedCoinTypeException("No contract available for type " + otherType.getId() + " in wallet id " + getId());
                }
                builder = ImmutableList.builder();
                for (EthTransaction tx2 : this.rawTransactions.values()) {
                    if (contract.isMineTx(tx2)) {
                        builder.add(new ERC20Transaction(tx2, otherType, this));
                    }
                }
                build = builder.build();
                this.lock.unlock();
                return build;
            } else {
                throw new UnsupportedCoinTypeException("Wallet with id " + getId() + " does not support type" + otherType.getId());
            }
        } catch (Throwable th) {

            return null;
        } finally {
            this.lock.unlock();
        }
    }



    public List<AbstractAddress> getActiveAddresses() {
        return ImmutableList.of((AbstractAddress)this.address);
    }

    public void markAddressAsUsed(AbstractAddress address) {
    }

    public void setWallet(Wallet wallet)  {
        super.setWallet(wallet);
        if (wallet != null) {
            try {
                initDb(wallet.getDbFolder());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (this.db != null) {
            try {
                this.db.commit();
                deleteDB();
            } catch (Throwable e) {
                log.info("Error deleting ethereum wallet ", e);
            }
        }
    }

    public Wallet getWallet() {
        return null;
    }

    public synchronized DB getDb() {
        if (this.db == null || this.db.isClosed()) {
            this.db = DBMaker.newFileDB((File) Preconditions.checkNotNull(this.dbFile)).mmapFileEnableIfSupported().closeOnJvmShutdown().make();
        }
        return this.db;
    }

    public synchronized HTreeMap<String, String> getDbTransactions() {
        if (this.dbTransactions == null) {
            this.dbTransactions = getDb().createHashMap("raw_transactions").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).makeOrGet();
        }
        return this.dbTransactions;
    }

    public synchronized HTreeMap<String, String> getDbBalance() {
        if (this.dbBalance == null) {
            this.dbBalance = getDb().createHashMap("balance_map").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).makeOrGet();
        }
        return this.dbBalance;
    }

    public synchronized HTreeMap<String, byte[]> getDbHeight() {
        if (this.dbHeight == null) {
            this.dbHeight = getDb().createHashMap("height_map").keySerializer(Serializer.STRING).valueSerializer(Serializer.BYTE_ARRAY).makeOrGet();
        }
        return this.dbHeight;
    }

    public synchronized HTreeMap<String, String> getDbContracts() {
        if (this.dbContracts == null) {
            this.dbContracts = getDb().createHashMap("contracts_map").keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).makeOrGet();
        }
        return this.dbContracts;
    }

    public synchronized Set<String> getDbFavorites() {
        if (this.dbFavorites == null) {
            this.dbFavorites = getDb().createHashSet("favorite_contracts").serializer(Serializer.STRING).makeOrGet();
        }
        return this.dbFavorites;
    }

    public void initDb(File dbPath) throws Exception {
        initDb(dbPath, 3);
    }

    private void initDb(File dbPath, int tries) throws Exception {
        log.info("Initializing " + EthFamilyWallet.class + " database for " + this.id.substring(0, 8));
        this.dbAccountFolder = new File(dbPath.getPath(), this.type.getId());
        if (!this.dbAccountFolder.exists()) {
            this.dbAccountFolder.mkdir();
        }
        this.dbFile = new File((File) Preconditions.checkNotNull(this.dbAccountFolder), this.id);
        this.lastBlockSeenHeight = Integer.valueOf(0);
        this.lastBlockSeenTimeSecs = Long.valueOf(0);
        if (getDbHeight().containsKey(this.address.toString())) {
            ByteBuffer b = ByteBuffer.wrap((byte[]) getDbHeight().get(this.address.toString()));
            b.order(ByteOrder.LITTLE_ENDIAN);
            this.lastBlockSeenHeight = Integer.valueOf(b.getInt());
            this.lastBlockSeenTimeSecs = Long.valueOf(b.getLong());
            b.clear();
        }
        List<String> temp = new ArrayList();
        this.ethContracts.clear();
        if (getDbContracts().size() > 0) {
            for (String s : getDbContracts().values()) {
                if (temp.contains(s)) {
                    throw new Exception("Could not init database - transactions corrupted");
                }
                temp.add(s);
                try {
                    EthContract contract = EthContract.fromJSON(this.type, new JSONObject(s));
                    this.ethContracts.put(contract.getContractAddress(), contract);
                    if (contract.isContractType("erc20")) {
                        ERC20Token token = ERC20Token.getERCToken(contract);
                        if (token != null) {
                            /*try
                            {*/
                            erc20Tokens.put(contract.getContractAddress(), token);
                            /*}
                            catch(JSONException ex){
                                log.info("error parsing contracts from db");
                            }*/
                        } else {
                            log.warn("Could not add ERC20 token for local db contract " + contract.getContractAddress());
                        }
                    }
                } catch (JSONException e) {
                    log.info("error parsing contracts from db");
                } catch (Throwable e2) {
                    deleteDB();
                    if (tries > 0) {
                        log.warn("Could not init database, retrying...", e2);
                        initDb(dbPath, tries - 1);
                        return;
                    }
                    RuntimeException runtimeException = new RuntimeException("Could not init database", e2);
                }
            }
        }

        JSONArray contractJsons = ERC20DefaultTokens.getERC20DefaultTokens(this.type);

        for(int i = 0; i< contractJsons.length(); i++) {
            JSONObject contractJson = contractJsons.getJSONObject(i);
            if (contractJson.has("address")) {
                if (!ethContracts.containsKey(contractJson.getString("address").toLowerCase())) {
                    EthContract contractObj = EthContract.fromJSON(this.type, contractJson);
                    ethContracts.put(contractObj.getContractAddress(), contractObj);
                    if (contractObj.isContractType("erc20")) {
                        ERC20Token r6 = ERC20Token.getERCToken(contractObj);
                        if (r6 != null) {
                            erc20Tokens.put(contractObj.getContractAddress(), r6);
                        } else {
                            log.warn("Could not add ERC20 token for default contract " + contractObj.getContractAddress());
                        }
                    }
                }

            }
        }

        temp.clear();
        this.rawTransactions.clear();
        this.nonce = null;
        if (getDbTransactions().size() > 0) {
            for (String s2 : getDbTransactions().values()) {
                if (temp.contains(s2)) {
                    throw new Exception("Could not init database - transactions corrupted");
                }
                temp.add(s2);
                try {
                    EthTransaction tx = EthTransaction.fromJSON(this.type, s2);
                    tx.setDepthInBlocks(this.lastBlockSeenHeight.intValue());
                    this.rawTransactions.put(tx.getHash(), tx);
                    if (((AbstractAddress) tx.getReceivedFrom().get(0)).toString().equals(this.address.toString()) && (this.nonce == null || tx.getNonce().compareTo(this.nonce) > 0)) {
                        this.nonce = tx.getNonce();
                    }
                } catch (Throwable e22) {
                    e22.printStackTrace();
                }
            }
        }
        if (getDbBalance().containsKey(this.address.toString())) {
            this.balance = Value.valueOf(this.type, new BigInteger((String) getDbBalance().get(this.address.toString()), 16));
        }
    }

    private void deleteDB() {
        closeDb();
        if (this.dbFile.exists()) {
            deleteRecursive(this.dbFile);
            log.debug("Deleted corrupted db file");
        }
    }

    private void closeDb() {
        if (!(this.db == null || this.db.isClosed())) {
            this.db.close();
        }
        this.db = null;
        this.dbTransactions = null;
        this.dbBalance = null;
        this.dbHeight = null;
        this.dbContracts = null;
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] dirFiles = fileOrDirectory.listFiles();
            if (dirFiles != null) {
                for (File child : dirFiles) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    public void walletSaveLater() {
        super.walletSaveLater();
    }

    public void walletSaveNow() {
        super.walletSaveNow();
    }

    List<Key> serializeKeychainToProtobuf() {
        this.lock.lock();
        try {
            List<Key> toProtobuf = this.keys.toProtobuf();
            return toProtobuf;
        } finally {
            this.lock.unlock();
        }
    }

    public boolean isEncryptable() {
        return true;
    }

    public boolean isEncrypted() {
        this.lock.lock();
        try {
            boolean isEncrypted = this.keys.isEncrypted();
            return isEncrypted;
        } finally {
            this.lock.unlock();
        }
    }

    public KeyCrypter getKeyCrypter() {
        this.lock.lock();
        try {
            KeyCrypter keyCrypter = this.keys.getKeyCrypter();
            return keyCrypter;
        } finally {
            this.lock.unlock();
        }
    }

    public void encrypt(KeyCrypter keyCrypter, KeyParameter aesKey) {
        Preconditions.checkNotNull(keyCrypter);
        Preconditions.checkNotNull(aesKey);
        this.lock.lock();
        try {
            this.keys = this.keys.toEncrypted(keyCrypter, aesKey);
        } finally {
            this.lock.unlock();
        }
    }

    public void decrypt(KeyParameter aesKey) {

    }

    public void addEventListener(WalletAccountEventListener listener) {
        addEventListener(listener, Threading.USER_THREAD);
    }

    public void addEventListener(WalletAccountEventListener listener, Executor executor) {
        this.listeners.add(new ListenerRegistration(listener, executor));
    }

    public boolean removeEventListener(WalletAccountEventListener listener) {
        return ListenerRegistration.removeFromList(listener, this.listeners);
    }

    public boolean isAddressMine(AbstractAddress address) {
        return this.address.equals(address);
    }

    public void maybeInitializeAllKeys() {
    }

    public String getPublicKeyMnemonic() {
        return null;
    }

    public SendRequest getEmptyWalletRequest(AbstractAddress destination, byte[] contractData) throws WalletAccountException {
        checkAddress(destination);
        return EthSendRequest.emptyWallet(this, (EthAddress) destination, contractData);
    }

    public SendRequest getSendToRequest(AbstractAddress destination, Value amount, byte[] contractData) throws WalletAccountException {
        checkAddress(destination);
        return EthSendRequest.to(this, (EthAddress) destination, amount, contractData);
    }

    private void checkAddress(AbstractAddress destination) throws WalletAccountException {
        if (!(destination instanceof EthAddress)) {
            throw new WalletAccountException("Incompatible address" + destination.getClass().getName() + ", expected " + EthAddress.class.getName());
        }
    }

    public void completeTransaction(SendRequest request) throws WalletAccountException {
        if (this.blockchainConnection == null) {
            throw new WalletAccountException("No connection available");
        }
        this.blockchainConnection.estimateGasSync(checkSendRequest(request));
        request.setCompleted(true);
        if (request.signTransaction) {
            signTransaction(request);
        }
    }

    public void signTransaction(SendRequest request) throws WalletAccountException {
        byte[] privKey;
        com.google.common.base.Preconditions.checkArgument(request.isCompleted(), "Send request is not completed");
        EthTransaction tx = (EthTransaction) Preconditions.checkNotNull(checkSendRequest(request).getTx());
        if (this.keys.isEncrypted()) {
            boolean z;
            if (request.aesKey != null) {
                z = true;
            } else {
                z = false;
            }
            com.google.common.base.Preconditions.checkArgument(z, "Wallet is encrypted but no decryption key provided");
            privKey = this.keys.toDecrypted(request.aesKey).getAccountPrivateKey();
        } else {
            privKey = this.keys.getAccountPrivateKey();
        }
        tx.sign(privKey);
        log.info("ethereum tx signed");
        Arrays.fill(privKey, (byte) 0);
    }

    private EthSendRequest checkSendRequest(SendRequest request) throws WalletAccountException {
        if (request instanceof EthSendRequest) {
            return (EthSendRequest) request;
        }
        throw new WalletAccountException("Incompatible request " + request.getClass().getName() + ", expected " + EthSendRequest.class.getName());
    }

    public void signMessage(SignedMessage unsignedMessage, KeyParameter aesKey) {
    }

    public void verifyMessage(SignedMessage signedMessage) {
    }

    public String getPublicKeySerialized() {
        return Hex.toHexString(getPublicKey());
    }

    public void onConnection(BlockchainConnection blockchainConnection) {
        this.blockchainConnection = (EthServerClient) blockchainConnection;
        subscribeIfNeeded();
    }

    public void onDisconnect() {
        this.blockchainConnection = null;
        queueOnConnectivity();
    }

    void queueOnConnectivity() {
        final WalletConnectivityStatus connectivity = getConnectivityStatus();
        for (final ListenerRegistration<WalletAccountEventListener> registration : this.listeners) {
            registration.executor.execute(new Runnable() {
                public void run() {
                    ((WalletAccountEventListener) registration.listener).onConnectivityStatus(connectivity);
                    ((WalletAccountEventListener) registration.listener).onWalletChanged(EthFamilyWallet.this);
                }
            });
        }
    }
//TODO
    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void subscribeIfNeeded() {
        this.lock.lock();
        try
        {
        if ( this.blockchainConnection != null && this.getAddressesToWatch().size() > 0)
        this.blockchainConnection.subscribeToAddresses(getAddressesToWatch(), this);
        } catch (Exception e) {
         try
         {
           log.error("Error subscribing to addresses", e);
         } catch (Exception e2) {
           throw e;
        }
        }finally {
            this.lock.unlock();
        }
    }

    List<AbstractAddress> getAddressesToWatch() {
        Builder<AbstractAddress> addressesToWatch = ImmutableList.builder();
        for (AbstractAddress address : getActiveAddresses()) {
            addressesToWatch.add(address);
        }
        return addressesToWatch.build();
    }

    public ECKey findKeyFromPubHash(byte[] bytes) {
        throw new RuntimeException("Not implemented");
    }

    public ECKey findKeyFromPubKey(byte[] bytes) {
        throw new RuntimeException("Not implemented");
    }

    public RedeemData findRedeemDataFromScriptHash(byte[] bytes) {
        return null;
    }

    public void onNewBlock(BlockHeader header) {
        log.info("Got a {} block: {}", this.type.getName(), Integer.valueOf(header.getBlockHeight()));
        this.lock.lock();
        try {
            this.lastBlockSeenTimeSecs = Long.valueOf(header.getTimestamp());
            this.lastBlockSeenHeight = Integer.valueOf(header.getBlockHeight());
            for (EthTransaction tx : this.rawTransactions.values()) {
                tx.setDepthInBlocks(this.lastBlockSeenHeight.intValue());
            }
            queueOnNewBlock();
            ByteBuffer b = ByteBuffer.allocate(12);
            b.order(ByteOrder.LITTLE_ENDIAN);
            b.putInt(this.lastBlockSeenHeight.intValue());
            b.putLong(this.lastBlockSeenTimeSecs.longValue());
            getDbHeight().put(this.address.toString(), b.array());
            getDb().commit();
        } finally {
            this.lock.unlock();
        }
    }

    void queueOnNewBlock() {
        Preconditions.checkState(this.lock.isHeldByCurrentThread(), "Lock is held by another thread");
        for (final ListenerRegistration<WalletAccountEventListener> registration : this.listeners) {
            registration.executor.execute(new Runnable() {
                public void run() {
                    ((WalletAccountEventListener) registration.listener).onNewBlock(EthFamilyWallet.this);
                    ((WalletAccountEventListener) registration.listener).onWalletChanged(EthFamilyWallet.this);
                }
            });
        }
    }

    public void onBlockUpdate(BlockHeader header) {
    }

    public void onAddressStatusUpdate(AddressStatus status) {
        log.debug("Got a status {}", (Object) status);
        this.lock.lock();
        try {
            if (status.getStatus() == null) {
                commitAddressStatus(status);
            } else if (isAddressStatusChanged(status)) {
                this.balance = Value.valueOf(this.type, new BigInteger(status.getStatus().replace("0x", ""), 16));
                storeBalance();
                log.info("balance 1 " + this.balance);
                queueOnNewBalance();
            }

        } finally {
            this.lock.unlock();
        }
    }

    private void storeBalance() {
        this.lock.lock();
        try {
        getDbBalance().put(this.address.toString(), this.balance.getBigInt().toString(16));
        getDb().commit();
        } finally {
            this.lock.unlock();
        }
    }

    void commitAddressStatus(AddressStatus newStatus) {
        this.lock.lock();
        try {
            this.addressesStatus.put(newStatus.getAddress(), newStatus.getStatus());
            queueOnNewBalance();
            if (newStatus.getStatus() != null) {
                walletSaveLater();
            }
        } finally {
            this.lock.unlock();
        }
    }

    private boolean isAddressStatusChanged(AddressStatus addressStatus) {
        boolean z = true;
        this.lock.lock();
        try {
            AbstractAddress address = addressStatus.getAddress();
            String newStatus = addressStatus.getStatus();
            if (this.addressesStatus.containsKey(address)) {
                String previousStatus = (String) this.addressesStatus.get(address);
                if (previousStatus == null) {
                    if (newStatus == null) {
                        z = false;
                    }
                    this.lock.unlock();
                    return z;
                }
                if (previousStatus.equals(newStatus)) {
                    z = false;
                }
                this.lock.unlock();
                return z;
            } else if (newStatus == null) {
                commitAddressStatus(addressStatus);
                this.lock.unlock();
                return false;
            } else {
                this.lock.unlock();
                return true;
            }
        } finally {
            this.lock.unlock();
        }
    }

    void queueOnNewBalance() {
        Preconditions.checkState(this.lock.isHeldByCurrentThread(), "Lock is held by another thread");
        final Value balance = getBalance();
        for (final ListenerRegistration<WalletAccountEventListener> registration : this.listeners) {
            registration.executor.execute(new Runnable() {
                public void run() {
                    ((WalletAccountEventListener) registration.listener).onNewBalance(balance);
                    ((WalletAccountEventListener) registration.listener).onWalletChanged(EthFamilyWallet.this);
                }
            });
        }
    }

    public void onTransactionHistory(AddressStatus status, List<HistoryTx> list) {
    }

    public void onTransactionUpdate(EthTransaction tx) {
        if (log.isInfoEnabled()) {
            log.info("Got a new transaction {}", tx.getHashAsString());
        }
        this.lock.lock();
        try {
            addNewTransactionIfNeeded(tx);
        } finally {
            this.lock.unlock();
        }
    }

    void addNewTransactionIfNeeded(final EthTransaction tx) {
        this.lock.lock();
        try {
            log.info("adding transaction to wallet");
            this.rawTransactions.put(tx.getHash(), tx);
            getDbTransactions().put(tx.getHashAsString(), tx.getJSONString());
            getDb().commit();
            if (((AbstractAddress) tx.getReceivedFrom().get(0)).toString().equals(this.address.toString()) && (this.nonce == null || tx.getNonce().compareTo(this.nonce) > 0)) {
                this.nonce = tx.getNonce();
            }
            for (final ListenerRegistration<WalletAccountEventListener> registration : this.listeners) {
                registration.executor.execute(new Runnable() {
                    public void run() {
                        ((WalletAccountEventListener) registration.listener).onTransactionConfidenceChanged(EthFamilyWallet.this, tx);
                        ((WalletAccountEventListener) registration.listener).onWalletChanged(EthFamilyWallet.this);
                    }
                });
            }
            if (this.favoritesERC20Tokens.containsKey(tx.to.toString())) {
                EthContract.executeFunction(this, ((ERC20Token) this.favoritesERC20Tokens.get(tx.to.toString())).getAddress(), "balanceOf", "0", new String[]{getReceiveAddress().toString()});
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (Throwable th) {
            this.lock.unlock();
        }
        this.lock.unlock();
    }

    public BigInteger getNonce() {
        return this.nonce == null ? BigInteger.ZERO : this.nonce.add(BigInteger.ONE);
    }

    public void setNonce(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            this.nonce = null;
        } else {
            this.nonce = new BigInteger(nonce.replace("0x", ""), 16);
        }
    }

    public void onTransactionBroadcast(EthTransaction transaction) {
        this.lock.lock();
        try {
            log.info("Transaction sent {}", (Object) transaction);
            this.rawTransactions.putIfAbsent(transaction.getHash(), transaction);
        } finally {
            this.lock.unlock();
        }
    }

    public void onTransactionBroadcastError(EthTransaction transaction) {
    }

    public Collection<? extends EthContract> getContracts() {
        return this.ethContracts.values();
    }

    public Map<String, EthContract> getAllContracts() {
        return this.ethContracts;
    }
    public Map<String, ERC20Token> getAllERC20Tokens() {
        return this.erc20Tokens;
    }

    public Map<String, ERC20Token> getERC20Favorites() {
        return this.favoritesERC20Tokens;
    }
    public void addContractEventListener(String contractId, AccountContractEventListener listener) {
        addContractEventListener(contractId, listener, Threading.THREAD_POOL);
    }

    public void addContractEventListener(String contractId, AccountContractEventListener listener, Executor executor) {
        if (this.contractListeners.containsKey(contractId)) {
            ((List) this.contractListeners.get(contractId)).add(new ListenerRegistration(listener, executor));
            return;
        }
        List<ListenerRegistration<AccountContractEventListener>> lst = new ArrayList();
        lst.add(new ListenerRegistration(listener, executor));
        this.contractListeners.put(contractId, lst);
    }

    public void removeContractEventListener(String contractId, AccountContractEventListener listener) {
        ListenerRegistration.removeFromList(listener, (List) this.contractListeners.get(contractId));
    }

    public void subscribeToContract(String contractId) {
        if (this.blockchainConnection != null) {
            this.blockchainConnection.subscribeToContract(contractId, this);
            this.subscribed = true;
        }
    }

    public void onEvent(final JSONObject message) {
        if (message.has("type")) {
            try {
                String address;
                if (message.getString("type").equalsIgnoreCase("balance")) {
                    address = message.getString("address");
                    String balance = message.getString("balance");
                    if (this.ethContracts.containsKey(address)) {
                        ((EthContract) this.ethContracts.get(address)).setBalance(balance);
                        for (final ListenerRegistration<AccountContractEventListener> registration :  this.contractListeners.get(address)) {
                            registration.executor.execute(new Runnable() {
                                public void run() {
                                    ((AccountContractEventListener) registration.listener).onEvent(message);
                                }
                            });
                        }
                    }
                } else if (message.getString("type").equalsIgnoreCase("eth_call")) {
                    address = message.getString("address");
                    if (this.ethContracts.containsKey(address)) {
                        ((EthContract) this.ethContracts.get(address)).parseResult(message);
                        for (final ListenerRegistration<AccountContractEventListener> registration2 : this.contractListeners.get(address)) {
                            registration2.executor.execute(new Runnable() {
                                public void run() {
                                    ((AccountContractEventListener) registration2.listener).onEvent(message);
                                }
                            });
                        }
                        storeContractToDB((EthContract) this.ethContracts.get(address));
                    }
                } else if (message.getString("type").equalsIgnoreCase("eth_estimateGas")) {
                    address = message.getString("address");
                    if (this.ethContracts.containsKey(address)) {
                        ((EthContract) this.ethContracts.get(address)).parseResult(message);
                        for (final ListenerRegistration<AccountContractEventListener> registration22 : this.contractListeners.get(address)) {
                            registration22.executor.execute(new Runnable() {
                                public void run() {
                                    ((AccountContractEventListener) registration22.listener).onEvent(message);
                                }
                            });
                        }
                        storeContractToDB((EthContract) this.ethContracts.get(address));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void callContractFunction(String contractId, String functionName, Value amount, Object... inputValues) {
        EthContract contract = (EthContract) this.ethContracts.get(contractId);
        JSONObject ethCall = new JSONObject();
        Transaction tx = CallTransaction.createCallTransaction(0, 1, 1000000, contract.getContractAddress().replace("0x", ""), 0, contract.getContract().getByName(functionName), inputValues);
        try {
            ethCall.put("from", this.address.toString());
            ethCall.put("to", contractId);
            ethCall.put("data", Hex.toHexString(tx.getData()));
            if (!contract.getContract().getByName(functionName).constant) {
                ethCall.put("value", amount != null ? amount.getBigInt().toString(16) : "0x0");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (isConnected()) {
            this.blockchainConnection.callContract(contractId, functionName, ethCall, this);
        }
    }

    public void newContract(EthContract contract) {
        if (this.ethContracts.containsKey(contract.getContractAddress())) {
            contract.setHistory(((EthContract) this.ethContracts.get(contract.getContractAddress())).getAllHistory());
        }
            this.ethContracts.put(contract.getContractAddress(), contract);
            if (contract.isContractType("erc20")) {
                try {
                    if (this.erc20Tokens.containsKey(contract.getContractAddress())) {
                        ((ERC20Token) this.erc20Tokens.get(contract.getContractAddress())).update(contract);
                    } else {
                        ERC20Token t = ERC20Token.getERCToken(contract);
                        if (t != null) {
                            this.erc20Tokens.put(contract.getContractAddress(), t);
                        } else {
                            log.warn("Could not convert to ERC20Token the contract " + contract.getContractAddress());
                        }
                    }
                } catch (Throwable e) {
                    log.error("Error while processing ERC20Token", e);
                }
            }
            storeContractToDB(contract);

    }

    public void updateContract(JSONObject contract) {
        if (contract.has("address")) {
            try {
                ((EthContract) this.ethContracts.get(contract.getString("address"))).update(contract);
            } catch (Throwable e) {
                log.error("Error while parsing contract ", e);
            }
        }
    }

    private void storeContractToDB(EthContract contract) {
        this.lock.lock();
        try {
            if (getDbContracts().containsKey(contract.getContractAddress())) {
                getDbContracts().replace(contract.getContractAddress(), contract.toJSON().toString());
            } else {
            getDbContracts().put(contract.getContractAddress(), contract.toJSON().toString());
            }
            getDb().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.lock.unlock();
        }
    }

    public int getAccountIndex() {
        return this.keys.getAccountIndex();
    }

    public ImmutableList<ChildNumber> getDeterministicRootKeyPath() {
        return this.keys.getRootKey().getPath();
    }

    public List<CoinType> availableSubTypes() {
        return new C03558();
    }

    public List<CoinType> favoriteSubTypes() {
        return new C03569();
    }

    public EthContract getContract(ERC20Token type) {
        return getContract(type.getAddress());
    }

    public EthContract getContract(String contractAddress) {
        return (EthContract) getAllContracts().get(contractAddress);
    }

    public EthContract getContract(EthAddress address) {
        return getContract(address.toString());
    }

    public boolean isFavorite(CoinType subType) {
        return (subType instanceof ERC20Token) && getERC20Favorites().containsKey(((ERC20Token) subType).getAddress());
    }
}
