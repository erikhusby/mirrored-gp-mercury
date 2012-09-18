package org.broadinstitute.gpinformatics.mercury.infrastructure.jaxrs;

import org.apache.commons.logging.Log;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author breilly
 */
@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<Exception> {

    private Log log;

    public CatchAllExceptionMapper() {}

    @Inject
    public CatchAllExceptionMapper(Log log) {
        this.log = log;
    }

    @Override
    public Response toResponse(Exception e) {
        log.error("Exception thrown from JAX-RS service", e);
        return Response.serverError().entity(e.getMessage()).build();
    }
}
