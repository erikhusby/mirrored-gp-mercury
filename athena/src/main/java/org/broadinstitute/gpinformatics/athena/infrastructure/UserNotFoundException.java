package org.broadinstitute.gpinformatics.athena.infrastructure;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/14/12
 * Time: 2:52 PM
 */
public class UserNotFoundException extends Exception {

    public UserNotFoundException(String message) {
        super(message);
    }
}
