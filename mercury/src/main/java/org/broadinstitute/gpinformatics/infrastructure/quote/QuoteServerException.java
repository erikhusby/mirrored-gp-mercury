package org.broadinstitute.gpinformatics.infrastructure.quote;

public class QuoteServerException extends Exception {

    public QuoteServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuoteServerException(String message) { super(message); }
}
