package org.broadinstitute.sequel.infrastructure.jaxrs;

import org.apache.commons.logging.Log;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author breilly
 */
@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBException> {

    @Inject
    private Log log;

    @Override
    public Response toResponse(EJBException e) {
        log.error(e);
        Exception realCause;
        if (e.getCausedByException() != null) {
            realCause = e.getCausedByException();
            log.error(realCause);
        } else {
            realCause = e;
        }
        return Response.serverError().entity(realCause.getMessage()).build();
    }
}
