package com.dataconversion.util;

/**
 * Exception for coordinate matching errors.
 */
public class CoordinateMatchingException extends ConversionException {
    public CoordinateMatchingException(String message) {
        super(message);
    }

    public CoordinateMatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
