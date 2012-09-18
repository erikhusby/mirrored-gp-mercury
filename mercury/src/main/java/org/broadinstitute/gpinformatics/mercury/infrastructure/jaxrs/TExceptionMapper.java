package org.broadinstitute.gpinformatics.mercury.infrastructure.jaxrs;

import org.apache.commons.logging.Log;
import org.apache.thrift.TException;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author breilly
 */
@Provider
public class TExceptionMapper implements ExceptionMapper<TException> {

    @Inject
    private Log log;

    @Override
    public Response toResponse(TException e) {
        log.error(e);
        return Response.serverError().entity(e.getMessage()).build();
    }
}
