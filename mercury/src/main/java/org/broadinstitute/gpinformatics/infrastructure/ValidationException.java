package org.broadinstitute.gpinformatics.infrastructure;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/10/12
 * Time: 5:37 PM
 */
public class ValidationException extends Exception {

    Set<String> validationMessages = new HashSet<String>();

    public ValidationException(String s) {
        super(s);
    }

    public ValidationException(String message, Set<String> validationMessages) {
        super(message);
        this.validationMessages = validationMessages;
    }

    public ValidationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ValidationException(Throwable throwable) {
        super(throwable);
    }
}
