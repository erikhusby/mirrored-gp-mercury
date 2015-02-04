package org.broadinstitute.gpinformatics.mercury.boundary;

import javax.ws.rs.core.Response;

/**
 */
public class UnknownUserException extends ResourceException {

    private final String username;

    public UnknownUserException(String username) {
        //todo: FORBIDDEN or UNAUTHORIZED?
        super(Response.Status.FORBIDDEN);
        this.username = username;
    }

    @Override
    public String getMessage() {
        return String.format("Unknown user: %s", username);
    }
}
