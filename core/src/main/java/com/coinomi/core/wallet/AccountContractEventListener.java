package com.coinomi.core.wallet;

import org.json.JSONObject;

public interface AccountContractEventListener {
    void onEvent(JSONObject jSONObject);
}
