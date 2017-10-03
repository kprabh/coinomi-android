package com.coinomi.core.network;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.network.interfaces.BlockchainConnection;
import com.coinomi.core.network.interfaces.ClientConnection;
import com.coinomi.core.network.interfaces.ConnectionEventListener;
import com.coinomi.stratumj.ServerAddress;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.bitcoinj.utils.ListenerRegistration;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServerClientBase implements ClientConnection {
    private static final Random RANDOM = new Random();
    private static final ScheduledThreadPoolExecutor connectionExec = new ScheduledThreadPoolExecutor(1);
    private static final Logger log = LoggerFactory.getLogger(ServerClientBase.class);
    private final ImmutableList<ServerAddress> addresses;
    private File cacheDir;
    private int cacheSize;
    private final ConnectivityHelper connectivityHelper;
    private transient CopyOnWriteArrayList<ListenerRegistration<ConnectionEventListener>> eventListeners;
    private final HashSet<ServerAddress> failedAddresses;
    private boolean isConnecting = false;
    private ServerAddress lastServerAddress;
    private long reconnectAt = 0;
    private Runnable reconnectTask = new Runnable() {
        public void run() {
            if (!ServerClientBase.this.isActivelyConnected()) {
                if (ServerClientBase.this.stopped) {
                    ServerClientBase.log.info("{} client stopped, aborting reconnect.", ServerClientBase.this.type.getName());
                    return;
                }
                long reconnectIn = Math.max(ServerClientBase.this.reconnectAt - System.currentTimeMillis(), 0);
                if (reconnectIn >= 1000) {
                    ServerClientBase.this.reschedule(ServerClientBase.this.reconnectTask, reconnectIn, TimeUnit.MILLISECONDS);
                } else if (ServerClientBase.this.connectivityHelper.isConnected()) {
                    ServerClientBase.this.startAsync();
                } else {
                    ServerClientBase.this.reschedule(ServerClientBase.this.reconnectTask, 1, TimeUnit.SECONDS);
                }
            }
        }
    };
    private Runnable resetExponentialBackoffTime = new Runnable() {
        public void run() {
            if (ServerClientBase.this.isActivelyConnected()) {
                ServerClientBase.this.reconnectAt = 0;
                ServerClientBase.this.retrySeconds = 0;
            }
        }
    };
    private long retrySeconds = 0;
    private boolean stopped = false;
    protected CoinType type;

    protected abstract void deleteNetworkClient();

    protected abstract BlockchainConnection getThisBlockchainConnection();

    protected abstract boolean isNetworkClientAvailable();

    protected abstract void setupNetworkClient(ServerAddress serverAddress);

    protected abstract void startNetworkClientAsync();

    private void reschedule(Runnable r, long delay, TimeUnit unit) {
        connectionExec.remove(r);
        connectionExec.schedule(r, delay, unit);
    }

    public ServerClientBase(CoinAddress coinAddress, ConnectivityHelper connectivityHelper) {
        this.connectivityHelper = connectivityHelper;
        this.eventListeners = new CopyOnWriteArrayList();
        this.failedAddresses = new HashSet();
        this.type = coinAddress.getType();
        this.addresses = ImmutableList.copyOf(coinAddress.getAddresses());
    }

    protected void onNetworkClientDisconnected() {
        log.info("{} client stopped", this.type.getName());
        this.isConnecting = false;
        broadcastOnDisconnect();
        this.failedAddresses.add(this.lastServerAddress);
        this.lastServerAddress = null;
        deleteNetworkClient();
        if (!this.stopped) {
            log.info("Reconnecting {} in {} seconds", this.type.getName(), Long.valueOf(this.retrySeconds));
            cancelConnectionCheck();
            cancelReconnectTask();
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (this.retrySeconds > 0) {
            this.reconnectAt = System.currentTimeMillis() + (this.retrySeconds * 1000);
            connectionExec.schedule(this.reconnectTask, this.retrySeconds, TimeUnit.SECONDS);
            return;
        }
        connectionExec.execute(this.reconnectTask);
    }

    protected void onNetworkClientConnected() {
        if (isActivelyConnected()) {
            log.info("{} client connected to {}", this.type.getName(), this.lastServerAddress);
            this.isConnecting = false;
            broadcastOnConnection();
            cancelReconnectTask();
            reschedule(this.resetExponentialBackoffTime, 30, TimeUnit.SECONDS);
        }
    }

    private void newNetworkClient() {
        this.lastServerAddress = getServerAddress();
        setupNetworkClient(this.lastServerAddress);
    }

    private ServerAddress getServerAddress() {
        ServerAddress address;
        if (this.failedAddresses.size() == this.addresses.size()) {
            this.failedAddresses.clear();
        }
        this.retrySeconds = Math.min(Math.max(1, this.retrySeconds * 2), 16);
        do {
            address = (ServerAddress) this.addresses.get(RANDOM.nextInt(this.addresses.size()));
        } while (this.failedAddresses.contains(address));
        return address;
    }

    public void resetConnection() {
        this.isConnecting = false;
        deleteNetworkClient();
    }

    public void startAsync() {
        if (this.isConnecting) {
            log.debug("Not starting service as it is explicitly stopped or connecting");
            return;
        }
        if (!isNetworkClientAvailable()) {
            log.info("Forcing service start " + this.type.getName());
            cancelReconnectTask();
            newNetworkClient();
        }
        if (!isActivelyConnected()) {
            startNetworkClientAsync();
            this.isConnecting = true;
            reschedule(this.reconnectTask, 16, TimeUnit.SECONDS);
        }
    }

    private void cancelReconnectTask() {
        connectionExec.remove(this.reconnectTask);
    }

    private void cancelConnectionCheck() {
        connectionExec.remove(this.resetExponentialBackoffTime);
    }

    public void stopAsync() {
        if (!this.stopped) {
            this.stopped = true;
            this.isConnecting = false;
            if (isActivelyConnected()) {
                broadcastOnDisconnect();
            }
            this.eventListeners.clear();
            cancelReconnectTask();
            deleteNetworkClient();
        }
    }

    public void addEventListener(ConnectionEventListener listener) {
        addEventListener(listener, Threading.USER_THREAD);
    }

    private void addEventListener(ConnectionEventListener listener, Executor executor) {
        boolean isNew = !ListenerRegistration.removeFromList(listener, this.eventListeners);
        this.eventListeners.add(new ListenerRegistration(listener, executor));
        if (isNew && isActivelyConnected()) {
            broadcastOnConnection();
        }
    }

    private void broadcastOnConnection() {
        Iterator it = this.eventListeners.iterator();
        while (it.hasNext()) {
            final ListenerRegistration<ConnectionEventListener> registration = (ListenerRegistration) it.next();
            registration.executor.execute(new Runnable() {
                public void run() {
                    ((ConnectionEventListener) registration.listener).onConnection((BlockchainConnection) Preconditions.checkNotNull(ServerClientBase.this.getThisBlockchainConnection()));
                }
            });
        }
    }

    private void broadcastOnDisconnect() {
        Iterator it = this.eventListeners.iterator();
        while (it.hasNext()) {
            final ListenerRegistration<ConnectionEventListener> registration = (ListenerRegistration) it.next();
            registration.executor.execute(new Runnable() {
                public void run() {
                    ((ConnectionEventListener) registration.listener).onDisconnect();
                }
            });
        }
    }

    public void setCacheDir(File cacheDir, int cacheSize) {
        this.cacheDir = cacheDir;
        this.cacheSize = cacheSize;
    }
}
