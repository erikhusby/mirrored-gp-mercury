package org.broadinstitute.gpinformatics.mercury.infrastructure;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/10/12
 * Time: 5:56 PM
 */
public class SubmissionException extends Exception {

    public SubmissionException(String s) {
        super(s);
    }

    public SubmissionException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public SubmissionException(Throwable throwable) {
        super(throwable);
    }
}
