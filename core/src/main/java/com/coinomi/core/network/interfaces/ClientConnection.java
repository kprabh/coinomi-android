package com.coinomi.core.network.interfaces;

public interface ClientConnection {
    void addEventListener(ConnectionEventListener connectionEventListener);

    boolean isActivelyConnected();

    void ping(String str);

    void resetConnection();

    void startAsync();

    void stopAsync();
}
