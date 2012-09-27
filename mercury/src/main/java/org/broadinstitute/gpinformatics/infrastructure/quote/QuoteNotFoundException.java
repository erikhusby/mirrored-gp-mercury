package org.broadinstitute.gpinformatics.infrastructure.quote;

public class QuoteNotFoundException extends Exception {

    public QuoteNotFoundException(String message,Throwable rootCause) {
        super(message,rootCause);
    }

    public QuoteNotFoundException(String message) {
        super(message);
    }
}
