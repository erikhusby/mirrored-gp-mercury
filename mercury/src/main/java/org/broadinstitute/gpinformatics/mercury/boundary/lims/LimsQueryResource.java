package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.*;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
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
    private LimsQueryResourceResponseFactory responseFactory;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    public LimsQueryResource() {}

    public LimsQueryResource(ThriftService thriftService, LimsQueryResourceResponseFactory responseFactory, TwoDBarcodedTubeDAO twoDBarcodedTubeDAO, StaticPlateDAO staticPlateDAO) {
        this.thriftService = thriftService;
        this.responseFactory = responseFactory;
        this.twoDBarcodedTubeDAO = twoDBarcodedTubeDAO;
        this.staticPlateDAO = staticPlateDAO;
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findImmediatePlateParents")
    public List<String> findImmediatePlateParents(@QueryParam("plateBarcode") String plateBarcode) {
        return thriftService.findImmediatePlateParents(plateBarcode);
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findRelatedDesignationsForAnyTube")
    public List<String> findRelatedDesignationsForAnyTube(@QueryParam("q") List<String> tubeBarcodes) {
        // TODO round 2: thrift proxy implementation
        return null;

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchUserIdForBadgeId")
    public String fetchUserIdForBadgeId(@QueryParam("badgeId") String badgeId) {
        return thriftService.fetchUserIdForBadgeId(badgeId);
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
        Map<String, Boolean> map;
        try {
            map = thriftService.fetchParentRackContentsForPlate(plateBarcode);
        } catch (RuntimeException e) {
            map = null;
        }

        Map<String, Boolean> mercuryMap = null;
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
        if (plate != null) {
            mercuryMap = new HashMap<String, Boolean>();
            // TODO
        }

        if (map == null && mercuryMap == null) {
            throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
        }
        return map != null ? map : mercuryMap;
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
        List<WellAndSourceTubeType> wellAndSourceTubeTypes = new ArrayList<WellAndSourceTubeType>();
        List<WellAndSourceTube> wellAndSourceTubes = thriftService.fetchSourceTubesForPlate(plateBarcode);
        for (WellAndSourceTube wellAndSourceTube : wellAndSourceTubes) {
            wellAndSourceTubeTypes.add(responseFactory.makeWellAndSourceTube(wellAndSourceTube));
        }
        return wellAndSourceTubeTypes;
    }

    // TODO round 3?: PlateInfo fetchPlateInfo(1:string plateBarcode)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchTransfersForPlate")
    public List<PlateTransferType> fetchTransfersForPlate(@QueryParam("plateBarcode") String plateBarcode, @QueryParam("depth") short depth) {
        List<PlateTransferType> plateTransferTypes = new ArrayList<PlateTransferType>();
        List<PlateTransfer> plateTransfers = thriftService.fetchTransfersForPlate(plateBarcode, depth);
        for (PlateTransfer plateTransfer : plateTransfers) {
            plateTransferTypes.add(responseFactory.makePlateTransfer(plateTransfer));
        }
        return plateTransferTypes;
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
