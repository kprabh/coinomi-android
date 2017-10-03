package com.coinomi.core.util;

import com.coinomi.core.coins.Value;

public class TransactionUtils {
    public static boolean isSending(Value value) {
        return value.signum() <= 0;
    }
}
