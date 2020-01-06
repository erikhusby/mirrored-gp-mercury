package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.ConcentrationAndVolume;
import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.PlateTransfer;
import edu.mit.broad.prodinfo.thrift.lims.PoolGroup;
import edu.mit.broad.prodinfo.thrift.lims.WellAndSourceTube;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.Strings;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ConcentrationAndVolumeAndWeightType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.FlowcellDesignationType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LibraryDataType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PoolGroupType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ProductInfosType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReagentDesignType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ValidationErrorType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.WellAndSourceTubeType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.WorklowValidationErrorType;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 * @author breilly
 */
@Path("/limsQuery")
public class LimsQueryResource {

    private static final Log log = LogFactory.getLog(LimsQueryResource.class);

    @Inject
    private ThriftService thriftService;

    @Inject
    private LimsQueries limsQueries;

    @Inject
    SequencingTemplateFactory sequencingTemplateFactory;

    @Inject
    private LimsQueryResourceResponseFactory responseFactory;

    @Inject
    private SystemOfRecord systemOfRecord;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private GenericReagentDao genericReagentDao;

    @Inject
    private WorkflowValidator workflowValidator;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    public LimsQueryResource() {
    }

    public LimsQueryResource(ThriftService thriftService, LimsQueries limsQueries,
                             SequencingTemplateFactory sequencingTemplateFactory,
                             LimsQueryResourceResponseFactory responseFactory,
                             SystemOfRecord systemOfRecord, BSPUserList bspUserList,
                             GenericReagentDao genericReagentDao) {
        this.thriftService = thriftService;
        this.limsQueries = limsQueries;
        this.sequencingTemplateFactory = sequencingTemplateFactory;
        this.responseFactory = responseFactory;
        this.systemOfRecord = systemOfRecord;
        this.bspUserList = bspUserList;
        this.genericReagentDao = genericReagentDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchLibraryDetailsByTubeBarcode")
    public List<LibraryDataType> fetchLibraryDetailsByTubeBarcode(
            @QueryParam("q") List<String> tubeBarcodes,
            @QueryParam("includeWorkRequestDetails") boolean includeWorkRequestDetails) {
        switch (systemOfRecord.getSystemOfRecord(tubeBarcodes)) {
        case MERCURY:
            return limsQueries.fetchLibraryDetailsByTubeBarcode(tubeBarcodes, includeWorkRequestDetails);
        case SQUID:
            List<LibraryData> libraryData = thriftService.fetchLibraryDetailsByTubeBarcode(tubeBarcodes, includeWorkRequestDetails);
            List<LibraryDataType> result = new ArrayList<>();
            for (LibraryData data : libraryData) {
                result.add(responseFactory.makeLibraryData(data));
            }
            return result;
        default:
            throw new RuntimeException("Unable to route fetchLibraryDetailsByTubeBarcode for tubes: " + tubeBarcodes);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchConcentrationAndVolumeAndWeightForTubeBarcodes")
    public Map<String,ConcentrationAndVolumeAndWeightType> fetchConcentrationAndVolumeAndWeightForTubeBarcodes(
            @QueryParam("q") List<String> tubeBarcodes,
            @DefaultValue("true") @QueryParam("labMetricsFirst") boolean labMetricsFirst) {
        switch (systemOfRecord.getSystemOfRecord(tubeBarcodes)) {
        case MERCURY:
            return limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(tubeBarcodes, labMetricsFirst);
        case SQUID:
            Map<String, ConcentrationAndVolume> concentrationAndVolumeMap =
                    thriftService.fetchConcentrationAndVolumeForTubeBarcodes(tubeBarcodes);
            Map<String, ConcentrationAndVolumeAndWeightType> result = new HashMap<>();
            for (Map.Entry<String, ConcentrationAndVolume> data : concentrationAndVolumeMap.entrySet()) {
                result.put(data.getKey(), responseFactory.makeConcentrationAndVolumeAndWeight(data.getValue()));
            }
            return result;
        default:
            throw new RuntimeException("Unable to route fetchConcentrationAndVolumeAndWeightForTubeBarcodes for tubes: " + tubeBarcodes);
        }
    }

    // TODO round 3: bool areLibrariesAllTheSameType(1:list<string> tubeBarcodes) throws (1:NotFoundException details)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doesLimsRecognizeAllTubes")
    public boolean doesLimsRecognizeAllTubes(@QueryParam("q") List<String> barcodes) {
        switch (systemOfRecord.getSystemOfRecord(barcodes)) {
        case MERCURY:
            return limsQueries.doesLimsRecognizeAllTubes(barcodes);
        case SQUID:
            return thriftService.doesSquidRecognizeAllLibraries(barcodes);
        default:
            throw new RuntimeException("Unable to route doesLimsRecognizeAllTubes for tubes: " + barcodes);
        }
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
    public FlowcellDesignationType findFlowcellDesignationByFlowcellBarcode(
            @QueryParam("flowcellBarcode") String flowcellBarcode) {
        FlowcellDesignationType flowcellDesignationType;
        FlowcellDesignation flowcellDesignation =
                thriftService.findFlowcellDesignationByFlowcellBarcode(flowcellBarcode);
        flowcellDesignationType = responseFactory.makeFlowcellDesignation(flowcellDesignation);
        return flowcellDesignationType;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findFlowcellDesignationByReagentBlockBarcode")
    public FlowcellDesignationType findFlowcellDesignationByReagentBlockBarcode(
            @QueryParam("reagentBlockBarcode") String reagentBlockBarcode) {
        FlowcellDesignation flowcellDesignation =
                thriftService.findFlowcellDesignationByReagentBlockBarcode(reagentBlockBarcode);
        return responseFactory.makeFlowcellDesignation(flowcellDesignation);
    }

    // TODO round 3: bool checkReceptaclesInTask(1:list<string> tubeBarcodes, 2:string taskName) throws(1:NotFoundException details)

    /**
     * Returns the plate barcodes of the plates that have been transferred directly into the given plate. Returns an
     * empty list if the given plate is not found in either Mercury or Squid.
     *
     * @param plateBarcode the barcode of the plate to query
     *
     * @return the immediate plate parents, or an empty list if the given plate isn't found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findImmediatePlateParents")
    public List<String> findImmediatePlateParents(@QueryParam("plateBarcode") String plateBarcode) {
        switch (systemOfRecord.getSystemOfRecord(plateBarcode)) {
            case MERCURY:
                return limsQueries.findImmediatePlateParents(plateBarcode);
            case SQUID:
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
        List<LibraryDataType> libraryDataTypeList = new ArrayList<>();
        List<LibraryData> libraryDataList = thriftService.fetchLibraryDetailsByLibraryName(libraryNames);
        if (libraryDataList == null || libraryDataList.isEmpty()) {
            return null;
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
     *
     * @param badgeId badge ID of a broad user
     *
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
     * Return a copy of the input Map that will iterate its entries with the well name keys in row major order,
     * e.g. A01, A02,..., H12.
     */
    private static Map<String, Boolean> sortWellNameKeys(@Nonnull Map<String, Boolean> wellNamesToBooleans) {
        return new TreeMap<>(wellNamesToBooleans);
    }

    /**
     * Returns a map of well position to boolean indicating whether or not the
     * well contains any material that was transferred from an upstream tube
     * rack.
     *
     * @param plateBarcode the plate barcode
     *
     * @return map of well position to well non-empty status
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchParentRackContentsForPlate")
    public Map<String, Boolean> fetchParentRackContentsForPlate(@QueryParam("plateBarcode") String plateBarcode) {
        switch (systemOfRecord.getSystemOfRecord(plateBarcode)) {
            case MERCURY:
                return sortWellNameKeys(limsQueries.fetchParentRackContentsForPlate(plateBarcode));
            case SQUID:
                return sortWellNameKeys(thriftService.fetchParentRackContentsForPlate(plateBarcode));
            default:
                throw new RuntimeException("Unable to route fetchParentRackContentsForPlate for plate: " + plateBarcode);
        }
    }

    /**
     * Build an initial sample existence Map with all well positions from A01 through H12 and all false values.
     */
    private Map<String, Boolean> buildInitialSampleExistenceMap() {
        // Initially fill all positions in the Map with false.  Use a TreeMap to keep keys in sorted order for
        // readability.
        Map<String, Boolean> map = new TreeMap<>();
        for (char r = 'A'; r <= 'H'; r++) {
            for (int c = 1; c <= 12; c++) {
                map.put(String.format("%c%02d", r, c), false);
            }
        }
        return map;
    }

    /**
     * Utility method to build a response for the specified status and map data.
     */
    private Response buildResponse(@Nonnull Response.Status status, @Nonnull Map<String, Boolean> map) {
        return Response.status(status).entity(map).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * Return a "sample existence" Map of all well position keys from A01 through H12 to a Boolean indicating whether
     * there is a sample in the parent rack at that position.  This is currently more generally useful than the
     * {@link LimsQueryResource#fetchParentRackContentsForPlate(String)} method which only works for Varioskan data
     * generation when run for Exome Express sets.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/parentRackContents")
    public Response parentRackContents(@QueryParam("plateBarcode") @Nonnull String plateBarcode) {

        LabVessel labVessel = labVesselDao.findByIdentifier(plateBarcode);
        // If the barcode is not recognized labVessel will be null.
        if (labVessel == null) {
            // EMPTY_MAP does not have type parameters.
            //noinspection unchecked
            return buildResponse(Response.Status.BAD_REQUEST, Collections.<String, Boolean>emptyMap());
        }

        LabVessel sourceRack = labVessel.getContainerRole().getSourceRack();
        if (sourceRack == null) {
            // EMPTY_MAP does not have type parameters.
            //noinspection unchecked
            return buildResponse(Response.Status.BAD_REQUEST, Collections.<String, Boolean>emptyMap());
        }

        Map<String, Boolean> map = buildInitialSampleExistenceMap();
        // Set any positions where this rack has samples to be true in the result Map.
        for (VesselPosition vesselPosition : sourceRack.getContainerRole().getMapPositionToVessel().keySet()) {
            map.put(vesselPosition.name(), true);
        }

        return buildResponse(Response.Status.OK, map);
    }

    // TODO round 3: TZamboniRun fetchSingleLane(1:string runName, 2:i16 laneNumber) throws (1:TZIMSException details)

    // TODO round 3: TZamboniRun fetchRun(1:string runName) throws (1:TZIMSException details)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchQpcrForTube")
    public Double fetchQpcrForTube(@QueryParam("tubeBarcode") String tubeBarcode,
                                   @QueryParam("quantType") String quantType,
                                   @DefaultValue("false") @QueryParam("onTubeOnly") boolean onTubeOnly ) {
        switch (systemOfRecord.getSystemOfRecord(tubeBarcode)) {
        case MERCURY:
            if( !onTubeOnly ) {
                return limsQueries.fetchNearestQuantForTube(tubeBarcode, quantType);
            } else {
                return limsQueries.fetchQuantForTube(tubeBarcode, quantType);
            }
        case SQUID:
            return thriftService.fetchQpcrForTube(tubeBarcode);
        default:
            throw new RuntimeException(
                    "Tube or quant not found for barcode: " + tubeBarcode + ", quant type: " + LabMetric.MetricType
                            .ECO_QPCR.getDisplayName());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchQpcrForTubeAndType")
    public Double fetchQpcrForTubeAndType(@QueryParam("tubeBarcode") String tubeBarcode,
            @QueryParam("qpcrType")String qpcrType) {
        switch (systemOfRecord.getSystemOfRecord(tubeBarcode)) {
        case MERCURY:
            throw new RuntimeException("Not implemented in Mercury");
        case SQUID:
            return thriftService.fetchQpcrForTubeAndType(tubeBarcode, qpcrType);
        default:
            throw new RuntimeException(
                    "Tube or quant not found for barcode: " + tubeBarcode + ", quant type: " + LabMetric.MetricType
                            .ECO_QPCR.getDisplayName());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchQuantForTube")
    public Double fetchQuantForTube(@QueryParam("tubeBarcode") String tubeBarcode,
                                    @QueryParam("quantType") String quantType,
                                    @DefaultValue("false") @QueryParam("onTubeOnly") boolean onTubeOnly ) {
        switch (systemOfRecord.getSystemOfRecord(tubeBarcode)) {
        case MERCURY:
            if( !onTubeOnly ) {
                return limsQueries.fetchNearestQuantForTube(tubeBarcode, quantType);
            } else {
                return limsQueries.fetchQuantForTube(tubeBarcode, quantType);
            }
        case SQUID:
            return thriftService.fetchQuantForTube(tubeBarcode, quantType);
        default:
            throw new RuntimeException(
                    "Tube or quant not found for barcode: " + tubeBarcode + ", quant type: " + quantType);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchSourceTubesForPlate")
    public List<WellAndSourceTubeType> fetchSourceTubesForPlate(@QueryParam("plateBarcode") String plateBarcode) {
        switch (systemOfRecord.getSystemOfRecord(plateBarcode)) {
            case MERCURY:
                return limsQueries.fetchSourceTubesForPlate(plateBarcode);
            case SQUID:
                List<WellAndSourceTubeType> wellAndSourceTubeTypes = new ArrayList<>();
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
        switch (systemOfRecord.getSystemOfRecord(plateBarcode)) {
            case MERCURY:
                return limsQueries.fetchTransfersForPlate(plateBarcode, depth);
            case SQUID:
                List<PlateTransferType> plateTransferTypes = new ArrayList<>();
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
        List<PoolGroupType> poolGroupTypes = new ArrayList<>();
        List<PoolGroup> poolGroups = thriftService.fetchPoolGroups(tubeBarcodes);
        for (PoolGroup poolGroup : poolGroups) {
            poolGroupTypes.add(responseFactory.makePoolGroup(poolGroup));
        }
        return poolGroupTypes;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchIlluminaSeqTemplate")
    public SequencingTemplateType fetchIlluminaSeqTemplate(@QueryParam("id") String id,
                                                           @QueryParam("idType") SequencingTemplateFactory.QueryVesselType queryVesselType,
                                                           @QueryParam("isPoolTest") boolean isPoolTest) {
        return sequencingTemplateFactory.fetchSequencingTemplate(id, queryVesselType, isPoolTest);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getSystemOfRecordForVesselBarcodes")
    public SystemOfRecord.System getSystemOfRecordForVesselBarcodes(@QueryParam("q") List<String> barcodes) {
        return systemOfRecord.getSystemOfRecord(barcodes);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/routeForVesselBarcodes")
    public SystemOfRecord.System routeForVesselBarcodes(@QueryParam("q") List<String> barcodes) {
        return SystemOfRecord.System.MERCURY;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findAllReagentsListedInEventWithReagent")
    public Set<ReagentType> findAllReagentsListedInEventWithReagent(@QueryParam("name") String name,
                                                        @QueryParam("lot") String lot,
                                                        @QueryParam("expiration") String expiration) {
        if (name == null || lot == null || expiration == null) {
            throw new RuntimeException("name, lot, and exp are all required query parameters");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Set<ReagentType> reagentTypes = new HashSet<>();
        try {
            Date expirationDate = sdf.parse(expiration);
            GenericReagent genericReagent =
                    genericReagentDao.findByReagentNameLotExpiration(name, lot, expirationDate);
            if (genericReagent != null) {
                Set<LabEventReagent> labEventReagents = genericReagent.getLabEventReagents();
                if (labEventReagents != null && !labEventReagents.isEmpty()) {
                    LabEventReagent labEventReagent = labEventReagents.iterator().next();
                    LabEvent labEvent = labEventReagent.getLabEvent();
                    for (Reagent reagent : labEvent.getReagents()) {
                        ReagentType reagentType = new ReagentType();
                        reagentType.setBarcode(reagent.getLot());
                        reagentType.setExpiration(reagent.getExpiration());
                        reagentType.setKitType(reagent.getName());
                        reagentTypes.add(reagentType);
                    }
                }
            } else {
                throw new RuntimeException(
                        "Reagent not found for name: " + name + ", lot: " + lot + ", and expiration: " + expiration);
            }
            return reagentTypes;
        } catch (ParseException e) {
            throw new RuntimeException("Expiration string must be in the format of yyyy-MM-dd");
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/validateWorkflow")
    public WorklowValidationErrorType validateWorkflow(
            @QueryParam("q") List<String> vesselBarcodes, @QueryParam("nextEventTypeName") String nextEventTypeName) {
        if (vesselBarcodes == null || vesselBarcodes.isEmpty() || nextEventTypeName == null) {
            throw new RuntimeException("vessel barcodes ('q') and nextEventTypeName are required query parameters.");
        }
        Map<String, LabVessel> mapBarcodetoVessel = labVesselDao.findByBarcodes(vesselBarcodes);
        Set<String> unknownLabVesselBarcodes = new HashSet<>();
        Set<String> unacceptableLabVesselBarcodes = new HashSet<>();
        for (Map.Entry<String, LabVessel> entry: mapBarcodetoVessel.entrySet()) {
            if (entry.getValue() == null) {
                unknownLabVesselBarcodes.add(entry.getKey());
            } else if (OrmUtil.proxySafeIsInstance(entry.getValue(), RackOfTubes.class)) {
                unacceptableLabVesselBarcodes.add(entry.getKey());
            }
        }
        if (!unknownLabVesselBarcodes.isEmpty()) {
            throw new RuntimeException("Failed to find lab vessels with barcodes: " + unknownLabVesselBarcodes);
        }
        if (!unacceptableLabVesselBarcodes.isEmpty()) {
            throw new RuntimeException("Incompatible vessel types: " + unacceptableLabVesselBarcodes);
        }
        List<WorkflowValidator.WorkflowValidationError> workflowValidationErrors =
                workflowValidator.validateWorkflow(mapBarcodetoVessel.values(), nextEventTypeName);
        WorklowValidationErrorType errorType = new WorklowValidationErrorType();
        errorType.setHasErrors(!workflowValidationErrors.isEmpty());
        if (errorType.isHasErrors()) {
            for (WorkflowValidator.WorkflowValidationError workflowValidationError: workflowValidationErrors) {
                for (ProductWorkflowDefVersion.ValidationError validationError: workflowValidationError.getErrors()) {
                    ValidationErrorType validationErrorType = new ValidationErrorType();
                    validationErrorType.setMessage(validationError.getMessage());
                    validationErrorType.getActualEventTypes().addAll(validationError.getActualEventNames());
                    validationErrorType.getExpectedEventTypes().addAll(validationError.getExpectedEventNames());
                    errorType.getValidationErrors().add(validationErrorType);
                }
            }

        }
        return errorType;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchExpectedReagentDesignsForTubeBarcodes")
    public List<ReagentDesignType> fetchExpectedReagentDesignsForTubeBarcodes(@QueryParam("q") List<String> tubeBarcodes) {
        return limsQueries.fetchExpectedReagentDesignsForTubeBarcodes(tubeBarcodes);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchProductInfoForTubeBarcodes")
    public List<ProductInfosType> fetchProductInfoForTubeBarcodes(@QueryParam("q") List<String> tubeBarcodes) {
        return limsQueries.fetchProductInfoForTubeBarcodes(tubeBarcodes);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/verifyChipTypes")
    public boolean verifyChipTypes(@QueryParam("plateBarcode") String plateBarcode, @QueryParam("chip") List<String> chips) {

        List<String> fileOutput;
        String fileChipType;

        LabVessel labVessel = labVesselDao.findByIdentifier(plateBarcode);
        if (labVessel == null) {
            throw new ResourceException("Failed to find Sample Plate" + plateBarcode,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        GenotypingChip chipType = null;
        for (SampleInstanceV2 sampleInstanceV2: labVessel.getSampleInstancesV2()) {
            ProductOrderSample pdoSampleForSingleBucket = sampleInstanceV2.getProductOrderSampleForSingleBucket();
            if (pdoSampleForSingleBucket == null) {
                for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                    chipType = findChipType(productOrderSample.getProductOrder(), new Date());
                    // this may be NA12878, so continue looping, and hope to find a non-null single bucket
                }
            } else {
                chipType = findChipType(pdoSampleForSingleBucket.getProductOrder(), new Date());
                if (chipType != null) {
                    break;
                }
            }
        }

        if (chipType == null) {
            throw new ResourceException("Found no array product orders ", Response.Status.INTERNAL_SERVER_ERROR);
        }
        String PDOChipType = chipType.getChipName();
        List<String> PDOChipTypeList = parseChipName(PDOChipType);

        String dataPathStr = infiniumStarterConfig.getDecodeDataPath();
        File dataPath = new File(dataPathStr);

        for (String chip: chips) {
            String fileName = String.format("%s_R01C01_01.dmap.gz", chip);
            File targetFolder = new File(dataPath, chip);
            File targetFile = new File(targetFolder, fileName);
            if (!targetFile.exists()){
                StringBuilder retryFileName = new StringBuilder(fileName).deleteCharAt(fileName.length() - 10);
                targetFile = new File(targetFolder, retryFileName.toString());
                if (!targetFile.exists()){
                    throw new ResourceException("Failed to find target DMAP File " + targetFile,
                            Response.Status.INTERNAL_SERVER_ERROR);
                }
            }

            GZIPInputStream DMAP = null;
            try {
                DMAP = createReader(targetFile, "Cp1252");
            }
            catch (IOException e) {
                log.error("Failed to unzip file", e);
            }
            finally {
                if (DMAP == null) {
                    throw new ResourceException("Failed to unzip DMAP File " + targetFile,
                            Response.Status.INTERNAL_SERVER_ERROR);
                }
            }

            fileOutput = Strings.process(DMAP);
            if (fileOutput == null){
                throw new ResourceException("Failed to parse DMAP File " + targetFile,
                        Response.Status.INTERNAL_SERVER_ERROR);
            } else if (fileOutput.size() < 4){
                throw new ResourceException("Unexpected DMAP File output size " + fileOutput.size(),
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            fileChipType = fileOutput.get(4);

            for (String PDOKeyword:PDOChipTypeList){
                if (PDOKeyword.length() > 3){
                    if (fileChipType.contains(PDOKeyword)){
                        return true;
                    }
                }
            }

        }
        return false;
    }

    private GenotypingChip findChipType(ProductOrder productOrder, Date effectiveDate) {
        GenotypingChip chip = null;
        Pair<String, String> chipFamilyAndName = productEjb.getGenotypingChip(productOrder,
                effectiveDate);
        if (chipFamilyAndName.getLeft() != null && chipFamilyAndName.getRight() != null) {
            chip = attributeArchetypeDao.findGenotypingChip(chipFamilyAndName.getLeft(),
                    chipFamilyAndName.getRight());
            if (chip == null) {
                throw new ResourceException("Chip " + chipFamilyAndName.getRight() + " is not configured",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return chip;
    }

    private static GZIPInputStream createReader(File f, String encoding) throws IOException {

        try {
            InputStream in = new FileInputStream(f);
            if (f.getName ().endsWith (".gz")) {
                return new GZIPInputStream(in, 10240);
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Missing encoding "+encoding, e);
        }
        return null;
    }

    private List<String> parseChipName(String name){

        List<String> phrases;
        if (name.contains("-")||name.contains("_")){
            phrases = Arrays.asList(name.split("\\s*_\\s*|\\s*-\\s*"));
        }
        else {
            phrases = Collections.singletonList(name);
        }
        return phrases;
    }

    public void setInfiniumStarterConfig(InfiniumStarterConfig infiniumStarterConfig) {
        this.infiniumStarterConfig = infiniumStarterConfig;
    }
}
