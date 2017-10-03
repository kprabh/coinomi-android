package com.coinomi.wallet.util;

import android.os.Handler;
import com.coinomi.core.wallet.AccountContractEventListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;

public abstract class ThrottlingAccountContractChangeListener implements AccountContractEventListener {
    private final boolean connectivityRelevant;
    private final boolean eventRelevant;
    private final Handler handler;
    private final AtomicLong lastMessageTime;
    private final AtomicBoolean relevant;
    private final Runnable runnable;
    private final long throttleMs;

    public abstract void onThrottledContractChanged();

    public ThrottlingAccountContractChangeListener() {
        this(500);
    }

    public ThrottlingAccountContractChangeListener(long throttleMs) {
        this(throttleMs, true, true);
    }

    public ThrottlingAccountContractChangeListener(long throttleMs, boolean eventRelevant, boolean connectivityRelevant) {
        this.lastMessageTime = new AtomicLong(0);
        this.handler = new Handler();
        this.relevant = new AtomicBoolean();
        this.runnable = new Runnable() {
            public void run() {
                ThrottlingAccountContractChangeListener.this.lastMessageTime.set(System.currentTimeMillis());
                ThrottlingAccountContractChangeListener.this.onThrottledContractChanged();
            }
        };
        this.throttleMs = throttleMs;
        this.eventRelevant = eventRelevant;
        this.connectivityRelevant = connectivityRelevant;
    }

    public void removeCallbacks() {
        this.handler.removeCallbacksAndMessages(null);
    }

    public void onEvent(JSONObject event) {
        checkRelevant(this.eventRelevant);
    }

    public final void checkRelevant(boolean isRelevant) {
        if (isRelevant) {
            this.handler.removeCallbacksAndMessages(null);
            if (System.currentTimeMillis() - this.lastMessageTime.get() > this.throttleMs) {
                this.handler.post(this.runnable);
            } else {
                this.handler.postDelayed(this.runnable, this.throttleMs);
            }
        }
    }
}
