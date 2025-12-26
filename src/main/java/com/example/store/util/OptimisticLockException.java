package com.example.store.util;

public class OptimisticLockException extends Exception {
    public OptimisticLockException(String message) { super(message); }
}
