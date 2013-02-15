package org.broadinstitute.gpinformatics.mercury.boundary;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * An exception thrown by JAX-RS web services
 */
public class ResourceException extends WebApplicationException {
    private String message;
    private Response.Status status;

    public ResourceException(String message, Response.Status status, Exception cause) {
        super(cause);
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public Response.Status getStatus() {
        return status;
    }
}
