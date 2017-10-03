package com.coinomi.core.exceptions;

public class ResetKeyException extends Exception {
    public ResetKeyException(String message) {
        super(message);
    }

    public ResetKeyException(Throwable cause) {
        super(cause);
    }
}
