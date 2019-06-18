package org.broadinstitute.gpinformatics.infrastructure.jaxrs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author breilly
 */
@Provider
public class CatchAllExceptionMapper implements ExceptionMapper<Exception> {

    private Log log = LogFactory.getLog(this.getClass());

    public CatchAllExceptionMapper() {}

    @Override
    public Response toResponse(Exception e) {
        log.error("Exception thrown from JAX-RS service", e);
        return Response.serverError().entity(e.getMessage()).build();
    }
}
