package com.dataconversion.util;

/**
 * Exception for header detection errors.
 */
public class HeaderDetectionException extends ConversionException {
    public HeaderDetectionException(String message) {
        super(message);
    }

    public HeaderDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
