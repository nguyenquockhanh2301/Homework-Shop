package com.example.store.util;

/**
 * Thrown when a delete/update violates database referential integrity (FK constraints).
 */
public class DataIntegrityException extends RuntimeException {
    public DataIntegrityException(String message) { super(message); }
    public DataIntegrityException(String message, Throwable cause) { super(message, cause); }
}

