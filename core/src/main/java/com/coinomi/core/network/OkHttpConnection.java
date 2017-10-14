package com.coinomi.core.network;

import java.io.IOException;
import java.util.Collections;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class OkHttpConnection {
    protected static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    protected OkHttpClient client;

    protected OkHttpConnection(OkHttpClient client) {
        this.client = client;
    }

    protected OkHttpConnection() {
        this.client = getBuilder().build();
    }

    private static Builder getBuilder() {
        return new Builder().connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS));
    }

    protected JSONObject makeApiCall(Request request) throws JSONException, IOException {
        Response response = this.client.newCall(request).execute();
        if (response.isSuccessful()) {
            return parseReply(response);
        }
        JSONObject reply = parseReply(response);
        String genericMessage = "Error code " + response.code();
        if (reply != null) {
            genericMessage = reply.optString("error", genericMessage);
        }
        throw new IOException(genericMessage);
    }

    private static JSONObject parseReply(Response response) throws IOException, JSONException {
        return new JSONObject(response.body().string());
    }
}
