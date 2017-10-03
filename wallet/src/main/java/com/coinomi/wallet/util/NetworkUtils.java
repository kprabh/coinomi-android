package com.coinomi.wallet.util;

import android.content.Context;

import com.coinomi.wallet.Constants;
import okhttp3.Cache;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author John L. Jegutanis
 */
public class NetworkUtils {
    private static OkHttpClient httpClient;

    public static OkHttpClient getHttpClient(Context context) {
        if (httpClient == null) {
            synchronized (NetworkUtils.class) {
                if (httpClient == null) {
                    httpClient = new Builder().connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS)).connectTimeout(30000, TimeUnit.MILLISECONDS).readTimeout(30000, TimeUnit.MILLISECONDS).cache(new Cache(new File(context.getCacheDir(), "http_cache"), 262144)).build();
                }
            }
        }
        return httpClient;
    }
}
