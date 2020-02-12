package org.broadinstitute.gpinformatics.mercury.boundary.lims;

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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 * REST methods used by lab automation machines.
 */
@Path("/limsQuery")
public class LimsQueryResource {

    private static final Log log = LogFactory.getLog(LimsQueryResource.class);

    @Inject
    private LimsQueries limsQueries;

    @Inject
    SequencingTemplateFactory sequencingTemplateFactory;

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

    public LimsQueryResource(LimsQueries limsQueries, SequencingTemplateFactory sequencingTemplateFactory,
            SystemOfRecord systemOfRecord, BSPUserList bspUserList, GenericReagentDao genericReagentDao) {
        this.limsQueries = limsQueries;
        this.sequencingTemplateFactory = sequencingTemplateFactory;
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
        return limsQueries.fetchLibraryDetailsByTubeBarcode(tubeBarcodes, includeWorkRequestDetails);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchConcentrationAndVolumeAndWeightForTubeBarcodes")
    public Map<String,ConcentrationAndVolumeAndWeightType> fetchConcentrationAndVolumeAndWeightForTubeBarcodes(
            @QueryParam("q") List<String> tubeBarcodes,
            @DefaultValue("true") @QueryParam("labMetricsFirst") boolean labMetricsFirst) {
        return limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(tubeBarcodes, labMetricsFirst);
    }

    // TODO round 3: bool areLibrariesAllTheSameType(1:list<string> tubeBarcodes) throws (1:NotFoundException details)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doesLimsRecognizeAllTubes")
    public boolean doesLimsRecognizeAllTubes(@QueryParam("q") List<String> barcodes) {
        return limsQueries.doesLimsRecognizeAllTubes(barcodes);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchMaterialTypesForTubeBarcodes")
    public List<String> fetchMaterialTypesForTubeBarcodes(@QueryParam("q") List<String> tubeBarcodes) {
        // There's nothing to return now that routing to Squid has ceased.
        return Collections.emptyList();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findFlowcellDesignationByTaskName")
    public FlowcellDesignationType findFlowcellDesignationByTaskName(@QueryParam("taskName") String taskName) {
        // There's nothing to return now that routing to Squid has ceased.
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findFlowcellDesignationByFlowcellBarcode")
    public FlowcellDesignationType findFlowcellDesignationByFlowcellBarcode(
            @QueryParam("flowcellBarcode") String flowcellBarcode) {
        // There's nothing to return now that routing to Squid has ceased.
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findFlowcellDesignationByReagentBlockBarcode")
    public FlowcellDesignationType findFlowcellDesignationByReagentBlockBarcode(
            @QueryParam("reagentBlockBarcode") String reagentBlockBarcode) {
        // There's nothing to return now that routing to Squid has ceased.
        return null;
    }

    // TODO round 3: bool checkReceptaclesInTask(1:list<string> tubeBarcodes, 2:string taskName) throws(1:NotFoundException details)

    /**
     * Returns the plate barcodes of the plates that have been transferred directly into the given plate. Returns an
     * empty list if the given plate is not found.
     *
     * @param plateBarcode the barcode of the plate to query
     *
     * @return the immediate plate parents, or an empty list if the given plate isn't found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/findImmediatePlateParents")
    public List<String> findImmediatePlateParents(@QueryParam("plateBarcode") String plateBarcode) {
        return limsQueries.findImmediatePlateParents(plateBarcode);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchLibraryDetailsByLibraryName")
    public List<LibraryDataType> fetchLibraryDetailsByLibraryName(@QueryParam("q") List<String> libraryNames) {
        // There's nothing to return now that routing to Squid has ceased.
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchUnfulfilledDesignations")
    public List<String> fetchUnfulfilledDesignations() {
        // There's nothing to return now that routing to Squid has ceased.
        return Collections.emptyList();
    }

    /**
     * Returns the name of the user.
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
        return sortWellNameKeys(limsQueries.fetchParentRackContentsForPlate(plateBarcode));
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchQpcrForTube")
    public Double fetchQpcrForTube(@QueryParam("tubeBarcode") String tubeBarcode,
                                   @QueryParam("quantType") String quantType,
                                   @DefaultValue("false") @QueryParam("onTubeOnly") boolean onTubeOnly ) {
        return onTubeOnly ? limsQueries.fetchQuantForTube(tubeBarcode, quantType) :
                limsQueries.fetchNearestQuantForTube(tubeBarcode, quantType);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchQpcrForTubeAndType")
    public Double fetchQpcrForTubeAndType(@QueryParam("tubeBarcode") String tubeBarcode,
            @QueryParam("qpcrType")String qpcrType) {
        // There's nothing to return now that routing to Squid has ceased.
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchQuantForTube")
    public Double fetchQuantForTube(@QueryParam("tubeBarcode") String tubeBarcode,
                                    @QueryParam("quantType") String quantType,
                                    @DefaultValue("false") @QueryParam("onTubeOnly") boolean onTubeOnly ) {
        return onTubeOnly ? limsQueries.fetchQuantForTube(tubeBarcode, quantType) :
                limsQueries.fetchNearestQuantForTube(tubeBarcode, quantType);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchSourceTubesForPlate")
    public List<WellAndSourceTubeType> fetchSourceTubesForPlate(@QueryParam("plateBarcode") String plateBarcode) {
        return limsQueries.fetchSourceTubesForPlate(plateBarcode);
    }

    // TODO round 3?: PlateInfo fetchPlateInfo(1:string plateBarcode)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchTransfersForPlate")
    public List<PlateTransferType> fetchTransfersForPlate(@QueryParam("plateBarcode") String plateBarcode,
                                                          @QueryParam("depth") short depth) {
        return limsQueries.fetchTransfersForPlate(plateBarcode, depth);
    }

    // TODO round ?: list<PlateTransfer> fetchTransfersForRack(1:string rackBarcode, 2:list<WellAndSourceTube> positionMap, 3:i16 depth)

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/fetchPoolGroups")
    public List<PoolGroupType> fetchPoolGroups(@QueryParam("q") List<String> tubeBarcodes) {
        // There's nothing to return now that routing to Squid has ceased.
        return Collections.emptyList();
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
