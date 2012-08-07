package org.broadinstitute.sequel.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftService;
import org.broadinstitute.sequel.limsquery.generated.FlowcellDesignationType;
import org.broadinstitute.sequel.limsquery.generated.LibraryDataType;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    @Path("/fetchLibraryDetailsByTubeBarcode")
    public List<LibraryDataType> fetchLibraryDetailsByTubeBarcode(List<String> tubeBarcodes, @QueryParam("includeWorkRequestDetails") boolean includeWorkRequestDetails) throws TException, TZIMSException {
        List<LibraryData> libraryData = thriftService.fetchLibraryDetailsByTubeBarcode(tubeBarcodes, includeWorkRequestDetails);
        List<LibraryDataType> result = new ArrayList<LibraryDataType>();
        for (LibraryData data : libraryData) {
            result.add(responseFactory.makeLibraryData(data));
        }
        return result;
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
        FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByTaskName(taskName);
        flowcellDesignationType = responseFactory.makeFlowcellDesignation(flowcellDesignation);
        return flowcellDesignationType;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/findFlowcellDesignationByFlowcellBarcode")
    public FlowcellDesignationType findFlowcellDesignationByFlowcellBarcode(@QueryParam("flowcellBarcode") String flowcellBarcode) throws TException, TZIMSException {
        FlowcellDesignationType flowcellDesignationType;
        FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByFlowcellBarcode(flowcellBarcode);
        flowcellDesignationType = responseFactory.makeFlowcellDesignation(flowcellDesignation);
        return flowcellDesignationType;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/fetchUserIdForBadgeId")
    public String fetchUserIdForBadgeId(@QueryParam("badgeId") String badgeId) throws TException, TZIMSException {
        return thriftService.fetchUserIdForBadgeId(badgeId);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/fetchParentRackContentsForPlate")
    public Map<String, Boolean> fetchParentRackContentsForPlate(@QueryParam("plateBarcode") String plateBarcode) {
        return thriftService.fetchParentRackContentsForPlate(plateBarcode);
    }


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/fetchQpcrForTube")
    public Double fetchQpcrForTube(@QueryParam("tubeBarcode") String tubeBarcode) throws TException, TZIMSException {
        return thriftService.fetchQpcrForTube(tubeBarcode);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/fetchQuantForTube")
    public Double fetchQuantForTube(@QueryParam("tubeBarcode") String tubeBarcode, @QueryParam("quantType") String quantType) throws TException, TZIMSException {
        return thriftService.fetchQuantForTube(tubeBarcode, quantType);
    }
}
