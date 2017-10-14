package com.coinomi.core.exceptions;

public class ExecutionException extends Exception {
    public ExecutionException(Exception e) {
        super(e);
    }

    public ExecutionException(String s) {
        super(s);
    }
}
