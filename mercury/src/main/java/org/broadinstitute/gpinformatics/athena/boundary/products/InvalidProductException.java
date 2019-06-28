package org.broadinstitute.gpinformatics.athena.boundary.products;

public class InvalidProductException extends Exception {

    public InvalidProductException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidProductException(Throwable cause) {
        super(cause);
    }

    public InvalidProductException(String message) {
        super(message);
    }
}
