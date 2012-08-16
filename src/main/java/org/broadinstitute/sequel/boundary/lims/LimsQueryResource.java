package org.broadinstitute.sequel.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftService;
import org.broadinstitute.sequel.limsquery.generated.FlowcellDesignationType;
import org.broadinstitute.sequel.limsquery.generated.LibraryDataType;
import org.broadinstitute.sequel.limsquery.generated.PoolGroupType;

import javax.inject.Inject;
import javax.ws.rs.GET;
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchLibraryDetailsByTubeBarcode")
    public List<LibraryDataType> fetchLibraryDetailsByTubeBarcode(@QueryParam("q") List<String> tubeBarcodes, @QueryParam("includeWorkRequestDetails") boolean includeWorkRequestDetails) {
        List<LibraryData> libraryData = thriftService.fetchLibraryDetailsByTubeBarcode(tubeBarcodes, includeWorkRequestDetails);
        List<LibraryDataType> result = new ArrayList<LibraryDataType>();
        for (LibraryData data : libraryData) {
            result.add(responseFactory.makeLibraryData(data));
        }
        return result;
    }

    // TODO round 2: map<string,ConcentrationAndVolume> fetchConcentrationAndVolumeForTubeBarcodes(1:list<string> tubeBarcodes)

    // TODO round 2: bool areLibrariesAllTheSameType(1:list<string> tubeBarcodes) throws (1:NotFoundException details)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doesLimsRecognizeAllTubes")
    public boolean doesLimsRecognizeAllTubes(@QueryParam("q") List<String> barcodes) {
        return thriftService.doesSquidRecognizeAllLibraries(barcodes);
    }

    // TODO round 2: list<string> fetchMaterialTypesForTubeBarcodes(1:list<string> tubeBarcodes)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findFlowcellDesignationByTaskName")
    public FlowcellDesignationType findFlowcellDesignationByTaskName(@QueryParam("taskName") String taskName) {
        FlowcellDesignationType flowcellDesignationType;
        FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByTaskName(taskName);
        flowcellDesignationType = responseFactory.makeFlowcellDesignation(flowcellDesignation);
        return flowcellDesignationType;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findFlowcellDesignationByFlowcellBarcode")
    public FlowcellDesignationType findFlowcellDesignationByFlowcellBarcode(@QueryParam("flowcellBarcode") String flowcellBarcode) {
        FlowcellDesignationType flowcellDesignationType;
        FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByFlowcellBarcode(flowcellBarcode);
        flowcellDesignationType = responseFactory.makeFlowcellDesignation(flowcellDesignation);
        return flowcellDesignationType;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findFlowcellDesignationByReagentBlockBarcode")
    public FlowcellDesignationType findFlowcellDesignationByReagentBlockBarcode(@QueryParam("reagentBlockBarcode") String reagentBlockBarcode) {
        FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByReagentBlockBarcode(reagentBlockBarcode);
        return responseFactory.makeFlowcellDesignation(flowcellDesignation);
    }

    // TODO round 2: bool checkReceptaclesInTask(1:list<string> tubeBarcodes, 2:string taskName) throws(1:NotFoundException details)

    // TODO round 2: list<string> findImmediatePlateParents(1:string plateBarcode)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchLibraryDetailsByLibraryName")
    public List<LibraryDataType> fetchLibraryDetailsByLibraryName(@QueryParam("q") List<String> libraryNames) {
        // TODO: thrift proxy implementation
        return null;
    }

    // TODO round 2: list<string> fetchUnfulfilledDesignations()

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findRelatedDesignationsForAnyTube")
    public List<String> findRelatedDesignationsForAnyTube(@QueryParam("q") List<String> tubeBarcodes) {
        // TODO: thrift proxy implementation
        return null;

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchUserIdForBadgeId")
    public String fetchUserIdForBadgeId(@QueryParam("badgeId") String badgeId) {
        return thriftService.fetchUserIdForBadgeId(badgeId);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchParentRackContentsForPlate")
    public Map<String, Boolean> fetchParentRackContentsForPlate(@QueryParam("plateBarcode") String plateBarcode) {
        return thriftService.fetchParentRackContentsForPlate(plateBarcode);
    }

    // TODO round 2: TZamboniRun fetchSingleLane(1:string runName, 2:i16 laneNumber) throws (1:TZIMSException details)

    // TODO round 2: TZamboniRun fetchRun(1:string runName) throws (1:TZIMSException details)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchQpcrForTube")
    public Double fetchQpcrForTube(@QueryParam("tubeBarcode") String tubeBarcode) {
        return thriftService.fetchQpcrForTube(tubeBarcode);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchQuantForTube")
    public Double fetchQuantForTube(@QueryParam("tubeBarcode") String tubeBarcode, @QueryParam("quantType") String quantType) {
        return thriftService.fetchQuantForTube(tubeBarcode, quantType);
    }

    // TODO round 2: list<WellAndSourceTube> fetchSourceTubesForPlate(1:string plateBarcode)

    // TODO ???: PlateInfo fetchPlateInfo(1:string plateBarcode)

    // TODO round 2: list<PlateTransfer> fetchTransfersForPlate(1:string plateBarcode, 2:i16 depth)

    // TODO round 2: list<PlateTransfer> fetchTransfersForRack(1:string rackBarcode, 2:list<WellAndSourceTube> positionMap, 3:i16 depth)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchPoolGroups")
    public List<PoolGroupType> fetchPoolGroups(@QueryParam("q") List<String> tubeBarcodes) {
        // TODO: thrift proxy implementation
        return null;
    }
}
