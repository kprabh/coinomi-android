package com.coinomi.core.exceptions;

import com.coinomi.core.coins.Value;
import com.google.common.base.Preconditions;

public class InsufficientMoneyException extends Exception {
    public final Value missing;

    public InsufficientMoneyException(Value missing) {
        this(missing, "Insufficient money,  missing " + missing.toFriendlyString());
    }

    public InsufficientMoneyException(Value missing, String message) {
        super(message);
        this.missing = (Value) Preconditions.checkNotNull(missing);
    }
}
