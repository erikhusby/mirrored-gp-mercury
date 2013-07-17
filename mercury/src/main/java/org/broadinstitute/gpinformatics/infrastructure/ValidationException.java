package org.broadinstitute.gpinformatics.infrastructure;

import javax.ejb.ApplicationException;
import java.util.ArrayList;
import java.util.List;

@ApplicationException(rollback = true)
public class ValidationException extends Exception {

    private final List<String> validationMessages = new ArrayList<String>();

    public ValidationException(String s) {
        super(s);
    }

    public ValidationException(String message, List<String> validationMessages) {
        super(message);
        this.validationMessages.addAll(validationMessages);
    }

    public ValidationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ValidationException(Throwable throwable) {
        super(throwable);
    }

    public List<String> getValidationMessages() {
        return validationMessages;
    }
}
