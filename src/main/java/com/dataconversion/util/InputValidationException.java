package com.dataconversion.util;

/**
 * Exception for input validation errors.
 */
public class InputValidationException extends ConversionException {
    public InputValidationException(String message) {
        super(message);
    }

    public InputValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
