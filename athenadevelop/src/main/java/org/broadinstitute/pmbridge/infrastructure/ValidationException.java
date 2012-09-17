package org.broadinstitute.pmbridge.infrastructure;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/10/12
 * Time: 5:37 PM
 */
public class ValidationException extends Exception {

    public ValidationException(String s) {
        super(s);
    }

    public ValidationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ValidationException(Throwable throwable) {
        super(throwable);
    }
}
