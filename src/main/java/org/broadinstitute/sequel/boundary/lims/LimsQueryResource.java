package org.broadinstitute.sequel.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.commons.logging.Log;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftService;
import org.broadinstitute.sequel.nonthrift.jaxb.FlowcellDesignationType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

/**
 * @author breilly
 */
@Path("/limsQuery")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class LimsQueryResource {

    private ThriftService thriftService;

    private LimsQueryResourceResponseFactory responseFactory;

    private Log log;

    public LimsQueryResource() {}

    @Inject
    public LimsQueryResource(ThriftService thriftService, LimsQueryResourceResponseFactory responseFactory, Log log) {
        this.thriftService = thriftService;
        this.responseFactory = responseFactory;
        this.log = log;
    }

    @GET
    @Path("/findFlowcellDesignationByTaskName")
    public FlowcellDesignationType findFlowcellDesignationByTaskName(@QueryParam("taskName") String taskName) {
        try {
            FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByTaskName(taskName);
            return responseFactory.makeFlowcellDesignation(flowcellDesignation);
        } catch (TException e) {
            log.error(e);
            throw new WebApplicationException(javax.ws.rs.core.Response.serverError().entity(e.getMessage()).build());
        } catch (TZIMSException e) {
            log.error(e);
            throw new WebApplicationException(javax.ws.rs.core.Response.serverError().entity(e.getDetails()).build());
        } catch (Exception e) {
            log.error(e);
            throw new WebApplicationException(javax.ws.rs.core.Response.serverError().entity(e.getMessage()).build());
        }
    }
}
