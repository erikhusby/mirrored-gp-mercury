package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;
import net.sourceforge.stripes.validation.ValidationState;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.analytics.AlignmentMetricsDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.columns.PickerVesselPlugin;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.CoverageTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.TopOffEjb;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TopOffStateMachineDecorator;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.CoverageType;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@UrlBinding(TopOffActionBean.ACTION_BEAN_URL)
public class TopOffActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(TopOffActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/workflows/topoff.action";

    private static final String TOPOFF_PAGE = "/hsa/workflows/aggregation/top_offs.jsp";
    public static final String DOWNLOAD_PICK_LIST = "downloadPickList";
    public static final String DOWNLOAD_PG_PICK_LIST = "downloadPoolGroupsPickList";
    public static final String CREATE_TOP_OFF_GROUP = "createTopOffGroup";
    public static final String SEND_TO_REWORK = "sendToRework";
    public static final String SEND_TO_HOLDING = "sendToHolding";
    public static final String REMOVE_FROM_POOL_GROUP = "removeFromPoolGroup";
    public static final String POOLING_BUCKET = "Pooling Bucket";
    public static final String CLEAR_REWORK = "clearRework";
    public static final String DOWNLOAD_POOL_GROUPS = "downloadPoolGroups";

    private List<String> selectedSamples;

    private Map<String, List<HoldForTopoffDto>> mapTabToDto = new HashMap<>();

    private Map<String, HoldForTopoffDto> mapSampleToDto = new HashMap<>();

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private AlignmentMetricsDao alignmentMetricsDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private WorkflowConfig workflowConfig;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private JiraConfig jiraConfig;

    @Inject
    private StateMachineDao stateMachineDao;

    @Inject
    private TopOffEjb topOffEjb;

    @Inject
    private CoverageTypeDao coverageTypeDao;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private ReworkReasonDao reworkReasonDao;

    @Inject
    private JiraService jiraService;

    private String summary;
    private String selectedLcset;
    private String selectedWorkflow;
    private Set<HoldForTopoffDto> selectedHoldForTopOffs;
    private String sequencingType;
    private List<PoolGroup> poolGroups = new ArrayList<>();

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution view() {
        initData();
        return new ForwardResolution(TOPOFF_PAGE);
    }

    public void initData() {
        mapTabToDto = new HashMap<>();
        poolGroups = new ArrayList<>();
        MessageCollection messageCollection = new MessageCollection();
        initBucketEntryData(messageCollection);
        TopOffStateMachineDecorator topOffStateMachine =
                topOffEjb.findOrCreateTopoffStateMachine();
        initStateMachineData(topOffStateMachine.getStateByName(TopOffStateMachineDecorator.StateNames.Nova),
                TopOffStateMachineDecorator.StateNames.Nova.getDisplayName(), messageCollection);
        initStateMachineData(topOffStateMachine.getStateByName(TopOffStateMachineDecorator.StateNames.HiSeqX),
                TopOffStateMachineDecorator.StateNames.HiSeqX.getDisplayName(), messageCollection);
        initPoolGroupData(topOffStateMachine.getPoolGroups(), messageCollection);
        initStateMachineData(topOffStateMachine.getStateByName(TopOffStateMachineDecorator.StateNames.SentToRework),
                TopOffStateMachineDecorator.StateNames.SentToRework.getDisplayName(), messageCollection);

        addMessages(messageCollection);

        // To speed requests, keep this current cache of the data for future requests
        if (mapSampleToDto.isEmpty()) {
            mapSampleToDto = new HashMap<>();
            for (List<HoldForTopoffDto> dtos : mapTabToDto.values()) {
                for (HoldForTopoffDto dto : dtos) {
                    mapSampleToDto.put(dto.getPdoSample(), dto);
                }
            }
        }
    }

    @ValidationMethod(on = "createOrAddToBatch")
    public void validateCreateOrAddBatch() {
        if (!StringUtils.isBlank(selectedLcset)) {
            LabBatch lcset = labBatchDao.findByName(selectedLcset);
            if (lcset == null) {
                addValidationError("selectedLcset", "Failed to find " + selectedLcset);
            }
        } else if (StringUtils.isBlank(summary)) {
            addValidationError("summary", "You must provide at least a summary to create a Jira Ticket.");
        }

        if (selectedSamples == null || selectedSamples.isEmpty()) {
            addValidationError("selectedSamples","Must select at least one sample");
        }

        List<HoldForTopoffDto> holdForTopoffDtos = mapTabToDto.get(TopOffStateMachineDecorator.StateNames.HoldForTopOff.getDisplayName());
        selectedHoldForTopOffs =
                holdForTopoffDtos.stream().filter(dto -> selectedSamples.contains(dto.getPdoSample()))
                        .collect(Collectors.toSet());
        Set<String> seqTypes =
                selectedHoldForTopOffs.stream().map(HoldForTopoffDto::getSeqType).collect(Collectors.toSet());
        if (seqTypes.size() > 1) {
            addValidationError("seqTypes", "Expect only one sequencing technology per batch.");
        }

        if (!getContext().getValidationErrors().isEmpty()) {
            initData();
        }
    }

    @HandlesEvent("createOrAddToBatch")
    public Resolution createOrAddToBatch() {
        LabBatch batch = null;
        String seqType = selectedHoldForTopOffs.iterator().next().getSeqType();

        List<Long> bucketEntryIds = selectedHoldForTopOffs.stream()
                .map(HoldForTopoffDto::getBucketEntryId)
                .collect(Collectors.toList());

        String batchName = selectedLcset;
        String action = "created";
        MessageCollection messageCollection = new MessageCollection();
        try {
            if (StringUtils.isBlank(selectedLcset)) {
                batch = topOffEjb.createBatchToState(bucketEntryIds, POOLING_BUCKET, this,
                        userBean.getBspUser().getUsername(), selectedSamples, seqType, selectedWorkflow, summary);

                Map<String, CustomFieldDefinition> mapNameToField = null;
                try {
                    mapNameToField = jiraService.getCustomFields(
                            LabBatch.TicketFields.TOP_OFFS.getName());
                    CustomField sequencingStationCustomField = new CustomField("Yes",
                            mapNameToField.get(LabBatch.TicketFields.TOP_OFFS.getName()));
                    jiraService.updateIssue(batch.getBusinessKey(),
                            Collections.singleton(sequencingStationCustomField));
                } catch (IOException e) {
                    String warning = "Failed to update Jira Ticket to set Topoffs field";
                    log.error(warning, e);
                    messageCollection.addWarning(warning);
                }

                batchName = "ignore";//batch.getBatchName();
            } else {
                action = "updated";
                try {
                    topOffEjb.updateBatchToState(selectedLcset, bucketEntryIds, POOLING_BUCKET,
                            this, seqType, selectedSamples);
                } catch (IOException e) {
                    addGlobalValidationError("IOException contacting JIRA service." + e.getMessage());
                    return new ForwardResolution(TOPOFF_PAGE);
                }
            }
        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return view();
        }

        if (messageCollection.hasWarnings()) {
            addMessages(messageCollection);
        }

        addMessage(MessageFormat.format("Lab batch ''{0}'' has been {1}." , batchName, action));
        return view();
    }

    @ValidationMethod(on = {DOWNLOAD_PICK_LIST, CREATE_TOP_OFF_GROUP, SEND_TO_REWORK, SEND_TO_HOLDING,
            REMOVE_FROM_POOL_GROUP, CLEAR_REWORK, DOWNLOAD_POOL_GROUPS, DOWNLOAD_PG_PICK_LIST})
    public void validateSelectedSamples() {
        if (CollectionUtils.isEmpty(selectedSamples)) {
            addGlobalValidationError("Must select at least one sample.");
            initData();
        }
    }

    @ValidationMethod(on = {CREATE_TOP_OFF_GROUP}, priority = 2, when = ValidationState.NO_ERRORS)
    public void validateIndexes () {
        mapTabToDto.get(sequencingType).stream()
            .filter(dto -> selectedSamples.contains(dto.getPdoSample()))
            .collect(Collectors.groupingBy(HoldForTopoffDto::getIndex))
            .values().stream()
            .filter(forTopoffDtos -> forTopoffDtos.size() > 1)
            .flatMap(List::stream)
            .forEach(dto -> {
                addGlobalValidationError("Can't add duplicate index to same group: " + dto.getIndex()
                                         + " for sample: " + dto.getPdoSample());
            });
        if (!getContext().getValidationErrors().isEmpty()) {
            initData();
        }
    }

    @HandlesEvent(SEND_TO_HOLDING)
    public Resolution sendToHolding() {
        TopOffStateMachineDecorator.StateNames stateNames =
                TopOffStateMachineDecorator.StateNames.getStateByName(sequencingType);

        Map<String, List<String>> mapLcsetToSamples = new HashMap<>();
        Map<String, List<String>> mapLcsetToPonds = new HashMap<>();
        for (HoldForTopoffDto dto: mapTabToDto.get(sequencingType)) {
            String sampleKey = dto.getPdoSample();
            if (selectedSamples.contains(sampleKey)) {
                if (!mapLcsetToSamples.containsKey(dto.getTopOffLcset())) {
                    mapLcsetToSamples.put(dto.getTopOffLcset(), new ArrayList<>());
                    mapLcsetToPonds.put(dto.getTopOffLcset(), new ArrayList<>());
                }

                mapLcsetToSamples.get(dto.getTopOffLcset()).add(dto.getPdoSample());
                mapLcsetToPonds.get(dto.getTopOffLcset()).add(dto.getLibrary());
            }
        }

        for (Map.Entry<String, List<String>> entry: mapLcsetToSamples.entrySet()) {
            List<String> removeBarcodes = mapLcsetToPonds.get(entry.getKey());
            labBatchEjb.updateLcsetFromScan(entry.getKey(), Collections.emptyList(),
                    this, Collections.emptyList(), removeBarcodes, removeBarcodes);
        }

        TopOffStateMachineDecorator topoffStateMachine = topOffEjb.findOrCreateTopoffStateMachine();
        State state = topoffStateMachine.getStateByName(stateNames);

        state.getMercurySamples().removeAll(mercurySampleDao.findBySampleKeys(selectedSamples));
        stateMachineDao.persist(topoffStateMachine.getFiniteStateMachine());

        return view();
    }

    @HandlesEvent(SEND_TO_REWORK)
    public Resolution sendToRework() {
        TopOffStateMachineDecorator.StateNames removeFromState =
                TopOffStateMachineDecorator.StateNames.getStateByName(sequencingType);
        List<MercurySample> samples = mercurySampleDao.findBySampleKeys(selectedSamples);

        TopOffStateMachineDecorator topoffStateMachine = topOffEjb.findOrCreateTopoffStateMachine();
        State removeState = topoffStateMachine.getStateByName(removeFromState);
        removeState.getMercurySamples().removeAll(samples);

        TopOffStateMachineDecorator.StateNames reworkStateName = TopOffStateMachineDecorator.StateNames.SentToRework;
        State reworkState = topoffStateMachine.getStateByName(reworkStateName);
        reworkState.getMercurySamples().addAll(samples);
        stateMachineDao.persist(topoffStateMachine.getFiniteStateMachine());

        return view();
    }

    @HandlesEvent(CREATE_TOP_OFF_GROUP)
    public Resolution createTopOffGroup() {
        TopOffStateMachineDecorator.StateNames stateNames =
                TopOffStateMachineDecorator.StateNames.getStateByName(sequencingType);
        topOffEjb.createPoolGroupAndDrainState(selectedSamples, getUserBean().getLoginUserName(), stateNames);
        addMessage("Created new pool group.");
        return view();
    }

    @HandlesEvent(REMOVE_FROM_POOL_GROUP)
    public Resolution removeFromPoolGroup() {
        Map<String, List<String>> mapSeqToSamples = new HashMap<>();
        for (PoolGroup poolGroup: poolGroups) {
            for (HoldForTopoffDto dto: poolGroup.getTopOffDtos()) {
                if (selectedSamples.contains(dto.getPdoSample())) {
                    mapSeqToSamples.computeIfAbsent(dto.getSeqType(), k -> new ArrayList<>()).add(dto.getPdoSample());
                }
            }
        }
        topOffEjb.drainPools(mapSeqToSamples);
        return view();
    }

    @NotNull
    private Map<String, List<String>> buildSamplesToRemoveFromPoolGroup() {
        Map<String, List<String>> mapSeqToSamples = new HashMap<>();
        for (PoolGroup poolGroup : poolGroups) {
            List<HoldForTopoffDto> topOffDtos = poolGroup.getTopOffDtos();
            for (HoldForTopoffDto dto : topOffDtos) {
                if (selectedSamples.contains(dto.getPdoSample())) {
                    mapSeqToSamples.computeIfAbsent(dto.getSeqType(), k -> new ArrayList<>()).add(dto.getPdoSample());
                }
            }
        }
        return mapSeqToSamples;
    }

    @HandlesEvent(CLEAR_REWORK)
    public Resolution clearRework() {
        List<MercurySample> samples = mercurySampleDao.findBySampleKeys(selectedSamples);

        TopOffStateMachineDecorator topoffStateMachine = topOffEjb.findOrCreateTopoffStateMachine();
        State reworkState = topoffStateMachine.getStateByName(TopOffStateMachineDecorator.StateNames.SentToRework);
        reworkState.getMercurySamples().removeAll(samples);
        stateMachineDao.persist(topoffStateMachine.getFiniteStateMachine());

        return view();
    }

    @HandlesEvent("markComplete")
    public Resolution markPoolGroupComplete() {
        Map<String, List<String>> mapSeqToSamples = buildSamplesToRemoveFromPoolGroup();
        topOffEjb.drainPools(mapSeqToSamples);
        return view();
    }

    @HandlesEvent(DOWNLOAD_POOL_GROUPS)
    public Resolution downloadPoolGroups() {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Topoffs");
            Row headerRow = sheet.createRow(0);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            CellStyle headerCellStyle = workbook.createCellStyle();

            headerCellStyle.setFont(headerFont);
            List<String> headers =
                    Arrays.asList("Group ID", "Lanes Needed", "Max X Needed", "Library", "LCSET", "Seq Type", "PDO Sample", "Index",
                            "X Needed", "Member Count", "Is Clinical?");
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerCellStyle);
            }

            int rowNum = 1;
            for (PoolGroup poolGroup : poolGroups) {
                for (HoldForTopoffDto dto : poolGroup.getTopOffDtos()) {
                    if (selectedSamples.contains(dto.getPdoSample())) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(poolGroup.getGroupId());
                        row.createCell(1).setCellValue(poolGroup.getLanesNeeded());
                        row.createCell(2).setCellValue(poolGroup.getMaxXNeeded());
                        row.createCell(3).setCellValue(dto.getLibrary());
                        row.createCell(4).setCellValue(dto.getTopOffLcset());
                        row.createCell(5).setCellValue(dto.getSeqType());
                        row.createCell(6).setCellValue(dto.getPdoSample());
                        row.createCell(7).setCellValue(dto.getIndex());
                        row.createCell(8).setCellValue(dto.getxNeeded());
                        row.createCell(9).setCellValue(poolGroup.getCount());
                        row.createCell(10).setCellValue(dto.isClinical());
                    }
                }
            }

            for(int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            String filename = "TopOffs.xls";

            // Streams the spreadsheet back to user.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                    new ByteArrayInputStream(out.toByteArray()));
            stream.setFilename(filename);
            return stream;
        } catch (IOException e) {
            String err = "Failed to create pool groups excel file";
            log.error(err, e);
            addGlobalValidationError(err);
        }

        return view();
    }

    @HandlesEvent("sendBackToSeqQueue")
    public Resolution sendBackToSeqQueue() {
        TopOffStateMachineDecorator.StateNames removeFromState =
                TopOffStateMachineDecorator.StateNames.getStateByName(sequencingType);
        List<MercurySample> samples = mercurySampleDao.findBySampleKeys(selectedSamples);

        TopOffStateMachineDecorator topoffStateMachine = topOffEjb.findOrCreateTopoffStateMachine();
        State removeState = topoffStateMachine.getStateByName(removeFromState);
        removeState.getMercurySamples().removeAll(samples);

        List<HoldForTopoffDto> holdForTopoffDtos = mapTabToDto.get(sequencingType);
        Map<String, List<HoldForTopoffDto>> mapSeqTypeToDtos = holdForTopoffDtos.stream()
                .filter(dto -> selectedSamples.contains(dto.getPdoSample()))
                .collect(Collectors.groupingBy(HoldForTopoffDto::getSeqType));
        for (Map.Entry<String, List<HoldForTopoffDto>> entry: mapSeqTypeToDtos.entrySet()) {
            TopOffStateMachineDecorator.StateNames addToStateName =
                    TopOffStateMachineDecorator.StateNames.getStateByName(entry.getKey());
            State addToState = topoffStateMachine.getStateByName(addToStateName);

            List<String> sampleList = entry.getValue().stream()
                    .map(HoldForTopoffDto::getPdoSample)
                    .collect(Collectors.toList());

            List<MercurySample> samplesToAdd = samples.stream()
                    .filter(ms -> sampleList.contains(ms.getSampleKey()))
                    .collect(Collectors.toList());

            addToState.getMercurySamples().addAll(samplesToAdd);
        }

        stateMachineDao.persist(topoffStateMachine.getFiniteStateMachine());
        return view();
    }

    @HandlesEvent(DOWNLOAD_PICK_LIST)
    public Resolution downloadPickList() {
        List<HoldForTopoffDto> holdForTopoffDtos = mapTabToDto.get(sequencingType);
        List<HoldForTopoffDto> selectedDtos = holdForTopoffDtos.stream()
                .filter(dto -> selectedSamples.contains(dto.getPdoSample()))
                .collect(Collectors.toList());
        MessageCollection messageCollection = new MessageCollection();
        Set<String> uniqueStorageLocations = new HashSet<>();
        String destinationContainer = "DEST";
        String header = "Source Rack Barcode,Source Well,Tube Barcode,Destination Rack Barcode,Destination Well";
        String rowFormat = "%s,%s,%s,%s,%s";
        List<String> xl20Rows = new ArrayList<>();
        xl20Rows.add(header);

        RackOfTubes.RackType rackType = RackOfTubes.RackType.Matrix96;
        int counter = 0;
        int rackCounter = 1;

        if (selectedDtos.isEmpty()) {
            messageCollection.addError("Please select at least one sample.");
        } else {
            List<String> libraries = selectedDtos.stream().map(HoldForTopoffDto::getLibrary).collect(Collectors.toList());
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(libraries);
            Map<String, MercurySample> mapIdToMercurySample =
                    mercurySampleDao.findMapIdToMercurySample(selectedSamples);
            for (Map.Entry<String, LabVessel> entry: mapBarcodeToVessel.entrySet()) {
                if (entry.getValue() == null) {
                    messageCollection.addError("Failed to find lab vessel " + entry.getKey());
                } else {
                    LabVessel labVessel = entry.getValue();
                    Triple<RackOfTubes, VesselPosition, String> triple =
                            PickerVesselPlugin.findStorageContainer(labVessel);
                    if (triple == null) {
                        messageCollection.addWarning("Not in storage: " + labVessel.getLabel());
                        continue;
                    }
                    BarcodedTube barcodedTube = OrmUtil.proxySafeCast(labVessel, BarcodedTube.class);
                    if (barcodedTube.getTubeType().getAutomationName().contains("Matrix")) {
                        RackOfTubes rack = triple.getLeft();
                        VesselPosition vesselPosition = triple.getMiddle();
                        uniqueStorageLocations.add(triple.getRight());
                        VesselPosition destinationPosition = rackType.getVesselGeometry().getVesselPositions()[counter];
                        xl20Rows.add(
                                String.format(rowFormat, rack.getLabel(), vesselPosition.name(), labVessel.getLabel(),
                                        destinationContainer, destinationPosition.name()));

                        counter++;
                        if (counter >= rackType.getVesselGeometry().getCapacity()) {
                            counter = 0;
                            rackCounter++;
                            destinationContainer = "DEST" + rackCounter;
                        }
                    }
                }
            }
        }

        messageCollection.addInfo("Unique Racks: " + StringUtils.join(uniqueStorageLocations, ","));
        if (!messageCollection.hasErrors()) {
            try {
                String csv = StringUtils.join(xl20Rows, '\n');
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(csv.getBytes());
                StreamingResolution stream = new StreamingResolution("text/csv", new ByteArrayInputStream(
                        out.toByteArray()));
                String fileDateTime = DateUtils.getFileDateTime(new Date());
                stream.setFilename("Topoff_" + fileDateTime + ".csv");
                return stream;
            } catch (Exception e) {
                messageCollection.addError("Failed to create a csv");
            }
        }

        addMessages(messageCollection);

        return new ForwardResolution(TOPOFF_PAGE);
    }

    private void initBucketEntryData(MessageCollection messageCollection) {
        List<HoldForTopoffDto> holdForTopoffDtos = new ArrayList<>();
        Bucket poolingBucket = bucketDao.findByName(POOLING_BUCKET);
        List<BucketEntry> poolingBucketEntries = bucketEntryDao.findBucketEntries(poolingBucket,
                new ArrayList<>(), new ArrayList<>());

        Set<BucketEntry> bucketEntrySet = new HashSet<>();
        Set<String> sampleIds = new HashSet<>();
        Map<LabVessel, String> mapVesselToSample = new HashMap<>();
        Map<LabVessel, String> mapVesselToSeqType = new HashMap<>();
        Map<LabVessel, String> mapVesselToLcset = new HashMap<>();
        for (BucketEntry bucketEntry: poolingBucketEntries) {
            LabVessel labVessel = bucketEntry.getLabVessel();
            for (SampleInstanceV2 sampleInstanceV2: labVessel.getSampleInstancesV2()) {
                ProductOrderSample productOrderSample = grabProductOrderSample(sampleInstanceV2, bucketEntry);
                if (productOrderSample != null) {
                    String sampleKey = productOrderSample.getSampleKey();
                    sampleIds.add(sampleKey);
                    mapVesselToSample.put(labVessel, sampleKey);
                    bucketEntrySet.add(bucketEntry);
                    MercurySample mercurySample = productOrderSample.getMercurySample();

                    List<LabBatch> lcSetBatches = new ArrayList<>();
                    for (LabBatchStartingVessel labBatchStartingVessel :
                            sampleInstanceV2.getAllBatchVessels(LabBatch.LabBatchType.WORKFLOW)) {
                        lcSetBatches.add(labBatchStartingVessel.getLabBatch());
                    }
                    if (lcSetBatches.size() > 1 && sampleInstanceV2.getSingleBatch() == null) {
                        messageCollection.addError(
                                String.format("Expected one LabBatch but found %s.", lcSetBatches.size()));
                        continue;
                    }
                    String lcSet = null;
                    if (sampleInstanceV2.getSingleBatch() != null) {
                        lcSet = sampleInstanceV2.getSingleBatch().getBatchName();
                    }

                    mapVesselToLcset.put(labVessel, lcSet);
                    IlluminaFlowcell.FlowcellType latestFlowcellForSample = topOffEjb.getLatestFlowcellForSample(mercurySample);
                    mapVesselToSeqType.put(labVessel, latestFlowcellForSample.getSequencerModel());
                }
            }
        }

        Map<String, AlignmentMetric> mapSampleToMetrics = alignmentMetricsDao.findMapBySampleAlias(sampleIds);

        for (BucketEntry bucketEntry: bucketEntrySet) {
            LabVessel labVessel = bucketEntry.getLabVessel();
            Set<String> molecularIndexes = new HashSet<>();
            for (SampleInstanceV2 sampleInstanceV2: labVessel.getSampleInstancesV2()) {
                MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
                if (molecularIndexingScheme != null) {
                    molecularIndexes.add(molecularIndexingScheme.getName());
                }
            }
            HoldForTopoffDto holdForTopoffDto = new HoldForTopoffDto();
            holdForTopoffDto.setIndex(StringUtils.join(molecularIndexes, ","));
            holdForTopoffDto.setPdoSample(mapVesselToSample.get(labVessel));
            ProductOrder productOrder = bucketEntry.getProductOrder();
            holdForTopoffDto.setPdo(productOrder.getBusinessKey());
            holdForTopoffDto.setLibrary(labVessel.getLabel());
            holdForTopoffDto.setSeqType( mapVesselToSeqType.get(labVessel));
            holdForTopoffDto.setLcset(mapVesselToLcset.get(labVessel));
            holdForTopoffDto.setBucketEntryId(bucketEntry.getBucketEntryId());
            AlignmentMetric alignmentMetric = mapSampleToMetrics.get(mapVesselToSample.get(labVessel));

            BigDecimal xNeeded = calculateXNeeded(productOrder, alignmentMetric, messageCollection);
            if (xNeeded != null) {
                holdForTopoffDto.setxNeeded(xNeeded.floatValue());
                holdForTopoffDtos.add(holdForTopoffDto);
            }
        }

        mapTabToDto.put(TopOffStateMachineDecorator.StateNames.HoldForTopOff.getDisplayName(), holdForTopoffDtos);
    }

    private ProductOrderSample grabProductOrderSample(SampleInstanceV2 sampleInstanceV2,
                                                      BucketEntry bucketEntry) {
        if (sampleInstanceV2.getProductOrderSampleForSingleBucket() != null) {
            return sampleInstanceV2.getProductOrderSampleForSingleBucket();
        }
        for (ProductOrderSample productOrderSample: sampleInstanceV2.getAllProductOrderSamples()) {
            if (productOrderSample.getProductOrder().equals(bucketEntry.getProductOrder())) {
                return productOrderSample;
            }
        }
        return null;
    }

    private BigDecimal calculateXNeeded(ProductOrder productOrder, AlignmentMetric metric, MessageCollection messageCollection) {
        String coverageTypeKey = productOrder.getCoverageTypeKey();
        CoverageType coverageType = coverageTypeDao.findByBusinessKey(coverageTypeKey);
        if (coverageType != null) {
            if (coverageType.getMeanCoverage() == null) {
                messageCollection.addError("No mean coverage Specified for " + productOrder.getBusinessKey());
            } else if (coverageType.getMeanCoverage().compareTo(metric.getAverageAlignmentCoverage()) > 0) {
                return coverageType.getMeanCoverage().subtract(metric.getAverageAlignmentCoverage());
            } else {
                messageCollection.addError("PDO Sample above coverage: " + metric.getSampleAlias());
                return BigDecimal.ZERO;
            }
        } else {
            messageCollection.addError("No Coverage Specified for " + productOrder.getBusinessKey());
        }

        return null;
    }

    private void initStateMachineData(State state, String tabName, MessageCollection messageCollection) {
        Map<String, AlignmentMetric> mapSampleToMetrics = alignmentMetricsDao.findMapByMercurySample(state.getMercurySamples());
        List<HoldForTopoffDto> dtos = new ArrayList<>();
        for (MercurySample mercurySample: state.getMercurySamples()) {
            HoldForTopoffDto dto = parseSampleData(mapSampleToMetrics, mercurySample, messageCollection);
            dtos.add(dto);
        }
        mapTabToDto.put(tabName, dtos);
    }

    @NotNull
    private HoldForTopoffDto parseSampleData(Map<String, AlignmentMetric> mapSampleToMetrics,
                                             MercurySample mercurySample, MessageCollection messageCollection) {
        if (mapSampleToDto.containsKey(mercurySample.getSampleKey())) {
            return mapSampleToDto.get(mercurySample.getSampleKey());
        }
        LabVessel labVessel = mercurySample.getLabVessel().iterator().next();
        LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                LabVesselSearchDefinition.POND_LAB_EVENT_TYPES);
        labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
        Set<LabVessel> ponds = eval.getPositions().keySet();
        LabVessel pond = ponds.iterator().next();

        HoldForTopoffDto dto = new HoldForTopoffDto();

        Set<String> molecularIndexes = new HashSet<>();
        String lcset = null;
        String topOffLcset = null;
        ProductOrder productOrder = null;
        for (SampleInstanceV2 sampleInstanceV2: pond.getSampleInstancesV2()) {
            MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
            if (molecularIndexingScheme != null) {
                molecularIndexes.add(molecularIndexingScheme.getName());
            }

            if (sampleInstanceV2.getSingleBatch() != null) {
                lcset = sampleInstanceV2.getSingleBatch().getBatchName();
            }
            List<LabBatch> allWorkflowBatches = sampleInstanceV2.getAllWorkflowBatches();
            List<String> sortedLcsets =
                    allWorkflowBatches.stream().map(LabBatch::getBatchName).sorted().collect(Collectors.toList());
            topOffLcset = sortedLcsets.get(sortedLcsets.size() - 1);
            ProductOrderSample productOrderSample = sampleInstanceV2.getProductOrderSampleForSingleBucket();
            if (productOrderSample != null) {
                productOrder = productOrderSample.getProductOrder();
            } else {
                for (ProductOrderSample pdoSample: sampleInstanceV2.getAllProductOrderSamples()) {
                    if (pdoSample.getSampleKey().equals(mercurySample.getSampleKey())) {
                        productOrder = pdoSample.getProductOrder();
                        break;
                    }
                }
            }
        }
        Boolean isClinical = false;
        if (productOrder == null) {
            messageCollection.addError("Failed to find product order for " + mercurySample.getSampleKey());
        } else {
            dto.setPdo(productOrder.getBusinessKey());
            AlignmentMetric alignmentMetric = mapSampleToMetrics.get(mercurySample.getSampleKey());
            BigDecimal xNeeded = calculateXNeeded(productOrder, alignmentMetric, messageCollection);
            if (xNeeded != null) {
                dto.setxNeeded(xNeeded.floatValue());
            }
            isClinical = !productOrder.getResearchProject().isResearchOnly();
        }


        IlluminaFlowcell.FlowcellType latestFlowcellForSample = topOffEjb.getLatestFlowcellForSample(mercurySample);
        dto.setSeqType(latestFlowcellForSample.getSequencerModel());
        dto.setPdoSample(mercurySample.getSampleKey());
        dto.setIndex(StringUtils.join(molecularIndexes, ","));
        dto.setLcset(lcset);
        dto.setTopOffLcset(topOffLcset);
        dto.setLibrary(pond.getLabel());
        dto.setVolume(pond.getVolume());
        dto.setStorage(pond.getStorageLocationStringify());
        dto.setClinical(isClinical);
        return dto;
    }

    private void initPoolGroupData(List<State> poolGroupStates, MessageCollection messageCollection) {
        List<MercurySample> mercurySamples = poolGroupStates.stream()
                .map(State::getMercurySamples)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        Map<String, AlignmentMetric> alignmentMetricsMap = alignmentMetricsDao.findMapByMercurySample(mercurySamples);

        for (State state: poolGroupStates) {
            PoolGroup poolGroup = new PoolGroup();
            poolGroup.setGroupId(state.getStateName());
            poolGroup.setCount(state.getMercurySamples().size());
            Float maxXNeeded = null;
            List<HoldForTopoffDto> dtos = new ArrayList<>();
            Integer expectedYieldPerLane = 25;
            for (MercurySample mercurySample: state.getMercurySamples()) {
                HoldForTopoffDto dto = parseSampleData(alignmentMetricsMap, mercurySample, messageCollection);
                dtos.add(dto);
                if (maxXNeeded == null || dto.getxNeeded() > maxXNeeded) {
                    maxXNeeded = dto.getxNeeded();
                }
                IlluminaFlowcell.FlowcellType flowcellType = topOffEjb.getLatestFlowcellForSample(mercurySample);
                if (flowcellType.getExpectedYieldPerLane() != null) {
                    expectedYieldPerLane = flowcellType.getExpectedYieldPerLane();
                }
            }
            poolGroup.setDefaultExpectedYieldPerLane(expectedYieldPerLane);
            poolGroup.setTopOffDtos(dtos);
            if (!dtos.isEmpty()) {
                poolGroup.setMaxXNeeded(maxXNeeded);
                poolGroups.add(poolGroup);
            }
        }
    }

    public Set<String> getAvailableWorkflows() {
        Set<String> workflows = new TreeSet<>();
        List<ProductWorkflowDef> productWorkflowDefs = workflowConfig.getProductWorkflowDefs();
        for (ProductWorkflowDef productWorkflowDef : productWorkflowDefs) {
            workflows.add(productWorkflowDef.getName());
        }
        return workflows;
    }

    public List<String> getSelectedSamples() {
        return selectedSamples;
    }

    public void setSelectedSamples(List<String> selectedSamples) {
        this.selectedSamples = selectedSamples;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSelectedLcset() {
        return selectedLcset;
    }

    public void setSelectedLcset(String selectedLcset) {
        this.selectedLcset = selectedLcset;
    }

    public String getSelectedWorkflow() {
        return selectedWorkflow;
    }

    public void setSelectedWorkflow(String selectedWorkflow) {
        this.selectedWorkflow = selectedWorkflow;
    }

    public List<HoldForTopoffDto> getTabData(String tabName) {
        return mapTabToDto.get(tabName);
    }

    public String getSequencingType() {
        return sequencingType;
    }

    public void setSequencingType(String sequencingType) {
        this.sequencingType = sequencingType;
    }

    public Map<String, List<HoldForTopoffDto>> getMapTabToDto() {
        return mapTabToDto;
    }

    public void setMapTabToDto(Map<String, List<HoldForTopoffDto>> mapTabToDto) {
        this.mapTabToDto = mapTabToDto;
    }

    public Map<String, HoldForTopoffDto> getMapSampleToDto() {
        return mapSampleToDto;
    }

    public void setMapSampleToDto(
            Map<String, HoldForTopoffDto> mapSampleToDto) {
        this.mapSampleToDto = mapSampleToDto;
    }

    public List<PoolGroup> getPoolGroups() {
        return poolGroups;
    }

    public void setPoolGroups(List<PoolGroup> poolGroups) {
        this.poolGroups = poolGroups;
    }

    public static class PoolGroup {
        private String groupId;
        private int count;
        private float lanesNeeded;
        private float maxXNeeded;
        private int defaultExpectedYieldPerLane;
        private List<HoldForTopoffDto> topOffDtos;

        public PoolGroup() {
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public float getLanesNeeded() {
            return lanesNeeded;
        }

        public void setLanesNeeded(float lanesNeeded) {
            this.lanesNeeded = lanesNeeded;
        }

        public float getMaxXNeeded() {
            return maxXNeeded;
        }

        public void setMaxXNeeded(float maxXNeeded) {
            this.maxXNeeded = maxXNeeded;
        }

        public List<HoldForTopoffDto> getTopOffDtos() {
            return topOffDtos;
        }

        public void setTopOffDtos(
                List<HoldForTopoffDto> topOffDtos) {
            this.topOffDtos = topOffDtos;
        }

        public int getDefaultExpectedYieldPerLane() {
            return defaultExpectedYieldPerLane;
        }

        public void setDefaultExpectedYieldPerLane(int defaultExpectedYieldPerLane) {
            this.defaultExpectedYieldPerLane = defaultExpectedYieldPerLane;
        }
    }

    public static class HoldForTopoffDto {
        private String seqType;
        private String pdoSample;
        private String pdo;
        private String library;
        private float xNeeded;
        private String index;
        private String lcset;
        private String topOffLcset;
        private BigDecimal volume;
        private String storage;
        private boolean clinical;
        private Long bucketEntryId;

        public HoldForTopoffDto() {
        }

        public String getPdoSample() {
            return pdoSample;
        }

        public void setPdoSample(String pdoSample) {
            this.pdoSample = pdoSample;
        }

        public String getPdo() {
            return pdo;
        }

        public void setPdo(String pdo) {
            this.pdo = pdo;
        }

        public String getLibrary() {
            return library;
        }

        public void setLibrary(String library) {
            this.library = library;
        }

        public float getxNeeded() {
            return xNeeded;
        }

        public void setxNeeded(float xNeeded) {
            this.xNeeded = xNeeded;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getSeqType() {
            return seqType;
        }

        public void setSeqType(String seqType) {
            this.seqType = seqType;
        }

        public String getLcset() {
            return lcset;
        }

        public void setLcset(String lcset) {
            this.lcset = lcset;
        }

        public Long getBucketEntryId() {
            return bucketEntryId;
        }

        public void setBucketEntryId(Long bucketEntryId) {
            this.bucketEntryId = bucketEntryId;
        }

        public BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }

        public String getStorage() {
            return storage;
        }

        public void setStorage(String storage) {
            this.storage = storage;
        }

        public String getTopOffLcset() {
            return topOffLcset;
        }

        public void setTopOffLcset(String topOffLcset) {
            this.topOffLcset = topOffLcset;
        }

        public boolean isClinical() {
            return clinical;
        }

        public void setClinical(boolean clinical) {
            this.clinical = clinical;
        }
    }
}
