package org.broadinstitute.sequel.quotes;

public class QuoteServerException extends Exception {
    public QuoteServerException() {
        super();
    }

    public QuoteServerException(String message) {
        super(message);
    }

    public QuoteServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuoteServerException(Throwable cause) {
        super(cause);  
    }
}
