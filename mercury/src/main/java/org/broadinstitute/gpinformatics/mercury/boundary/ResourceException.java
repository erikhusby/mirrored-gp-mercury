package org.broadinstitute.gpinformatics.mercury.boundary;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * An exception thrown by JAX-RS web services
 */
public class ResourceException extends WebApplicationException {
    public ResourceException(String message, int status) {
        super(Response.status(status).entity(message).type(MediaType.TEXT_PLAIN_TYPE).build());
    }
}
