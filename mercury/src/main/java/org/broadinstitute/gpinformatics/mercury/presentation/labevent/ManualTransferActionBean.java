package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.decoder.BarcodeDecoderRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationSetupEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.barcode.generated.DecodeResponse;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.DBSPuncherFileParser;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LimsFileType;
import org.broadinstitute.gpinformatics.mercury.control.vessel.QiagenRackFileParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A Stripes Action Bean to record manual transfers.
 */
@UrlBinding(ManualTransferActionBean.ACTION_BEAN_URL)
public class ManualTransferActionBean extends RackScanActionBean {
    private static final Log log = LogFactory.getLog(ManualTransferActionBean.class);

    public static final String MANUAL_TRANSFER_PAGE = "/labevent/manual_transfer.jsp";
    public static final String CHOOSE_EVENT_TYPE_ACTION = "chooseEventType";
    public static final String TRANSFER_ACTION = "transfer";
    public static final String FETCH_EXISTING_ACTION = "fetchExisting";
    public static final String CLEAR_CONNECTIONS_ACTION = "ClearConnectionsButton";
    public static final String ACTION_BEAN_URL = "/labevent/manualtransfer.action";
    public static final String PAGE_TITLE = "Manual Transfers";
    public static final String RACK_SCAN_EVENT = "rackScan";
    public static final String PARSE_LIMS_FILE_ACTION = "parseLimsFile";
    public static final String SKIP_LIMS_FILE_ACTION = "skipLimsFile";
    public static final String DECODE_IMAGE_ACTION = "decodeImage";
    public static final String BAD_STATION_NAME = "None";
    private final String syntheticBarcode = String.valueOf(System.currentTimeMillis());

    /** Parameter from batch workflow page. */
    private String workflowProcessName;
    /** Parameter from batch workflow page. */
    private String workflowStepName;
    /** Parameter from batch workflow page. */
    private Date workflowEffectiveDate;
    /** Parameter from batch workflow page. */
    private String batchName;
    /** Parameter from batch workflow page, allows return to same link. */
    private Integer anchorIndex;
    /** Loaded based on parameters. */
    private WorkflowStepDef workflowStepDef;

    /** POSTed from the form. */
    private List<StationEventType> stationEvents = new ArrayList<>();
    /** POSTed from the form, for rack scan. */
    private Integer scanIndex;
    /** POSTed from the form, for rack scan. */
    private Boolean scanSource;
    /** Set in the init method, from a POSTed parameter. */
    private LabEventType labEventType;
    /** Set in the init method, from workflowStepDef or labEventType. */
    private LabEventType.ManualTransferDetails manualTransferDetails;
    /** Makes unique the synthetic barcodes of racks. */
    private int anonymousRackDisambiguator = 1;
    /** Persist the validation event to prevent invalid connection warnings for plate and strip-tube cherry pick events */
    private boolean isValidation = false;

    private FileBean limsUploadFile;

    private LimsFileType limsFileType;

    private boolean isParseLimsFile;

    private boolean isUseWebCam;

    private Map<VesselPosition, Boolean> mapPositionToDepleteFlag;

    private Map<VesselPosition, Boolean> mapPositionToBackupFlag;

    private Map<Integer, Boolean> depleteAll;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private WorkflowConfig workflowConfig;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private BarcodeDecoderRestClient barcodeDecoderRestClient;

    private String imageFile;
    private String eventClass;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    /**
     * Create StationEvent subclasses, so Stripes can bind form fields to correct structure.
     */
    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        String eventType = getContext().getRequest().getParameter("stationEvents[0].eventType");
        if (eventType != null) {
            labEventType = LabEventType.getByName(eventType);
            String workflowEffectiveDateLocal = getContext().getRequest().getParameter("workflowEffectiveDate");
            if (!StringUtils.isEmpty(workflowEffectiveDateLocal)) {
                workflowStepDef = loadWorkflowStepDef(new Date(workflowEffectiveDateLocal),
                        workflowConfig, getContext().getRequest().getParameter("workflowProcessName"),
                        getContext().getRequest().getParameter("workflowStepName"));
            }
            if (workflowStepDef == null) {
                manualTransferDetails = labEventType.getManualTransferDetails();
            } else {
                manualTransferDetails = workflowStepDef.getManualTransferDetails();
                if (manualTransferDetails == null) {
                    manualTransferDetails = labEventType.getManualTransferDetails();
                }
            }
            assert manualTransferDetails != null;

            // A secondary event (one source, two destinations) requires an additional station event
            int numEvents = manualTransferDetails.getNumEvents();
            if (manualTransferDetails.getSecondaryEvent() != null) {
                numEvents++;
            }

            for (int i = 0; i < numEvents; i++) {
                StationEventType stationEvent;
                switch (manualTransferDetails.getMessageType()) {
                    case PLATE_EVENT:
                        stationEvent = new PlateEventType();
                        break;
                    case PLATE_TRANSFER_EVENT:
                        stationEvent = new PlateTransferEventType();
                        break;
                    case STATION_SETUP_EVENT:
                        stationEvent = new StationSetupEvent();
                        break;
                    case PLATE_CHERRY_PICK_EVENT:
                        stationEvent = new PlateCherryPickEvent();
                        break;
                    case RECEPTACLE_PLATE_TRANSFER_EVENT:
                        stationEvent = new ReceptaclePlateTransferEvent();
                        break;
                    case RECEPTACLE_EVENT:
                        stationEvent = new ReceptacleEventType();
                        break;
                    case RECEPTACLE_TRANSFER_EVENT:
                        stationEvent = new ReceptacleTransferEventType();
                        break;
                    case STRIP_TUBE_CHERRY_PICK_EVENT:
                        stationEvent = new PlateCherryPickEvent();
                        break;
                    default:
                        throw new RuntimeException("Unknown labEventType " + manualTransferDetails.getMessageType());
                }
                stationEvents.add(stationEvent);
            }
        }
    }

    @HandlesEvent(CHOOSE_EVENT_TYPE_ACTION)
    public Resolution chooseLabEventType() {
        List<String> reagentNames = new ArrayList<>();
        Map<String, LabEventType.ReagentRequirements> mapReagentNameToRequirement;
        if (workflowStepDef != null && !CollectionUtils.isEmpty(workflowStepDef.getReagentTypes())) {
            reagentNames = workflowStepDef.getReagentTypes();
            mapReagentNameToRequirement = new HashMap<>(reagentNames.size());
            for (String reagentName : reagentNames) {
                mapReagentNameToRequirement.put(reagentName, new LabEventType.ReagentRequirements(reagentName));
            }

        } else {
            reagentNames.addAll(manualTransferDetails.getReagentNames());
            mapReagentNameToRequirement = manualTransferDetails.getMapReagentNameToRequirements();
        }
        for (String reagentName : reagentNames) {
            for (int fieldIndex = 0; fieldIndex < mapReagentNameToRequirement.get(reagentName).getFieldCount(); fieldIndex++) {
                ReagentType reagentType = new ReagentType();
                reagentType.setKitType(reagentName);
                stationEvents.get(0).getReagent().add(reagentType);
            }
        }

        int stationEventIndex = 0;
        for (StationEventType stationEvent : stationEvents) {
            if (manualTransferDetails.getSecondaryEvent() != null && stationEventIndex > 0) {
                stationEvent.setEventType(manualTransferDetails.getSecondaryEvent().getName());
            } else {
                stationEvent.setEventType(labEventType.getName());
            }
            switch (manualTransferDetails.getMessageType()) {
                case PLATE_EVENT:
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    PlateType plateType = new PlateType();
                    VesselTypeGeometry vesselTypeGeometry = manualTransferDetails.getTargetVesselTypeGeometry();
                    plateType.setPhysType(vesselTypeGeometry.getDisplayName());
                    plateEventType.setPlate(plateType);
                    if (vesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateEventType.setPositionMap(new PositionMapType());
                    }
                break;
                case PLATE_TRANSFER_EVENT:
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    PlateType sourcePlate = new PlateType();
                    VesselTypeGeometry sourceVesselTypeGeometry = manualTransferDetails.getSourceVesselTypeGeometry();
                    sourcePlate.setPhysType(sourceVesselTypeGeometry.getDisplayName());
                    plateTransferEventType.setSourcePlate(sourcePlate);
                    if (sourceVesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateTransferEventType.setSourcePositionMap(new PositionMapType());
                    }

                    PlateType destinationPlateType = new PlateType();
                    VesselTypeGeometry targetVesselTypeGeometry = manualTransferDetails.getTargetVesselTypeGeometry();
                    destinationPlateType.setPhysType(targetVesselTypeGeometry.getDisplayName());
                    plateTransferEventType.setPlate(destinationPlateType);
                    if (targetVesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateTransferEventType.setPositionMap(new PositionMapType());
                    }
                    break;
                case STATION_SETUP_EVENT:
                    StationSetupEvent stationEventType = (StationSetupEvent) stationEvent;
                    break;
                case STRIP_TUBE_CHERRY_PICK_EVENT:
                    // todo jmt fix copy / paste from next case
                    //Source
                    PlateCherryPickEvent stripTubeCherryPickEvent = (PlateCherryPickEvent) stationEvent;
                    PlateType sourcePlateStripTube = new PlateType();
                    VesselTypeGeometry sourceVesselTypeGeometryStripTube = manualTransferDetails.getSourceVesselTypeGeometry();
                    sourcePlateStripTube.setPhysType(sourceVesselTypeGeometryStripTube.getDisplayName());
                    stripTubeCherryPickEvent.getSourcePlate().add(sourcePlateStripTube);
                    if (sourceVesselTypeGeometryStripTube instanceof RackOfTubes.RackType) {
                        stripTubeCherryPickEvent.getSourcePositionMap().add(new PositionMapType());
                    }

                    //Target
                    PlateType destinationPlateTypeStripTube = new PlateType();
                    VesselTypeGeometry targetVesselTypeGeometryStripTube = manualTransferDetails.getTargetVesselTypeGeometry();
                    destinationPlateTypeStripTube.setPhysType(targetVesselTypeGeometryStripTube.getDisplayName());
                    stripTubeCherryPickEvent.getPlate().add(destinationPlateTypeStripTube);
                    if (targetVesselTypeGeometryStripTube instanceof RackOfTubes.RackType) {
                        stripTubeCherryPickEvent.getPositionMap().add(new PositionMapType());
                    }
                    break;
                case PLATE_CHERRY_PICK_EVENT:
                    LabEventType.ManualTransferDetails localManualTransferDetails =
                            manualTransferDetails.getSecondaryEvent() != null && stationEventIndex > 0 ?
                                    manualTransferDetails.getSecondaryEvent().getManualTransferDetails() :
                                    manualTransferDetails;
                    PlateCherryPickEvent plateCherryPickEvent = (PlateCherryPickEvent) stationEvent;

                    //Source
                    PlateType sourcePlateCp = new PlateType();
                    VesselTypeGeometry sourceVesselTypeGeometryCp = localManualTransferDetails.getSourceVesselTypeGeometry();
                    sourcePlateCp.setPhysType(sourceVesselTypeGeometryCp.getDisplayName());
                    plateCherryPickEvent.getSourcePlate().add(sourcePlateCp);
                    if (sourceVesselTypeGeometryCp instanceof RackOfTubes.RackType) {
                        plateCherryPickEvent.getSourcePositionMap().add(new PositionMapType());
                    }

                    //Target
                    PlateType destinationPlateTypeCp = new PlateType();
                    VesselTypeGeometry targetVesselTypeGeometryCp = localManualTransferDetails.getTargetVesselTypeGeometry();
                    destinationPlateTypeCp.setPhysType(targetVesselTypeGeometryCp.getDisplayName());
                    plateCherryPickEvent.getPlate().add(destinationPlateTypeCp);
                    if (targetVesselTypeGeometryCp instanceof RackOfTubes.RackType) {
                        plateCherryPickEvent.getPositionMap().add(new PositionMapType());
                    }
                    break;
                case RECEPTACLE_PLATE_TRANSFER_EVENT:
                    ReceptaclePlateTransferEvent receptaclePlateTransferEvent = (ReceptaclePlateTransferEvent) stationEvent;
                    break;
                case RECEPTACLE_EVENT:
                    ReceptacleEventType receptacleEventType = (ReceptacleEventType) stationEvent;
                    ReceptacleType receptacleType = new ReceptacleType();
                    receptacleType.setReceptacleType(workflowStepDef == null ?
                            manualTransferDetails.getTargetVesselTypeGeometry().getDisplayName() :
                            workflowStepDef.getTargetBarcodedTubeType().getDisplayName());
                    receptacleEventType.setReceptacle(receptacleType);
                    break;
                case RECEPTACLE_TRANSFER_EVENT:
                    ReceptacleTransferEventType receptacleTransferEventType = (ReceptacleTransferEventType) stationEvent;
                    ReceptacleType sourceReceptacle = new ReceptacleType();
                    if (manualTransferDetails.getSourceVesselTypeGeometry() != null) {
                        sourceReceptacle.setReceptacleType(
                                manualTransferDetails.getSourceVesselTypeGeometry().getDisplayName());
                    } else if (manualTransferDetails.getSourceVesselTypeGeometries() == null ||
                               manualTransferDetails.getSourceVesselTypeGeometries().length == 0) {
                        throw new RuntimeException("Source VesselTypeGeometry isn't set for this event");
                    }
                    receptacleTransferEventType.setSourceReceptacle(sourceReceptacle);

                    ReceptacleType destinationReceptacle = new ReceptacleType();
                    if (manualTransferDetails.getTargetVesselTypeGeometry() != null) {
                        destinationReceptacle.setReceptacleType(
                                manualTransferDetails.getTargetVesselTypeGeometry().getDisplayName());
                    } else if (manualTransferDetails.getTargetVesselTypeGeometries() == null ||
                               manualTransferDetails.getTargetVesselTypeGeometries().length == 0) {
                        throw new RuntimeException("Target VesselTypeGeometry isn't set for this event");
                    }
                    receptacleTransferEventType.setReceptacle(destinationReceptacle);
                    break;
                default:
                    throw new RuntimeException("Unknown labEventType " + manualTransferDetails.getMessageType());
            }
            stationEventIndex++;
        }
        assignSyntheticBarcodes();
        isParseLimsFile = manualTransferDetails.isLimsFile();
        isUseWebCam = manualTransferDetails.isUseWebCam();
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    private void assignSyntheticBarcode(PlateType plateType, VesselTypeGeometry vesselTypeGeometry,
            String containerPrefix) {
        if (!vesselTypeGeometry.isBarcoded()) {
            String barcode;
            if (containerPrefix == null) {
                barcode = syntheticBarcode + anonymousRackDisambiguator;
                anonymousRackDisambiguator++;
            } else {
                barcode = containerPrefix + syntheticBarcode;
            }
            plateType.setBarcode(barcode);
        }
    }

    @Nullable
    public static WorkflowStepDef loadWorkflowStepDef(Date workflowEffectiveDate, WorkflowConfig workflowConfig,
                                                      String workflowProcessName, String workflowStepName) {
        WorkflowStepDef workflowStepDef = null;
        if (workflowProcessName != null) {
            workflowStepDef = workflowConfig.getStep(workflowProcessName, workflowStepName,
                    workflowEffectiveDate);
        }
        return workflowStepDef;
    }

    @HandlesEvent(RACK_SCAN_EVENT)
    public Resolution rackScan() throws ScannerException {
        scan();
        StationEventType stationEventType = stationEvents.get(scanIndex);
        PositionMapType positionMapType;

        if(manualTransferDetails.getMessageType().equals(LabEventType.MessageType.PLATE_CHERRY_PICK_EVENT)) {
            positionMapType = scanSource ? ((PlateCherryPickEvent) stationEventType).getSourcePositionMap().get(0) :
                    ((PlateCherryPickEvent) stationEventType).getPositionMap().get(0);
        }
        else {
            positionMapType = scanSource ? ((PlateTransferEventType) stationEventType).getSourcePositionMap() :
                    ((PlateEventType) stationEventType).getPositionMap();
        }

        for (Map.Entry<String, String> positionBarcodeEntry : rackScan.entrySet()) {
            ReceptacleType receptacleAtPosition = findReceptacleAtPosition(positionMapType, positionBarcodeEntry.getKey());
            if (receptacleAtPosition == null) {
                receptacleAtPosition = new ReceptacleType();
                positionMapType.getReceptacle().add(receptacleAtPosition);
            }
            receptacleAtPosition.setPosition(positionBarcodeEntry.getKey());
            receptacleAtPosition.setBarcode(positionBarcodeEntry.getValue());
        }

        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    @HandlesEvent(SKIP_LIMS_FILE_ACTION)
    public Resolution skipLimsFile() {
        chooseLabEventType();
        isParseLimsFile = false;
        isUseWebCam = false;
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    @HandlesEvent(PARSE_LIMS_FILE_ACTION)
    public Resolution parseLimsFile() {
        chooseLabEventType();
        if (limsUploadFile == null) {
            addGlobalValidationError("File not selected.");
            return new ForwardResolution(MANUAL_TRANSFER_PAGE);
        }
        InputStream limsFileStream = null;
        MessageCollection messageCollection = new MessageCollection();
        try {
            limsFileStream = limsUploadFile.getInputStream();
            switch (limsFileType) {
            case QIAGEN_BLOOD_BIOPSY_24:
                PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvents.get(0);
                plateTransferEventType.getSourcePlate().setSection(manualTransferDetails.getSourceSection().getSectionName());
                plateTransferEventType.getPlate().setSection(manualTransferDetails.getTargetSection().getSectionName());
                QiagenRackFileParser qiagenRackFileParser = new QiagenRackFileParser();
                qiagenRackFileParser.attachSourcePlateData(plateTransferEventType, limsFileStream,
                        messageCollection);

                // If the inputs to QIAsymphony are SM-IDs, convert them to (FluidX) vessel labels
                List<String> sampleIds = new ArrayList<>();
                for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap().getReceptacle()) {
                    String barcode = receptacleType.getBarcode();
                    if (barcode.startsWith("SM-")) {
                        sampleIds.add(barcode);
                    }
                }
                if (!sampleIds.isEmpty()) {
                    Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIds);
                    for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap().getReceptacle()) {
                        String barcode = receptacleType.getBarcode();
                        MercurySample mercurySample = mapIdToMercurySample.get(barcode);
                        if (mercurySample == null) {
                            messageCollection.addError("Failed to find sample " + barcode);
                        } else {
                            Set<LabVessel> labVessels = mercurySample.getLabVessel();
                            if (labVessels.size() != 1) {
                                messageCollection.addError("Expected one vessel for " + barcode + ", found " +
                                        labVessels.size());
                            }
                            receptacleType.setBarcode(labVessels.iterator().next().getLabel());
                        }
                    }
                }
                break;
            case DBS_PUNCHER:
                DBSPuncherFileParser dbsPuncherFileParser = new DBSPuncherFileParser();
                DBSPuncherFileParser.DBSPuncherRun puncherRun =
                        dbsPuncherFileParser.parseRun(limsFileStream, messageCollection);
                Map<VesselPosition, String> mapPositionToSampleBarcode = puncherRun.getMapPositionToSampleBarcode();
                if (mapPositionToSampleBarcode.size() == 0) {
                    messageCollection.addError(
                            "Failed to find any transfers in file: " + mapPositionToSampleBarcode.size());
                } else {
                    Iterator<StationEventType> eventIterator = stationEvents.iterator();
                    PlateTransferEventType plateTransferEventTypeDbs =
                            (PlateTransferEventType) eventIterator.next();
                    String sourceBarcode =  manualTransferDetails.getSourceContainerPrefix() + syntheticBarcode;
                    plateTransferEventTypeDbs.getSourcePlate().setBarcode(sourceBarcode);
                    plateTransferEventTypeDbs.getPlate().setBarcode(puncherRun.getPlateBarcode());
                    PositionMapType sourcePositionMap = plateTransferEventTypeDbs.getSourcePositionMap();
                    sourcePositionMap.setBarcode(plateTransferEventTypeDbs.getSourcePlate().getBarcode());
                    for (Map.Entry<VesselPosition, String> entry: mapPositionToSampleBarcode.entrySet()) {
                        ReceptacleType receptacleType = new ReceptacleType();
                        receptacleType.setReceptacleType(manualTransferDetails.getSourceBarcodedTubeType().
                                getAutomationName());
                        receptacleType.setBarcode(entry.getValue());
                        receptacleType.setPosition(entry.getKey().name());
                        sourcePositionMap.getReceptacle().add(receptacleType);
                    }
                }

                break;
            }
        } catch (IOException e) {
            log.error("IO Exception when parsing LIMS File", e);
            messageCollection.addError("IO Exception when parsing LIMS File");
        } finally {
            IOUtils.closeQuietly(limsFileStream);

            try {
                limsUploadFile.delete();
            } catch (IOException ignored) {
                // If cannot delete, oh well.
            }
        }

        addMessages(messageCollection);
        isParseLimsFile = messageCollection.hasErrors();
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    /**
     * Called after the user has entered barcodes.  Fetches existing data, if any.
     * @return JSP
     */
    @HandlesEvent(FETCH_EXISTING_ACTION)
    public Resolution fetchExisting() {
        isValidation = true;
        LabBatch labBatch = null;
        if (batchName != null) {
            labBatch = labBatchDao.findByName(batchName);
        }
        MessageCollection messageCollection = new MessageCollection();
        validateBarcodes(labBatch, messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    public String getImageFile() {
        return imageFile;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    public String getEventClass() {
        return eventClass;
    }

    public void setEventClass(String eventClass) {
        this.eventClass = eventClass;
    }

    @HandlesEvent(DECODE_IMAGE_ACTION)
    public Resolution decodeBarcodeImage() throws IOException {
        String base64Image = getImageFile().split(",")[1];
        byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(base64Image);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        File outputfile = File.createTempFile("DecodedImage", "png");
        ImageIO.write(img, "png", outputfile);
        DecodeResponse decodeResult = barcodeDecoderRestClient.analyzeImage(outputfile, getEventClass());
        ObjectMapper mapper = new ObjectMapper();
        return new StreamingResolution("application/json", mapper.writeValueAsString(decodeResult));
    }

    private void validateBarcodes(@Nullable LabBatch labBatch, MessageCollection messageCollection) {
        switch (manualTransferDetails.getMessageType()) {
            case PLATE_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    loadPlateFromDb(plateEventType.getPlate(), plateEventType.getPositionMap(), true, null, labBatch,
                            messageCollection, Direction.SOURCE);
                }
                break;
            case PLATE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    Map<String, LabVessel> mapBarcodeToVessel = loadPlateFromDb(plateTransferEventType.getSourcePlate(),
                            plateTransferEventType.getSourcePositionMap(), true, null, labBatch, messageCollection,
                            Direction.SOURCE);
                    LabEventType repeatedEvent = manualTransferDetails.getRepeatedEvent();
                    if (repeatedEvent != null) {
                        validateRepeatedEvent(plateTransferEventType, mapBarcodeToVessel, repeatedEvent,
                                manualTransferDetails.getRepeatedWorkflowQualifier(), messageCollection);
                    }
                    loadPlateFromDb(plateTransferEventType.getPlate(), plateTransferEventType.getPositionMap(),
                            manualTransferDetails.isTargetExpectedToExist(),
                            manualTransferDetails.isTargetExpectedEmpty(), labBatch, messageCollection,
                            Direction.TARGET);
                }
                break;
            case STATION_SETUP_EVENT:
                break;
            case STRIP_TUBE_CHERRY_PICK_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateCherryPickEvent plateCherryPickEvent = (PlateCherryPickEvent) stationEvent;

                    loadPlateFromDb(plateCherryPickEvent.getSourcePlate().get(0),
                            plateCherryPickEvent.getSourcePositionMap().get(0), true, null, labBatch, messageCollection,
                            Direction.SOURCE);

                    loadPlateFromDb(plateCherryPickEvent.getPlate().get(0), plateCherryPickEvent.getPositionMap().get(0),
                            false, null, labBatch, messageCollection,
                            Direction.TARGET);

                    if (messageCollection.hasErrors() || isValidation) {
                        break;
                    }
                }
                break;
            case PLATE_CHERRY_PICK_EVENT:
                Set<String> rootSampleIds = new HashSet<>();
                MultiValuedMap<String, String> mapPositionToSampleIds = new HashSetValuedHashMap<>();
                Map<String, SampleData> mapSampleIdToData = null;
                for (int eventIndex = 0; eventIndex < stationEvents.size(); eventIndex++) {
                    StationEventType stationEvent = stationEvents.get(eventIndex);
                    PlateCherryPickEvent plateCherryPickEvent = (PlateCherryPickEvent) stationEvent;

                    if (manualTransferDetails.getSecondaryEvent() == null || eventIndex == 0) {
                        Map<String, LabVessel> mapBarcodeToVessel = loadPlateFromDb(plateCherryPickEvent.getSourcePlate().get(0),
                                plateCherryPickEvent.getSourcePositionMap().get(0), true, null, labBatch, messageCollection,
                                Direction.SOURCE);

                        //Check for duplicate molecular indexes in source tubes.
                        Set<String> molIndexSchemes = new HashSet<>();
                        for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
                            ReceptacleType receptacleType = findReceptacleAtPosition(
                                    plateCherryPickEvent.getSourcePositionMap().get(0), cherryPickSourceType.getWell());
                            LabVessel currentLabVessel = mapBarcodeToVessel.get(receptacleType.getBarcode());
                            if (currentLabVessel == null) {
                                continue;
                            }
                            for (SampleInstanceV2 sample : currentLabVessel.getSampleInstancesV2()) {
                                if (sample.getMolecularIndexingScheme() != null) {
                                    String molIndex = sample.getMolecularIndexingScheme().getName();
                                    if (molIndex != null) {
                                        if (!molIndexSchemes.add(molIndex)) {
                                            messageCollection.addWarning("Duplicate molecular index: " + molIndex);
                                        }
                                    }
                                }
                            }
                        }

                        // Get participants in source tubes
                        if (manualTransferDetails.isRequireSingleParticipant()) {
                            // Get sample data for each source
                            for (ReceptacleType receptacleType :
                                    plateCherryPickEvent.getSourcePositionMap().get(0).getReceptacle()) {
                                if (!StringUtils.isEmpty(receptacleType.getBarcode())) {
                                    LabVessel currentLabVessel = mapBarcodeToVessel.get(receptacleType.getBarcode());
                                    if (currentLabVessel == null) {
                                        continue;
                                    }
                                    for (SampleInstanceV2 sample : currentLabVessel.getSampleInstancesV2()) {
                                        String rootSampleName = sample.getMercuryRootSampleName();
                                        if (rootSampleName == null) {
                                            messageCollection.addError("No root sample for " +
                                                    currentLabVessel.getLabel());
                                        } else {
                                            rootSampleIds.add(rootSampleName);
                                        }
                                        mapPositionToSampleIds.put(receptacleType.getPosition(), rootSampleName);
                                    }
                                }
                            }
                            if (!rootSampleIds.isEmpty()) {
                                // Map sample to participant
                                mapSampleIdToData = sampleDataFetcher.fetchSampleData(rootSampleIds);
                                for (SampleData sampleData : mapSampleIdToData.values()) {
                                    if (StringUtils.isEmpty(sampleData.getCollaboratorParticipantId())) {
                                        messageCollection.addError("No collaborator participant ID for " +
                                                sampleData.getSampleId());
                                    }
                                }
                            }
                        }
                    }

                    // Check for duplicate participants in pooling transfers
                    if (manualTransferDetails.isRequireSingleParticipant()) {

                        if (!rootSampleIds.isEmpty()) {
                            // Map destination positions to source positions
                            MultiValuedMap<String, String> mapDestToSource = new HashSetValuedHashMap<>();
                            for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
                                mapDestToSource.put(cherryPickSourceType.getDestinationWell(),
                                        cherryPickSourceType.getWell());
                            }

                            // If dest has multiple sources, check they are the same participant
                            for (String dest : mapDestToSource.keySet()) {
                                Set<String> ptIds = new HashSet<>();
                                Collection<String> sources = mapDestToSource.get(dest);
                                if (sources.size() > 1) {
                                    for (String source : sources) {
                                        Collection<String> sampleIds = mapPositionToSampleIds.get(source);
                                        for (String sampleId : sampleIds) {
                                            SampleData sampleData = mapSampleIdToData.get(sampleId);
                                            ptIds.add(sampleData.getCollaboratorParticipantId());
                                        }
                                    }
                                }
                                if (ptIds.size() > 1) {
                                    messageCollection.addError("More than one participant: " +
                                            StringUtils.join(ptIds.toArray(), ','));
                                }
                            }
                        }
                    }

                    loadPlateFromDb(plateCherryPickEvent.getPlate().get(0), plateCherryPickEvent.getPositionMap().get(0),
                            false, null, labBatch, messageCollection, Direction.TARGET);
                }
                break;
            case RECEPTACLE_PLATE_TRANSFER_EVENT:
                break;
            case RECEPTACLE_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    ReceptacleEventType receptacleEventType = (ReceptacleEventType) stationEvent;
                    loadReceptacleFromDb(receptacleEventType.getReceptacle(), true, messageCollection);
                }
                break;
            case RECEPTACLE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    ReceptacleTransferEventType receptacleTransferEventType = (ReceptacleTransferEventType) stationEvent;
                    loadReceptacleFromDb(receptacleTransferEventType.getSourceReceptacle(), true, messageCollection);
                    // todo jmt take required, empty from workflow
                    loadReceptacleFromDb(receptacleTransferEventType.getReceptacle(), false, messageCollection);
                }
                break;
        }
    }

    /**
     * Validate that the sources and destinations in a repeated event are the same as those in the event that it
     * repeats.
     * @param plateTransferEventType    second event
     * @param mapBarcodeToVessel        database entities
     * @param repeatedEvent             first event type
     * @param repeatedWorkflowQualifier first event qualifier
     * @param messageCollection         output, errors and infos
     */
    private static void validateRepeatedEvent(PlateTransferEventType plateTransferEventType,
            Map<String, LabVessel> mapBarcodeToVessel, LabEventType repeatedEvent,
            String repeatedWorkflowQualifier, MessageCollection messageCollection) {

        // For each vessel in source section, map to destination
        Map<String, String> mapSourceBarcodeToTargetBarcode = new HashMap<>();
        SBSSection sourceSection = SBSSection.getBySectionName(plateTransferEventType.getSourcePlate().getSection());
        SBSSection targetSection = SBSSection.getBySectionName(plateTransferEventType.getPlate().getSection());
        for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap().getReceptacle()) {
            if (receptacleType.getBarcode() == null) {
                continue;
            }
            VesselPosition targetVesselPosition = targetSection.getWells().get(
                    sourceSection.getWells().indexOf(VesselPosition.getByName(receptacleType.getPosition())));
            for (ReceptacleType targetReceptacleType : plateTransferEventType.getPositionMap().getReceptacle()) {
                if (targetReceptacleType.getPosition().equals(targetVesselPosition.name())) {
                    mapSourceBarcodeToTargetBarcode.put(receptacleType.getBarcode(), targetReceptacleType.getBarcode());
                    break;
                }
            }
        }

        // Compare source and destination barcodes in this transfer to previous transfer
        int matches = 0;
        int errors = 0;
        for (String sourceBarcode : mapSourceBarcodeToTargetBarcode.keySet()) {
            LabVessel currentLabVessel = mapBarcodeToVessel.get(sourceBarcode);
            if (currentLabVessel == null) {
                // We expect loadPlateFromDb to have added an error for a missing source
                continue;
            }
            boolean found = false;
            for (LabEvent labEvent : currentLabVessel.getTransfersFrom()) {
                if (labEvent.getLabEventType() == repeatedEvent &&
                        (repeatedWorkflowQualifier == null ||
                                labEvent.getWorkflowQualifier().equals(repeatedWorkflowQualifier))) {
                    found = true;
                    SectionTransfer sectionTransfer = labEvent.getSectionTransfers().iterator().next();
                    VesselPosition sourceVesselPos = sectionTransfer.getSourceVesselContainer().getPositionOfVessel(
                            currentLabVessel);
                    int sourceIndex = sectionTransfer.getSourceSection().getWells().indexOf(sourceVesselPos);
                    LabVessel previousTargetVessel = sectionTransfer.getTargetVesselContainer().getVesselAtPosition(
                            sectionTransfer.getTargetSection().getWells().get(sourceIndex));
                    String currentTargetBarcode = mapSourceBarcodeToTargetBarcode.get(currentLabVessel.getLabel());
                    assert previousTargetVessel != null;
                    if (previousTargetVessel.getLabel().equals(currentTargetBarcode)) {
                        matches++;
                    } else {
                        messageCollection.addError("Expected " + previousTargetVessel.getLabel() + ", but found " +
                                currentTargetBarcode);
                        errors++;
                    }
                }
            }
            if (!found) {
                messageCollection.addError(currentLabVessel.getLabel() + " not found in previous message");
                errors++;
            }
        }
        if (matches == mapSourceBarcodeToTargetBarcode.size() && errors == 0) {
            messageCollection.addInfo("Transfer matches previous");
        }
    }

    private void loadReceptacleFromDb(ReceptacleType receptacleType, boolean required,
            MessageCollection messageCollection) {
        if (receptacleType != null) {
            String barcode = receptacleType.getBarcode();
            if (!StringUtils.isBlank(barcode)) {
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    if (required) {
                        messageCollection.addError(barcode + " is not in the database");
                    } else {
                        messageCollection.addInfo(barcode + " is not in the database");
                    }
                } else {
                    if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                        BarcodedTube barcodedTube = OrmUtil.proxySafeCast(labVessel, BarcodedTube.class);
                        receptacleType.setReceptacleType(barcodedTube.getTubeType().getDisplayName());
                        if (barcodedTube.getVolume() != null) {
                            receptacleType.setVolume(barcodedTube.getVolume());
                        }
                        receptacleType.setConcentration(barcodedTube.getConcentration());
                        receptacleType.setReceptacleWeight(barcodedTube.getReceptacleWeight());
                        messageCollection.addInfo(barcode + " is in the database");
                    } else {
                        messageCollection.addError(barcode + " is not a tube");
                    }
                }
            }
        }
    }

    private enum Direction {
        SOURCE("Source"),
        TARGET("Destination");

        private String text;

        Direction(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    private Map<String, LabVessel> loadPlateFromDb(PlateType plateType, PositionMapType positionMapType,
            boolean required, Boolean expectedEmpty, @Nullable LabBatch labBatch, MessageCollection messageCollection,
            Direction direction) {
        Map<String, LabVessel> returnMapBarcodeToVessel = new HashMap<>();
        if (plateType != null) {
            String barcode = plateType.getBarcode();
            if (!StringUtils.isBlank(barcode)) {
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    // Racks don't have to exist, static plates do
                    if (required && positionMapType == null) {
                        messageCollection.addError(direction.getText() + " " + barcode + " is not in the database");
                    } else {
                        messageCollection.addInfo(direction.getText() + " " + barcode + " is not in the database");
                    }
                } else {
                    messageCollection.addInfo(direction.getText() + " " + barcode + " is in the database");
                    returnMapBarcodeToVessel.put(labVessel.getLabel(), labVessel);
                }
            }
        }
        if (positionMapType != null) {
            List<String> barcodes = new ArrayList<>();
            for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                if (!StringUtils.isBlank(receptacleType.getBarcode())) {
                    barcodes.add(receptacleType.getBarcode());
                }
            }
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
            LabEventFactory.trySampleIds(barcodes, mapBarcodeToVessel, mercurySampleDao);
            for (Map.Entry<String, LabVessel> stringLabVesselEntry : mapBarcodeToVessel.entrySet()) {
                LabVessel labVessel = stringLabVesselEntry.getValue();
                String barcode = stringLabVesselEntry.getKey();
                if (labVessel == null) {
                    String message = direction.getText() + " " + barcode + " is not in the database";
                    if (required) {
                        messageCollection.addError(message);
                    } else {
                        messageCollection.addInfo(message);
                    }
                } else {
                    String message = direction.getText() + " " + barcode + " is in the database";
                    if (required) {
                        messageCollection.addInfo(message);
                    } else {
                        if (labEventType != LabEventType.DEV) {
                            messageCollection.addError(message);
                        } else {
                            messageCollection.addWarning(message);
                        }
                    }
                    if (expectedEmpty != null) {
                        if (labVessel.getTransfersTo().isEmpty()) {
                            if (!expectedEmpty) {
                                messageCollection.addError(direction.getText() + " " + barcode + " is empty");
                            }
                        } else {
                            if (expectedEmpty) {
                                messageCollection.addError(direction.getText() + " " + barcode + " is not empty");
                            }
                        }
                    }

                    if (labBatch != null && direction == Direction.SOURCE) {
                        Collection<LabBatch> nearestWorkflowLabBatches = labVessel.getNearestWorkflowLabBatches();
                        List<LabBatch> workflowLabBatches = labVessel.getWorkflowLabBatches();
                        if (!workflowLabBatches.contains(labBatch) && nearestWorkflowLabBatches != null &&
                            !nearestWorkflowLabBatches.contains(labBatch)) {
                            messageCollection.addError(direction.getText() + " " + barcode + " is not in batch " + labBatch
                                            .getBatchName());
                        }
                    }
                }
            }
            if (labBatch != null && required && barcodes.size() != labBatch.getLabBatchStartingVessels().size()) {
                messageCollection.addWarning("Batch has " + labBatch.getLabBatchStartingVessels().size() +
                        " vessels, but " + barcodes.size() + " were scanned.");
            }
            if (labBatch != null && required && direction == Direction.SOURCE) {
                List<String> expectedBarcodes = labBatch.getLabBatchStartingVessels()
                        .stream().map(lbsv -> lbsv.getLabVessel().getLabel()).collect(Collectors.toList());
                for (String missingBarcode : CollectionUtils.removeAll(expectedBarcodes, barcodes)) {
                    messageCollection.addWarning("Expected to find " + missingBarcode + " in batch.");
                }
                for (String missingBarcode : CollectionUtils.removeAll(barcodes, expectedBarcodes)) {
                    messageCollection.addWarning(missingBarcode + " is not expected to be in batch.");
                }
            }
            returnMapBarcodeToVessel.putAll(mapBarcodeToVessel);
        }
        return returnMapBarcodeToVessel;
    }

    private void assignSyntheticBarcodes() {
        int stationEventIndex = 0;
        for (StationEventType stationEvent : stationEvents) {
            switch (manualTransferDetails.getMessageType()) {
                case PLATE_EVENT:
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    VesselTypeGeometry vesselTypeGeometry = manualTransferDetails.getTargetVesselTypeGeometry();
                    assignSyntheticBarcode(plateEventType.getPlate(), vesselTypeGeometry,
                            manualTransferDetails.getSourceContainerPrefix());
                    break;
                case PLATE_TRANSFER_EVENT:
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    VesselTypeGeometry sourceVesselTypeGeometry = manualTransferDetails.getSourceVesselTypeGeometry();
                    assignSyntheticBarcode(plateTransferEventType.getSourcePlate(), sourceVesselTypeGeometry,
                            manualTransferDetails.getSourceContainerPrefix());

                    VesselTypeGeometry targetVesselTypeGeometry = manualTransferDetails.getTargetVesselTypeGeometry();
                    assignSyntheticBarcode(plateTransferEventType.getPlate(), targetVesselTypeGeometry,
                            manualTransferDetails.getTargetContainerPrefix());
                    break;
                case PLATE_CHERRY_PICK_EVENT:
                    LabEventType.ManualTransferDetails localManualTransferDetails =
                            manualTransferDetails.getSecondaryEvent() != null && stationEventIndex > 0 ?
                                    manualTransferDetails.getSecondaryEvent().getManualTransferDetails() :
                                    manualTransferDetails;
                    PlateCherryPickEvent plateCherryPickEvent = (PlateCherryPickEvent) stationEvent;

                    //Source
                    VesselTypeGeometry sourceVesselTypeGeometryCp = localManualTransferDetails.getSourceVesselTypeGeometry();
                    assignSyntheticBarcode(plateCherryPickEvent.getSourcePlate().get(0), sourceVesselTypeGeometryCp,
                            localManualTransferDetails.getSourceContainerPrefix());

                    //Target
                    VesselTypeGeometry targetVesselTypeGeometryCp = localManualTransferDetails.getTargetVesselTypeGeometry();
                    assignSyntheticBarcode(plateCherryPickEvent.getPlate().get(0), targetVesselTypeGeometryCp,
                            localManualTransferDetails.getTargetContainerPrefix() + anonymousRackDisambiguator);
                    for (CherryPickSourceType cherryPickSourceType : plateCherryPickEvent.getSource()) {
                        if (!sourceVesselTypeGeometryCp.isBarcoded()) {
                            cherryPickSourceType.setBarcode(plateCherryPickEvent.getSourcePlate().get(0).getBarcode());
                        }
                        if (!targetVesselTypeGeometryCp.isBarcoded()) {
                            cherryPickSourceType.setDestinationBarcode(plateCherryPickEvent.getPlate().get(0).getBarcode());
                        }
                    }

                    break;
            }
            stationEventIndex++;
        }
    }

    @HandlesEvent(TRANSFER_ACTION)
    public Resolution transfer() {
        BettaLIMSMessage bettaLIMSMessage = buildBettaLIMSMessage();

        if (getContext().getValidationErrors().isEmpty()) {
            try {
                ObjectMarshaller<BettaLIMSMessage> bettaLIMSMessageObjectMarshaller =
                        new ObjectMarshaller<>(BettaLIMSMessage.class);
                bettaLimsMessageResource.storeAndProcess(bettaLIMSMessageObjectMarshaller.marshal(bettaLIMSMessage));
                // Assign new synthetic barcodes, in case user pastes in new tube barcodes without refreshing page
                assignSyntheticBarcodes();
                addMessage("Transfer recorded successfully.");
            } catch (Exception e) {
                log.error("Failed to process message", e);
                addGlobalValidationError(e.getCause().getMessage());
            }
        }
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    @Nullable
    BettaLIMSMessage buildBettaLIMSMessage() {
        MessageCollection messageCollection = new MessageCollection();
        LabBatch labBatch = null;
        if (batchName != null) {
            labBatch = labBatchDao.findByName(batchName);
        }
        validateBarcodes(labBatch, messageCollection);
        // If there are only infos, ignore them
        if (!messageCollection.getErrors().isEmpty()) {
            addMessages(messageCollection);
        }

        Map<String, LabEventType.ReagentRequirements> mapReagentNameToRequirements =
                manualTransferDetails.getMapReagentNameToRequirements();
        for (ReagentType reagentType : stationEvents.get(0).getReagent()) {
            if (StringUtils.isBlank(reagentType.getKitType())) {
                addGlobalValidationError("Reagent type is required");
            }
            LabEventType.ReagentRequirements reagentRequirements =
                    mapReagentNameToRequirements.get(reagentType.getKitType());

            // If the reagent barcode is not blank, check to see if the barcode is valid based off requirements defined.
            if (!StringUtils.isBlank(reagentType.getBarcode()) && !reagentRequirements.verifyBarcode(reagentType.getBarcode())){
                addGlobalValidationError("The reagent barcode " + reagentType.getBarcode() + " is in an invalid format.");
            }

            // We're only checking for reagent requirements if there is only one instance of a reagent expected.
            if (reagentRequirements.getFieldCount() == 1) {
                if (StringUtils.isBlank(reagentType.getBarcode())) {
                    addGlobalValidationError("Reagent barcode is required");
                }
                // If a expiration date is expected, add an error if one is not found.
                // The manual transfer page is expected to handle validating dates and provide a warning accordingly.
                if (reagentRequirements.isExpirationDateIncluded()) {
                    if (reagentType.getExpiration() == null) {
                        addGlobalValidationError("Reagent expiration is required");
                    }
                }
            }
        }

        if (stationEvents.get(0).getStation() != null &&
                stationEvents.get(0).getStation().equalsIgnoreCase(BAD_STATION_NAME)) {
            addGlobalValidationError("A valid station is required");
        }

        BettaLIMSMessage bettaLIMSMessage = null;
        if (getContext().getValidationErrors().isEmpty()) {
            // remove unused reagents
            Iterator<ReagentType> reagentIterator = stationEvents.get(0).getReagent().iterator();
            while (reagentIterator.hasNext()) {
                ReagentType reagentType = reagentIterator.next();
                if (StringUtils.isBlank(reagentType.getBarcode()) &&
                    mapReagentNameToRequirements.get(reagentType.getKitType()).getFieldCount() > 1) {
                    reagentIterator.remove();
                }
            }
            bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
            Date start = new Date();
            Iterator<StationEventType> iterator = stationEvents.iterator();
            if (stationEvents.get(0).getStation() == null) {
                stationEvents.get(0).setStation(LabEvent.UI_EVENT_LOCATION);
            }

            int eventIndex = 0;
            while (iterator.hasNext()) {
                StationEventType stationEvent = iterator.next();
                if (workflowStepDef != null) {
                    stationEvent.setWorkflowQualifier(workflowStepDef.getWorkflowQualifier());
                }
                stationEvent.setOperator(getUserBean().getLoginUserName());
                stationEvent.setProgram(LabEvent.UI_PROGRAM_NAME);
                stationEvent.setDisambiguator((long) (eventIndex + 1));
                stationEvent.setStart(start);

                // Remove empty elements
                if (stationEvent instanceof PlateTransferEventType) {
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    cleanupPositionMap(plateTransferEventType.getSourcePositionMap(),
                            plateTransferEventType.getSourcePlate(),
                            manualTransferDetails.getSourceVesselTypeGeometry());
                    cleanupPositionMap(plateTransferEventType.getPositionMap(), plateTransferEventType.getPlate(),
                            manualTransferDetails.getTargetVesselTypeGeometry());
                    if (manualTransferDetails.getSecondaryEvent() != null && eventIndex > 0) {
                        // copy source from primary
                        PlateTransferEventType firstPlateTransferEventType =
                                (PlateTransferEventType) stationEvents.get(0);
                        plateTransferEventType.setSourcePlate(firstPlateTransferEventType.getSourcePlate());
                        plateTransferEventType.setSourcePositionMap(firstPlateTransferEventType.getSourcePositionMap());
                    }
                    if (manualTransferDetails.getTargetWellType() != null &&
                        manualTransferDetails.getTargetWellType() != PlateWell.WellType.None) {
                        addWellTypes(plateTransferEventType, plateTransferEventType.getPlate().getBarcode(),
                                manualTransferDetails.getTargetVesselTypeGeometry(), manualTransferDetails.getTargetWellType());
                        if (labEventType.getResultingMaterialType() != null) {
                            addResultingMaterialType(labEventType.getResultingMaterialType(),
                                    plateTransferEventType.getSourcePositionMap(), plateTransferEventType.getPositionMap());
                        }
                    }
                    if (manualTransferDetails.sourceMassRemoved()) {
                        addMass(plateTransferEventType.getSourcePositionMap(), plateTransferEventType.getPositionMap());
                        boolean depleteAll = getDepleteAll() != null && getDepleteAll().containsKey(eventIndex) &&
                                             getDepleteAll().get(eventIndex);
                        addDepleteMetadata(plateTransferEventType.getSourcePositionMap(), depleteAll);
                    }
                    bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
                } else if (stationEvent instanceof PlateEventType) {
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    // Remove events for which the user did not enter a barcode
                    if (manualTransferDetails.getNumEvents() > 1 && plateEventType.getPositionMap() == null &&
                            (plateEventType.getPlate() == null || plateEventType.getPlate().getBarcode() == null)) {
                        iterator.remove();
                        continue;
                    }
                    cleanupPositionMap(plateEventType.getPositionMap(), plateEventType.getPlate(),
                            manualTransferDetails.getTargetVesselTypeGeometry());
                    bettaLIMSMessage.getPlateEvent().add(plateEventType);
                } else if (stationEvent instanceof StationSetupEvent) {
                    bettaLIMSMessage.setStationSetupEvent((StationSetupEvent) stationEvent);
                } else if (stationEvent instanceof PlateCherryPickEvent) {

                    PlateCherryPickEvent plateCherryPickEvent = (PlateCherryPickEvent) stationEvent;
                    if (manualTransferDetails.getSecondaryEvent() == null || eventIndex == 0) {
                        cleanupPositionMap(plateCherryPickEvent.getSourcePositionMap().get(0),
                                plateCherryPickEvent.getSourcePlate().get(0),
                                manualTransferDetails.getSourceVesselTypeGeometry());
                    }
                    cleanupPositionMap(plateCherryPickEvent.getPositionMap().get(0), plateCherryPickEvent.getPlate().get(0),
                            manualTransferDetails.getTargetVesselTypeGeometry());
                    if (manualTransferDetails.getSecondaryEvent() != null && eventIndex > 0) {
                        // copy source from primary
                        PlateCherryPickEvent firstPlateCherryPickEventType =
                                (PlateCherryPickEvent) stationEvents.get(0);
                        plateCherryPickEvent.getSourcePlate().addAll(firstPlateCherryPickEventType.getSourcePlate());
                        plateCherryPickEvent.getSourcePositionMap().addAll(firstPlateCherryPickEventType.getSourcePositionMap());
                    }
                    if (manualTransferDetails.destinationMarkBackup() && plateCherryPickEvent.getPositionMap().size() == 1) {
                        addMarkBackupMetadata(plateCherryPickEvent.getPositionMap().get(0));
                    }
                    bettaLIMSMessage.getPlateCherryPickEvent().add((PlateCherryPickEvent) stationEvent);
                } else if (stationEvent instanceof ReceptaclePlateTransferEvent) {
                    bettaLIMSMessage.getReceptaclePlateTransferEvent().add((ReceptaclePlateTransferEvent) stationEvent);
                } else if (stationEvent instanceof ReceptacleTransferEventType) {
                    bettaLIMSMessage.getReceptacleTransferEvent().add((ReceptacleTransferEventType) stationEvent);
                } else if (stationEvent instanceof ReceptacleEventType) {
                    bettaLIMSMessage.getReceptacleEvent().add((ReceptacleEventType) stationEvent);
                } else {
                    throw new RuntimeException("Unknown StationEvent subclass " + stationEvent.getClass());
                }

                if (eventIndex > 0) {
                    stationEvent.getReagent().addAll(stationEvents.get(0).getReagent());
                    stationEvent.setStation(stationEvents.get(0).getStation());
                }
                eventIndex++;
            }
        }
        return bettaLIMSMessage;
    }

    /**
     * For plates, add a Well Type to position map in order to include things later like Volume, Concentration, or Mass.
     */
    private void addWellTypes(PlateEventType plateEventType, String plateBarcode, VesselTypeGeometry vesselTypeGeometry,
                              VesselTypeGeometry targetWellType) {
        if (plateEventType.getPositionMap() == null) {
            plateEventType.setPositionMap(new PositionMapType());
            plateEventType.getPositionMap().setBarcode(plateBarcode);
        }
        for (VesselPosition vesselPosition: vesselTypeGeometry.getVesselGeometry().getVesselPositions()) {
            ReceptacleType receptacleType = new ReceptacleType();
            String wellType = PlateWell.WellType.getByDisplayName(targetWellType.getDisplayName()).getAutomationName();
            receptacleType.setReceptacleType(wellType);
            receptacleType.setPosition(vesselPosition.name());
            plateEventType.getPositionMap().getReceptacle().add(receptacleType);
        }
    }

    private void addMass(PositionMapType sourcePositionMap, PositionMapType positionMapType) {
        if (positionMapType != null) {
            for (ReceptacleType receptacleType: positionMapType.getReceptacle()) {
                for (ReceptacleType sourceReceptacle: sourcePositionMap.getReceptacle()) {
                    if (receptacleType.getPosition() != null &&
                        receptacleType.getPosition().equals(sourceReceptacle.getPosition())) {
                        receptacleType.setMass(sourceReceptacle.getMass());
                    }
                }
            }
        }
    }

    private void addResultingMaterialType(MaterialType resultingMaterialType, PositionMapType sourcePositionMap,
                                          PositionMapType positionMapType) {
        if (positionMapType != null) {
            for (ReceptacleType receptacleType: positionMapType.getReceptacle()) {
                for (ReceptacleType sourceReceptacle: sourcePositionMap.getReceptacle()) {
                    if (receptacleType.getPosition() != null &&
                        receptacleType.getPosition().equals(sourceReceptacle.getPosition())) {
                        receptacleType.setMaterialType(resultingMaterialType.getDisplayName());
                    }
                }
            }
        }
    }

    /**
     * User can deplete all via a checkbox at top, or individually on a well level, priority being check all at top.
     * @param positionMapType - position map to update receptacle metadata tag.
     * @param depleteAll - if for given event the user selected deplete all checkbox.
     */
    private void addDepleteMetadata(PositionMapType positionMapType, boolean depleteAll) {
        if (mapPositionToDepleteFlag == null && !depleteAll) {
            return;
        }
        for (ReceptacleType receptacleType: positionMapType.getReceptacle()) {
            VesselPosition vesselPosition = VesselPosition.getByName(receptacleType.getPosition());
            if (depleteAll || mapPositionToDepleteFlag.containsKey(vesselPosition)) {
                MetadataType depleteMeta = new MetadataType();
                Boolean depleteFlag = depleteAll || mapPositionToDepleteFlag.get(vesselPosition);
                depleteMeta.setName(Metadata.Key.DEPLETE_WELL.getDisplayName());
                depleteMeta.setValue(String.valueOf(depleteFlag));
                receptacleType.getMetadata().add(depleteMeta);
            }
        }
    }

    /**
     * User can mark a sample as a backup individually on a well level.
     * @param positionMapType - position map to update receptacle metadata tag.
     */
    private void addMarkBackupMetadata(PositionMapType positionMapType) {
        if (mapPositionToBackupFlag == null) {
            return;
        }
        for (ReceptacleType receptacleType: positionMapType.getReceptacle()) {
            VesselPosition vesselPosition = VesselPosition.getByName(receptacleType.getPosition());
            if (mapPositionToBackupFlag.containsKey(vesselPosition)) {
                MetadataType backupMetadata = new MetadataType();
                Boolean markBackup = mapPositionToBackupFlag.get(vesselPosition);
                backupMetadata.setName(Metadata.Key.MARK_BACKUP.getDisplayName());
                backupMetadata.setValue(String.valueOf(markBackup));
                receptacleType.getMetadata().add(backupMetadata);
            }
        }
    }

    private void cleanupPositionMap(PositionMapType positionMapType, PlateType plate,
            VesselTypeGeometry vesselTypeGeometry) {
        if (positionMapType != null) {
            Iterator<ReceptacleType> iterator = positionMapType.getReceptacle().iterator();
            while (iterator.hasNext()) {
                ReceptacleType next = iterator.next();
                if (next.getBarcode() == null || next.getBarcode().isEmpty()) {
                    iterator.remove();
                }
            }
            if (positionMapType.getReceptacle().isEmpty()) {
                addGlobalValidationError("There must be at least one tube in the rack.");
            }
            positionMapType.setBarcode(plate.getBarcode());
        }
    }

    public ReceptacleType findReceptacleAtPosition(PositionMapType positionMapType, String position) {
        // todo jmt create map in advance to improve performance?
        ReceptacleType receptacleTypeReturn = null;
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            if (receptacleType.getPosition().equals(position)) {
                receptacleTypeReturn = receptacleType;
                break;
            }
        }
        return receptacleTypeReturn;
    }

    /* Clears manual transfer connections */
    @HandlesEvent(CLEAR_CONNECTIONS_ACTION)
    public Resolution clearConnections() {
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    public List<StationEventType> getStationEvents() {
        return stationEvents;
    }

    public void setStationEvents(List<StationEventType> stationEvents) {
        this.stationEvents = stationEvents;
    }

    public List<LabEventType> getManualEventTypes() {
        List<LabEventType> manualLabEventTypes = new ArrayList<>();
        for (LabEventType eventType : LabEventType.values()) {
            if (eventType.getManualTransferDetails() != null) {
                manualLabEventTypes.add(eventType);
            }
        }
        return manualLabEventTypes;
    }

    public LabEventType getLabEventType() {
        return labEventType;
    }

    public LabEventType labEventTypeByIndex(int eventIndex) {
        LabEventType secondaryEvent = labEventType.getManualTransferDetails().getSecondaryEvent();
        if (secondaryEvent != null && eventIndex > 0) {
            return secondaryEvent;
        }
        return labEventType;
    }

    public String getWorkflowProcessName() {
        return workflowProcessName;
    }

    public void setWorkflowProcessName(String workflowProcessName) {
        this.workflowProcessName = workflowProcessName;
    }

    public String getWorkflowStepName() {
        return workflowStepName;
    }

    public void setWorkflowStepName(String workflowStepName) {
        this.workflowStepName = workflowStepName;
    }

    public Date getWorkflowEffectiveDate() {
        return workflowEffectiveDate;
    }

    public void setWorkflowEffectiveDate(Date workflowEffectiveDate) {
        this.workflowEffectiveDate = workflowEffectiveDate;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public WorkflowStepDef getWorkflowStepDef() {
        return workflowStepDef;
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    public void setScanIndex(Integer scanIndex) {
        this.scanIndex = scanIndex;
    }

    public void setScanSource(Boolean scanSource) {
        this.scanSource = scanSource;
    }

    public Integer getAnchorIndex() {
        return anchorIndex;
    }

    public void setAnchorIndex(Integer anchorIndex) {
        this.anchorIndex = anchorIndex;
    }

    public LabEventType.ManualTransferDetails getManualTransferDetails() {
        return manualTransferDetails;
    }

    /** For testing. */
    void setManualTransferDetails(LabEventType.ManualTransferDetails manualTransferDetails) {
        this.manualTransferDetails = manualTransferDetails;
    }

    /** For testing. */
    void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /** For testing. */
    void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    public FileBean getLimsUploadFile() {
        return limsUploadFile;
    }

    public void setLimsUploadFile(FileBean limsUploadFile) {
        this.limsUploadFile = limsUploadFile;
    }

    public LimsFileType getLimsFileType() {
        return limsFileType;
    }

    public void setLimsFileType(LimsFileType limsFileType) {
        this.limsFileType = limsFileType;
    }

    public boolean isParseLimsFile() {
        return isParseLimsFile;
    }

    public void setParseLimsFile(boolean parseLimsFile) {
        isParseLimsFile = parseLimsFile;
    }

    public boolean isUseWebCam() {
        return isUseWebCam;
    }

    public void setUseWebCam(boolean useWebCam) {
        isUseWebCam = useWebCam;
    }

    public Map<VesselPosition, Boolean> getMapPositionToDepleteFlag() {
        return mapPositionToDepleteFlag;
    }

    public void setMapPositionToDepleteFlag(Map<VesselPosition, Boolean> mapPositionToDepleteFlag) {
        this.mapPositionToDepleteFlag = mapPositionToDepleteFlag;
    }

    public Map<VesselPosition, Boolean> getMapPositionToBackupFlag() {
        return mapPositionToBackupFlag;
    }

    public void setMapPositionToBackupFlag(
            Map<VesselPosition, Boolean> mapPositionToBackupFlag) {
        this.mapPositionToBackupFlag = mapPositionToBackupFlag;
    }

    public Map<Integer, Boolean> getDepleteAll() {
        return depleteAll;
    }

    public void setDepleteAll(Map<Integer, Boolean> depleteAll) {
        this.depleteAll = depleteAll;
    }
}
