package org.broadinstitute.gpinformatics.athena.infrastructure;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/10/12
 * Time: 5:56 PM
 */
public class ConnectionException extends RuntimeException {

    public ConnectionException(final String s) {
        super(s);
    }

    public ConnectionException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public ConnectionException(final Throwable throwable) {
        super(throwable);
    }
}
