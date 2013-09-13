package org.broadinstitute.gpinformatics.infrastructure;

import javax.ejb.ApplicationException;
import java.util.List;

@ApplicationException(rollback = true)
public class ValidationWithRollbackException extends ValidationException {

    public ValidationWithRollbackException(String s) {
        super(s);
    }

    public ValidationWithRollbackException(String message, List<String> validationMessages) {
        super(message, validationMessages);
    }

    public ValidationWithRollbackException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ValidationWithRollbackException(Throwable throwable) {
        super(throwable);
    }

    public ValidationWithRollbackException(ValidationException e) {
        super(e.getMessage(), e.getValidationMessages());
    }
}
