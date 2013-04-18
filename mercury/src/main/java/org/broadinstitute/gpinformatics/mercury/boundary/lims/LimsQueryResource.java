package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.*;

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

    @Inject
    private Log log;

    @Inject
    private ThriftService thriftService;

    @Inject
    private LimsQueries limsQueries;

    @Inject
    private LimsQueryResourceResponseFactory responseFactory;

    @Inject
    private MercuryOrSquidRouter mercuryOrSquidRouter;

    @Inject
    private BSPUserList bspUserList;

    public LimsQueryResource() {}

    public LimsQueryResource(ThriftService thriftService, LimsQueries limsQueries,
                             LimsQueryResourceResponseFactory responseFactory,
                             MercuryOrSquidRouter mercuryOrSquidRouter, BSPUserList bspUserList) {
        this.thriftService = thriftService;
        this.limsQueries = limsQueries;
        this.responseFactory = responseFactory;
        this.mercuryOrSquidRouter = mercuryOrSquidRouter;
        this.bspUserList = bspUserList;
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

    // TODO round 3: map<string,ConcentrationAndVolume> fetchConcentrationAndVolumeForTubeBarcodes(1:list<string> tubeBarcodes)

    // TODO round 3: bool areLibrariesAllTheSameType(1:list<string> tubeBarcodes) throws (1:NotFoundException details)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doesLimsRecognizeAllTubes")
    public boolean doesLimsRecognizeAllTubes(@QueryParam("q") List<String> barcodes) {
        boolean doesSquidRecognizeAllTubes = thriftService.doesSquidRecognizeAllLibraries(barcodes);
//        boolean doesSequelRecognizeAllTubes = twoDBarcodedTubeDAO.findByBarcodes(barcodes).size() == barcodes.size();
        return doesSquidRecognizeAllTubes; // || doesSequelRecognizeAllTubes;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchMaterialTypesForTubeBarcodes")
    public List<String> fetchMaterialTypesForTubeBarcodes(@QueryParam("q") List<String> tubeBarcodes) {
        return thriftService.fetchMaterialTypesForTubeBarcodes(tubeBarcodes);
    }

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

    // TODO round 3: bool checkReceptaclesInTask(1:list<string> tubeBarcodes, 2:string taskName) throws(1:NotFoundException details)

    /**
     * Returns the plate barcodes of the plates that have been transferred directly into the given plate. Returns an
     * empty list if the given plate is not found in either Mercury or Squid.
     *
     * @param plateBarcode    the barcode of the plate to query
     * @return the immediate plate parents, or an empty list if the given plate isn't found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findImmediatePlateParents")
    public List<String> findImmediatePlateParents(@QueryParam("plateBarcode") String plateBarcode) {
        switch (mercuryOrSquidRouter.routeForVessel(plateBarcode)) {
            case MERCURY:
                return limsQueries.findImmediatePlateParents(plateBarcode);
            case SQUID:
            case BOTH:
                return thriftService.findImmediatePlateParents(plateBarcode);
            default:
                throw new RuntimeException("Unable to route findImmediatePlateParents for plate: " + plateBarcode);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchLibraryDetailsByLibraryName")
    public List<LibraryDataType> fetchLibraryDetailsByLibraryName(@QueryParam("q") List<String> libraryNames) {

        if (libraryNames == null || libraryNames.isEmpty()) {
            //return null || error
            return null;
        }

        LibraryDataType libraryDataType = null;
        List<LibraryDataType> libraryDataTypeList = new ArrayList<LibraryDataType>();
        List<LibraryData> libraryDataList = thriftService.fetchLibraryDetailsByLibraryName(libraryNames);
        if (libraryDataList == null || libraryDataList.isEmpty()) {
            return  null;
        }

        for (LibraryData libraryData : libraryDataList) {
            libraryDataType = responseFactory.makeLibraryData(libraryData);
            libraryDataTypeList.add(libraryDataType);
        }

        return libraryDataTypeList;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchUnfulfilledDesignations")
    public List<String> fetchUnfulfilledDesignations() {
        return thriftService.fetchUnfulfilledDesignations();
    }

    /**
     * Contrary to the path of this service, the data provided here is no longer provided by a Thrift Query.  This data
     * now comes from the BspUserList
     * @param badgeId badge ID of a broad user
     * @return Broad User ID that corresponds to the Badge.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchUserIdForBadgeId")
    public String fetchUserIdForBadgeId(@QueryParam("badgeId") String badgeId) {

        BspUser foundUser = bspUserList.getByBadgeId(badgeId);
        if (foundUser != null) {
            return foundUser.getUsername();
        } else {
            throw new RuntimeException("User not found for badge ID: " + badgeId);
        }
    }

    /**
     * Returns a map of well position to boolean indicating whether or not the
     * well contains any material that was transferred from an upstream tube
     * rack.
     *
     * @param plateBarcode    the plate barcode
     * @return map of well position to well non-empty status
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchParentRackContentsForPlate")
    public Map<String, Boolean> fetchParentRackContentsForPlate(@QueryParam("plateBarcode") String plateBarcode) {
        switch (mercuryOrSquidRouter.routeForVessel(plateBarcode)) {
            case MERCURY:
                return limsQueries.fetchParentRackContentsForPlate(plateBarcode);
            case SQUID:
            case BOTH:
                return thriftService.fetchParentRackContentsForPlate(plateBarcode);
            default:
                throw new RuntimeException("Unable to route fetchParentRackContentsForPlate for plate: " + plateBarcode);
        }
    }

    // TODO round 3: TZamboniRun fetchSingleLane(1:string runName, 2:i16 laneNumber) throws (1:TZIMSException details)

    // TODO round 3: TZamboniRun fetchRun(1:string runName) throws (1:TZIMSException details)

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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchSourceTubesForPlate")
    public List<WellAndSourceTubeType> fetchSourceTubesForPlate(@QueryParam("plateBarcode") String plateBarcode) {
        switch (mercuryOrSquidRouter.routeForVessel(plateBarcode)) {
            case MERCURY:
                return limsQueries.fetchSourceTubesForPlate(plateBarcode);
            case SQUID:
            case BOTH:
                List<WellAndSourceTubeType> wellAndSourceTubeTypes = new ArrayList<WellAndSourceTubeType>();
                List<WellAndSourceTube> wellAndSourceTubes = thriftService.fetchSourceTubesForPlate(plateBarcode);
                for (WellAndSourceTube wellAndSourceTube : wellAndSourceTubes) {
                    wellAndSourceTubeTypes.add(responseFactory.makeWellAndSourceTube(wellAndSourceTube));
                }
                return wellAndSourceTubeTypes;
            default:
                throw new RuntimeException("Unable to route fetchSourceTubesForPlate for plate: " + plateBarcode);
        }
    }

    // TODO round 3?: PlateInfo fetchPlateInfo(1:string plateBarcode)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchTransfersForPlate")
    public List<PlateTransferType> fetchTransfersForPlate(@QueryParam("plateBarcode") String plateBarcode,
                                                          @QueryParam("depth") short depth) {
        switch (mercuryOrSquidRouter.routeForVessel(plateBarcode)) {
            case MERCURY:
                return limsQueries.fetchTransfersForPlate(plateBarcode, depth);
            case SQUID:
            case BOTH:
                List<PlateTransferType> plateTransferTypes = new ArrayList<PlateTransferType>();
                List<PlateTransfer> plateTransfers = thriftService.fetchTransfersForPlate(plateBarcode, depth);
                for (PlateTransfer plateTransfer : plateTransfers) {
                    plateTransferTypes.add(responseFactory.makePlateTransfer(plateTransfer));
                }
                return plateTransferTypes;
            default:
                throw new RuntimeException("Unable to route fetchTransfersForPlate for plate: " + plateBarcode);
        }
    }

    // TODO round ?: list<PlateTransfer> fetchTransfersForRack(1:string rackBarcode, 2:list<WellAndSourceTube> positionMap, 3:i16 depth)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchPoolGroups")
    public List<PoolGroupType> fetchPoolGroups(@QueryParam("q") List<String> tubeBarcodes) {
        List<PoolGroupType> poolGroupTypes = new ArrayList<PoolGroupType>();
        List<PoolGroup> poolGroups = thriftService.fetchPoolGroups(tubeBarcodes);
        for (PoolGroup poolGroup : poolGroups) {
            poolGroupTypes.add(responseFactory.makePoolGroup(poolGroup));
        }
        return poolGroupTypes;
    }
}
