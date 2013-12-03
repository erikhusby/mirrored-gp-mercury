package org.broadinstitute.gpinformatics.mercury.boundary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Called by RESTEasy to translate a ResourceException into a web service response
 */
@Provider
public class ResourceExceptionMapper implements ExceptionMapper<ResourceException> {

    private final Log log = LogFactory.getLog(ResourceExceptionMapper.class);

    @Override
    public Response toResponse(ResourceException e) {
        log.error(e);
        return Response.status(e.getStatus()).entity(e.getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}
