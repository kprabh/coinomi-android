package com.coinomi.core.network;


import com.coinomi.core.Preconditions;
import com.coinomi.core.network.interfaces.BlockchainConnection;
import com.coinomi.core.network.interfaces.TransactionEventListener;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AccountContractEventListener;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletAccount.WalletAccountException;
import com.coinomi.core.wallet.families.eth.EthBlockchainConnection;
import com.coinomi.core.wallet.families.eth.EthContract;
import com.coinomi.core.wallet.families.eth.EthContractSuits;
import com.coinomi.core.wallet.families.eth.EthFamilyWallet;
import com.coinomi.core.wallet.families.eth.EthSendRequest;
import com.coinomi.core.wallet.families.eth.EthTransaction;
import com.coinomi.stratumj.ServerAddress;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.bitcoinj.core.Sha256Hash;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class EthServerClient extends ServerClientBase implements EthBlockchainConnection {
    private static final TimeUnit TIMEOUT_UNITS = TimeUnit.MILLISECONDS;
    private static final Logger log = LoggerFactory.getLogger(EthServerClient.class);
    private final WalletAccount account;
    private final HashMap<AbstractAddress, TransactionEventListener<EthTransaction>> addressSubscribers;
    private final ConcurrentHashMap<Long, SettableFuture<JSONObject>> callers = new ConcurrentHashMap();
    private final HashMap<String, AccountContractEventListener> contractSubscribers;
    private AtomicLong idCounter = new AtomicLong();
    private WebSocket ws;

    public EthServerClient(CoinAddress coinAddress, ConnectivityHelper connectivityHelper, WalletAccount account) {
        super(coinAddress, connectivityHelper);
        this.account = account;
        this.addressSubscribers = new HashMap();
        this.contractSubscribers = new HashMap();
    }

    protected void setupNetworkClient(ServerAddress address) {
        String accountSub = this.account.getReceiveAddress().toString();
        String serverAddr = String.format(Locale.US, "ws://%s:%d/%s?address=%s&height=%d", new Object[]{address.getHost(), Integer.valueOf(address.getPort()), address.getPath(), accountSub, Integer.valueOf(this.account.getLastBlockSeenHeight())});
        log.info("ethereum websocket: " + serverAddr);
        try {
            this.ws = new WebSocketFactory().setConnectionTimeout(15000).createSocket(serverAddr).addListener(new WebSocketAdapter() {
                public void onTextMessage(WebSocket websocket, String message) {
                    EthServerClient.log.info(message);
                    EthServerClient.this.parseMessage(message);
                }

                public void onConnected(WebSocket websocket, Map<String, List<String>> map) {
                    EthServerClient.log.info("ethereum service connected");
                    EthServerClient.this.onNetworkClientConnected();
                }

                public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                    EthServerClient.log.info("ethereum service disconnected");
                    EthServerClient.this.onNetworkClientDisconnected();
                }

                public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                    EthServerClient.log.info("ethereum service connection error: " + exception.getMessage());
                    EthServerClient.this.onNetworkClientDisconnected();
                }
            }).addExtension("permessage-deflate");
        } catch (IOException e) {
            onNetworkClientDisconnected();
        }
    }

    protected boolean isNetworkClientAvailable() {
        return this.ws != null;
    }

    protected void startNetworkClientAsync() {
        if (this.ws != null) {
            Preconditions.checkState(!this.ws.isOpen());
            this.ws.connectAsynchronously();
        }
    }

    public void deleteNetworkClient() {
        if (this.ws != null) {
            this.ws.disconnect();
        }
        this.ws = null;
    }

    protected BlockchainConnection getThisBlockchainConnection() {
        return this;
    }

    public boolean isActivelyConnected() {
        return this.ws != null && this.ws.isOpen();
    }

    private void parseMessage(String message) {
        try {
            JSONObject obj = new JSONObject(message);
            if (obj.has("id")) {
                long id = obj.getLong("id");
                if (this.callers.containsKey(Long.valueOf(id))) {
                    SettableFuture<JSONObject> future = (SettableFuture) this.callers.get(Long.valueOf(id));
                    this.callers.remove(Long.valueOf(id));
                    future.set(obj);
                    return;
                }
                log.error("No caller found with id " + id);
            } else if (!obj.has("msg")) {
            } else {
                if (obj.getString("msg").equals("balance")) {
                    newBalanceMessage(obj);
                } else if (obj.getString("msg").equals("block")) {
                    newBlockMessage(obj);
                } else if (obj.getString("msg").equals("contract")) {
                    newContract(obj);
                } else if (obj.getString("msg").equals("transaction")) {
                    newTransactionMessage(obj);
                } else if (obj.getString("msg").equals("contract_event")) {
                    newContractMessage(obj);
                } else if (obj.getString("msg").equals("contract_template")) {
                    newContractTemplate(obj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void newContract(JSONObject obj) {
        if (obj.has("address")) {
            try {
                log.info("new contract event");
                ((EthFamilyWallet) this.account).newContract(EthContract.fromJSON(this.type, obj));
            } catch (JSONException e) {
                log.error("error parsing contract event");
                e.printStackTrace();
            }
        }
    }

    private void newContractMessage(JSONObject obj) throws JSONException {
        if (obj.has("address")) {
            try {
                if (this.contractSubscribers.containsKey(obj.getString("address"))) {
                    log.info("new contract event " + obj.toString());
                    ((AccountContractEventListener) this.contractSubscribers.get(obj.getString("address"))).onEvent(obj);
                }
            } catch (JSONException e) {
                log.error("error parsing contract event");
                e.printStackTrace();
            }
        }
    }

    private void newContractTemplate(JSONObject obj) throws JSONException {
        EthContractSuits.parseTemplate(obj);
    }

    private void newBlockMessage(JSONObject obj) {
        BlockHeader blockHeader;
        JSONException e;
        for (TransactionEventListener t : this.addressSubscribers.values()) {
            log.info("new block");
            try {
                BlockHeader header = new BlockHeader(this.type, obj.getLong("timestamp"), obj.getInt("height"));
               // try {
                    t.onNewBlock(header);
                    blockHeader = header;
            /*    } catch (JSONException e2) {
                    e = e2;
                    blockHeader = header;
                    e.printStackTrace();
                }*/
            } catch (JSONException e3) {
                e = e3;
                e.printStackTrace();
            }
        }
    }

    private void newTransactionMessage(JSONObject obj) throws JSONException {
        for (TransactionEventListener t : this.addressSubscribers.values()) {
            log.info("new transaction");
            t.onTransactionUpdate(new EthTransaction(this.type, obj));
        }
    }

    private void newBalanceMessage(JSONObject obj) throws JSONException {
        Object newBalance = this.type.value(new BigInteger(obj.getString("balance").replace("0x", ""), 16));
        log.info("New balance {}", newBalance);
        if (!this.account.getBalance().equals(newBalance)) {
            for (TransactionEventListener t : this.addressSubscribers.values()) {
                log.info("new balance sending to transaction listener");
                t.onAddressStatusUpdate(new AddressStatus((AbstractAddress) this.addressSubscribers.keySet().iterator().next(), obj.getString("balance")));
            }
        }
        ((EthFamilyWallet) this.account).setNonce(obj.getString("nonce"));
    }


    public void subscribeToContract(String contractId, AccountContractEventListener listener) {
        this.contractSubscribers.put(contractId, listener);
        JSONObject obj = new JSONObject();
        try {
            obj.put("msg", "contract_event");
            obj.put("type", "balance");
            obj.put("address", contractId);
            if (this.ws != null) {
                this.ws.sendText(obj.toString());
            }
        } catch (Throwable e) {
            log.error("Error parsing JSON", e);
        }
    }

    public void callContract(String contractId, String functionName, JSONObject ethCall, AccountContractEventListener listener) {
        this.contractSubscribers.put(contractId, listener);
        JSONObject obj = new JSONObject();
        try {
            obj.put("msg", "contract_event");
            obj.put("address", contractId);
            obj.put("function", functionName);
            if (ethCall.has("value")) {
                obj.put("eth_estimateGas", ethCall);
                obj.put("type", "eth_estimateGas");
            } else {
                obj.put("eth_call", ethCall);
                obj.put("type", "eth_call");
            }
            if (this.ws != null) {
                this.ws.sendText(obj.toString());
            }
        } catch (Throwable e) {
            log.error("Error parsing JSON", e);
        }
    }

    @Deprecated
    public boolean broadcastTxSync(EthTransaction tx) {
        log.info("Broadcasting transaction..");
        JSONObject obj = new JSONObject();
        try {
            obj.put("msg", "broadcast_tx");
            obj.put("raw_tx", Hex.toHexString(tx.getRawTransaction()));
            JSONObject reply = (JSONObject) call(obj).get(15000, TIMEOUT_UNITS);
            if (!reply.has("error")) {
                return true;
            }
            log.error(reply.getString("error"));
            return false;
        } catch (Throwable e) {
            log.error("IOException", e);
            return false;
        }
    }

    public void ping(String versionString) {
    }

    public void setCacheDir(File cacheDir, int cacheSize) {
    }

    private void send(JSONObject message) throws IOException {
        if (this.ws != null) {
            this.ws.sendText(message.toString());
            return;
        }
        throw new IOException("No blockchain connection");
    }

    public void estimateGasSync(EthSendRequest request) throws WalletAccountException {
        log.info("Estimate gas..");
        try {
            JSONObject obj = new JSONObject();
            obj.put("msg", "contract_event");
            obj.put("type", "eth_estimateGas");
            obj.put("eth_estimateGas", request.getJsonDetails());
            request.updateGasLimit(new BigInteger(((JSONObject) call(obj).get(15000, TIMEOUT_UNITS)).getString("result").replace("0x", ""), 16));
        } catch (WalletAccountException e) {
            throw e;
        } catch (Throwable e2) {
            throw new WalletAccountException(e2);
        }
    }

    public ListenableFuture<JSONObject> call(JSONObject message) {
        SettableFuture<JSONObject> future = SettableFuture.create();
        long id = this.idCounter.getAndIncrement();
        try {
            message.put("id", id);
            send(message);
            this.callers.put(Long.valueOf(id), future);
        } catch (Throwable e) {
            future.setException(e);
            log.error("Error making a call to eth server: {}", e.getMessage());
        }
        return future;
    }

    public void getBlock(int height, TransactionEventListener listener) {

    }

    public void subscribeToBlockchain(TransactionEventListener listener) {

    }

    public void getHistoryTx(AddressStatus status, TransactionEventListener listener) {

    }

    public void getTransaction(Sha256Hash txHash, TransactionEventListener listener) {

    }

    public void broadcastTx(Object tx, TransactionEventListener listener) {

    }

    public boolean broadcastTxSync(Object tx) {
        return false;
    }

    public void subscribeToAddresses(List list, TransactionEventListener listener) {

    }
}
