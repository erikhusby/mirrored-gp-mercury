package org.broadinstitute.gpinformatics.infrastructure;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends Exception {

    private final List<String> validationMessages = new ArrayList<>();

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
