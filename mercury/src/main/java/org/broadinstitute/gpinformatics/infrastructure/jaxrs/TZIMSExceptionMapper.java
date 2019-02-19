package org.broadinstitute.gpinformatics.infrastructure.jaxrs;

import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
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
public class TZIMSExceptionMapper implements ExceptionMapper<TZIMSException> {

    private Log log = LogFactory.getLog(this.getClass());

    @Override
    public Response toResponse(TZIMSException e) {
        log.error(e);
        return Response.serverError().entity(e.getDetails()).build();
    }
}
