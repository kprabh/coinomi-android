package com.coinomi.core.network;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.coins.families.EthFamily;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.exceptions.UnsupportedCoinTypeException;
import com.coinomi.core.network.interfaces.BlockchainConnection;
import com.coinomi.core.wallet.WalletAccount;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerClientController {
    private static ConnectivityHelper DEFAULT_CONNECTIVITY_HELPER = new ConnectivityHelper() {
        public boolean isConnected() {
            return true;
        }
    };
    private static final Logger log = LoggerFactory.getLogger(ServerClient.class);
    private HashMap<CoinType, CoinAddress> addresses = new HashMap();
    private File cacheDir;
    private int cacheSize;
    private HashMap<String, BlockchainConnection> connections = new HashMap();
    private final ConnectivityHelper connectivityHelper;

    public ServerClientController(List<CoinAddress> coinAddresses, ConnectivityHelper connectivityHelper) {
        this.connectivityHelper = connectivityHelper;
        setupAddresses(coinAddresses);
    }

    private void setupAddresses(List<CoinAddress> coins) {
        for (CoinAddress coinAddress : coins) {
            this.addresses.put(coinAddress.getType(), coinAddress);
        }
    }

    public void resetAccount(WalletAccount account) {
        BlockchainConnection connection = (BlockchainConnection) this.connections.get(account.getId());
        if (connection != null) {
            connection.addEventListener(account);
            connection.resetConnection();
        }
    }

    public void startAsync(WalletAccount account) {
        if (account == null) {
            log.warn("Provided wallet account is null, not doing anything");
            return;
        }
        BlockchainConnection connection = getConnection(account);
        connection.addEventListener(account);
        connection.startAsync();
    }

    private BlockchainConnection getConnection(WalletAccount account) {
        String id = account.getId();
        CoinType type = account.getCoinType();
        if (this.connections.containsKey(id)) {
            return (BlockchainConnection) this.connections.get(id);
        }
        if (!this.addresses.containsKey(type)) {
            throw new RuntimeException("Tried to create connection for an unknown server.");
        } else if (type instanceof BitFamily) {
            ServerClient client = new ServerClient((CoinAddress) this.addresses.get(type), this.connectivityHelper);
            client.setCacheDir(this.cacheDir, this.cacheSize);
            this.connections.put(id, client);
            return client;
        } else if (type instanceof NxtFamily) {
            NxtServerClient client = new NxtServerClient((CoinAddress) this.addresses.get(type), this.connectivityHelper);
            client.setCacheDir(this.cacheDir, this.cacheSize);
            this.connections.put(id, client);
            return client;
        } else if (type instanceof EthFamily) {
            EthServerClient client = new EthServerClient((CoinAddress) this.addresses.get(type), this.connectivityHelper, account);
            client.setCacheDir(this.cacheDir, this.cacheSize);
            this.connections.put(id, client);
            return client;
        } else {
            throw new UnsupportedCoinTypeException(type);
        }
    }

    public void stopAllAsync() {
        for (BlockchainConnection client : this.connections.values()) {
            client.stopAsync();
        }
        this.connections.clear();
    }

    public void ping(String versionString) {
        for (String id : this.connections.keySet()) {
            BlockchainConnection connection = (BlockchainConnection) this.connections.get(id);
            if (connection.isActivelyConnected()) {
                connection.ping(versionString);
            }
        }
    }

    public void resetConnections() {
        for (String id : this.connections.keySet()) {
            BlockchainConnection connection = (BlockchainConnection) this.connections.get(id);
            if (connection.isActivelyConnected()) {
                connection.resetConnection();
            }
        }
    }

    public void setCacheDir(File cacheDir, int cacheSize) {
        this.cacheDir = cacheDir;
        this.cacheSize = cacheSize;
    }

    public void startOrResetAccountAsync(WalletAccount account) {
        if (this.connections.containsKey(account.getId())) {
            resetAccount(account);
        } else {
            startAsync(account);
        }
    }
}
