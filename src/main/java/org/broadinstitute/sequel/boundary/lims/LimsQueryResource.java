package org.broadinstitute.sequel.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.commons.logging.Log;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.sequel.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftService;
import org.broadinstitute.sequel.limsquery.generated.FlowcellDesignationType;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author breilly
 */
@Path("/limsQuery")
public class LimsQueryResource {

    private ThriftService thriftService;

    private LimsQueryResourceResponseFactory responseFactory;

    public LimsQueryResource() {}

    @Inject
    public LimsQueryResource(ThriftService thriftService, LimsQueryResourceResponseFactory responseFactory) {
        this.thriftService = thriftService;
        this.responseFactory = responseFactory;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/doesLimsRecognizeAllTubes")
    public boolean doesLimsRecognizeAllTubes(List<String> barcodes) throws TException, TZIMSException {
        return thriftService.doesSquidRecognizeAllLibraries(barcodes);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/findFlowcellDesignationByTaskName")
    public FlowcellDesignationType findFlowcellDesignationByTaskName(@QueryParam("taskName") String taskName) throws TException, TZIMSException {
        FlowcellDesignationType flowcellDesignationType;
        try {
            FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByTaskName(taskName);
            flowcellDesignationType = responseFactory.makeFlowcellDesignation(flowcellDesignation);
        } catch (TTransportException e) {
            /* This seems to be thrown when the designation doesn't exist.
             * Looking at LIMQueriesImpl.java,
             * findFlowcellDesignationByTaskName(FcellDesignationGroup, EntityManager)
             * is given null in this case and likely throws a
             * NullPointerException, though that detail is not exposed to thrift
             * clients. Returning null here will result in "204 No Content".
             */
            flowcellDesignationType = null;
        }
        return flowcellDesignationType;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/findFlowcellDesignationByFlowcellBarcode")
    public FlowcellDesignationType findFlowcellDesignationByFlowcellBarcode(@QueryParam("flowcellBarcode") String flowcellBarcode) throws TException, TZIMSException {
        FlowcellDesignationType flowcellDesignationType;
        try {
            FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByFlowcellBarcode(flowcellBarcode);
            flowcellDesignationType = responseFactory.makeFlowcellDesignation(flowcellDesignation);
        } catch (TTransportException e) {
            // This seems to be thrown when the flowcell doesn't exist
            flowcellDesignationType = null;
        }
        return flowcellDesignationType;
    }
}
