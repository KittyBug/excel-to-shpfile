package com.dataconversion.util;

/**
 * Base exception for data conversion errors.
 */
public class ConversionException extends Exception {
    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}

