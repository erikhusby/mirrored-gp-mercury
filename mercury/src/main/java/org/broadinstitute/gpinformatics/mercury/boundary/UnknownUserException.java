package org.broadinstitute.gpinformatics.mercury.boundary;

import javax.ws.rs.core.Response;

/**
 * An exception to be thrown from a web service when a user requesting an operation is not known. This will map to an
 * HTTP 403 "FORBIDDEN" response, indicating that the user, whoever they are, is not allowed access to the web service.
 * Note that the other option, 401 "UNAUTHORIZED", would imply that we know who the user is and we are denying access
 * for that user.
 */
public class UnknownUserException extends ResourceException {

    private final String username;

    public UnknownUserException(String username) {
        super(Response.Status.FORBIDDEN);
        this.username = username;
    }

    @Override
    public String getMessage() {
        return String.format("Unknown user: %s", username);
    }
}
