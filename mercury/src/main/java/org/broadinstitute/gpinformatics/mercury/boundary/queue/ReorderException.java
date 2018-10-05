package org.broadinstitute.gpinformatics.mercury.boundary.queue;

public class ReorderException extends Exception {
    public ReorderException() {
    }

    public ReorderException(String s) {
        super(s);
    }

    public ReorderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReorderException(Throwable cause) {
        super(cause);
    }

    public ReorderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
