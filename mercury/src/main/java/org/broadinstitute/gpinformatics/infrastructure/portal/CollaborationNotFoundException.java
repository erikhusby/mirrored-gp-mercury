package org.broadinstitute.gpinformatics.infrastructure.portal;

public class CollaborationNotFoundException extends Exception {

    private static final long serialVersionUID = 382060262974845393L;

    public CollaborationNotFoundException(String message, Throwable rootCause) {
        super(message,rootCause);
    }

    public CollaborationNotFoundException(String message) {
        super(message);
    }
}
