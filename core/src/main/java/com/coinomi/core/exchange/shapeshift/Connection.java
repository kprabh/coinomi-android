package com.coinomi.core.exchange.shapeshift;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

/**
 * @author John L. Jegutanis
 */
abstract public class Connection {
    private static final String DEFAULT_BASE_URL = "https://shapeshift.io/";

    OkHttpClient client;
    String baseUrl = DEFAULT_BASE_URL;

    protected Connection(OkHttpClient client) {
        this.client = client;
    }

    /*protected Connection() {
        client = new OkHttpClient();
        client.setConnectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS));
    }*/
    protected Connection() {
        this.baseUrl = "https://shapeshift.io/";
        this.client = getBuilder().build();
    }

    private static Builder getBuilder() {
        return new Builder().connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS));
    }
    protected String getApiUrl(String path) {
        return baseUrl + path;
    }
}
