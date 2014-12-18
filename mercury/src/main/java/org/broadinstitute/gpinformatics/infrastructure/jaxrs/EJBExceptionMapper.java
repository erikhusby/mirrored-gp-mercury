package org.broadinstitute.gpinformatics.infrastructure.jaxrs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

/**
 * This ExceptionMapper called when EJBException is thrown.
 */
@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBException> {

    private Log log = LogFactory.getLog(EJBExceptionMapper.class);

    @Context
    private Providers providers;

    @Override
    @SuppressWarnings("unchecked") // for mapper.toResponse()
    public Response toResponse(EJBException exception) {

        // EJBExceptions are most likely wrapped around other kinds of exceptions. If this is the case, find the
        // cause and check if it has its own ExceptionMapper.
        Throwable cause = exception.getCause();
        if (cause != null) {
            log.error("EJBException's CausedByException", cause);
            ExceptionMapper mapper = providers.getExceptionMapper(cause.getClass());
            if (mapper == null) {
                return Response.serverError().entity(cause.getMessage()).build();
            }
            return mapper.toResponse(cause);
        } else {
            log.error("EJBException thrown from JAX-RS service", exception);
            return Response.serverError().entity(exception.getMessage()).build();
        }
    }
}
