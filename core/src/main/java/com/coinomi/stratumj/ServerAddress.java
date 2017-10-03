package com.coinomi.stratumj;

/**
 * @author John L. Jegutanis
 */
final public class ServerAddress {
    final private String host;
    final private int port;private final String path;
    public ServerAddress(String host, int port) {
        this(host, port, null);
    }
    public ServerAddress(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = path;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
    public String getPath() {
        return this.path;
    }
    @Override
    public String toString() {
        return "ServerAddress{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
