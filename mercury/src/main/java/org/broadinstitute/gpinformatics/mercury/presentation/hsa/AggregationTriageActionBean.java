package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import net.sourceforge.stripes.validation.ValidationState;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.analytics.AlignmentMetricsDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.presentation.DashboardLink;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.WaitForReviewTaskDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineEngine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.ReadGroupUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn.PDO_SEARCH_COLUMNS;


@UrlBinding(AggregationTriageActionBean.ACTION_BEAN_URL)
public class AggregationTriageActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(AggregationTriageActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/workflows/aggregation_triage.action";

    private static final String ALIGNMENT_TRIAGE_PAGE = "/hsa/workflows/aggregation/triage.jsp";

    private static final String TRIAGE_FLOWCELL_DETAIL = "/hsa/workflows/aggregation/triage_flowcell_detail.jsp";

    private static final String UPDATE_OOS_ACTION = "updateOutOfSpec";

    private static final String UPDATE_IN_SPEC = "updateInSpec";

    public static final double MAX_CONTAM = 0.01;
    public static final int MIN_COV_20 = 95;

    private List<TriageDto> passingTriageDtos = new ArrayList<>();

    private List<TriageDto> oosTriageDtos = new ArrayList<>();

    private String reworkReason;

    @Validate(required = true, on = UPDATE_OOS_ACTION)
    private String commentText;

    private OutOfSpecCommands decision;

    private List<String> selectedSamples;

    private List<TriageDto> selectedDtos = new ArrayList<>();

    private Map<String, MercurySample> mapNameToSample = new HashMap<>();

    private Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<>();

    private Map<String, LabVessel> mapKeyToPond = new HashMap<>();

    private String pdoSample;

    @Inject
    private WaitForReviewTaskDao waitForReviewTaskDao;

    @Inject
    private AlignmentMetricsDao alignmentMetricsDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private DashboardLink dashboardLink;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private FiniteStateMachineEngine finiteStateMachineEngine;

    private TriageDto dto;

    private List<WaitForReviewTask> selectedTasks;
    private Map<TriageDto, WaitForReviewTask> mapDtoToTask;


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        List<WaitForReviewTask> runningWithSamples = waitForReviewTaskDao.findRunningWithSamples();

        Map<String, WaitForReviewTask> mapSampleToTask = new HashMap<>();
        for (WaitForReviewTask task: runningWithSamples) {
            MercurySample mercurySample = task.getState().getMercurySamples().iterator().next();
            mapSampleToTask.put(mercurySample.getSampleKey(), task);
        }

        Set<String> sampleIds = mapSampleToTask.keySet();

        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(sampleIds);

        List<AlignmentMetric> bySampleAlias = alignmentMetricsDao.findAggregationBySampleAlias(sampleIds);
        Set<AlignmentMetric> alignmentMetrics = new HashSet<>(bySampleAlias);
        Map<String, AlignmentMetric> mapAliasToMetric = alignmentMetrics.stream()
                .collect(Collectors.toMap(AlignmentMetric::getReadGroup, Function.identity()));

        List<TriageDto> dtos = new ArrayList<>();
        // todo jmt optimize search column list
        Map<String, SampleData> mapIdToSampleData = sampleDataFetcher.fetchSampleDataForSamples(mercurySamples,
                PDO_SEARCH_COLUMNS);

        for (MercurySample mercurySample: mercurySamples) {
            String sampleKey = mercurySample.getSampleKey();
            String aggReadGroup = ReadGroupUtil.toAggregationReadGroupMetric(sampleKey);
            mercurySample.setSampleData(mapIdToSampleData.get(sampleKey));

            AlignmentMetric alignmentMetric = mapAliasToMetric.get(aggReadGroup);
            if (alignmentMetric == null) {
                log.debug("Failed to find alignment metric for sample in 'triage' " + sampleKey);
                continue;
            }

            TriageDto dto = createTriageDto(sampleKey, mercurySample, alignmentMetric);
            WaitForReviewTask task = mapSampleToTask.get(sampleKey);
            dto.setTaskId(task.getTaskId());
            if (task.getTaskDecision() != null) {
                dto.setTaskDecision(task.getTaskDecision());
            }
            dtos.add(dto);
        }

        Set<String> overrideSampleKeys = runningWithSamples.stream()
                .filter(Task::isOverrideOutOfSpec)
                .map(task -> task.getState().getMercurySamples())
                .flatMap(Collection::stream)
                .map(MercurySample::getSampleKey)
                .collect(Collectors.toSet());
        Map<Boolean, List<TriageDto>> mapPassToDtos = dtos.stream().collect(Collectors.partitioningBy(isAggregationInSpec(overrideSampleKeys)));
        passingTriageDtos = mapPassToDtos.get(Boolean.TRUE);
        oosTriageDtos = mapPassToDtos.get(Boolean.FALSE);

        commentText = "";
        return new ForwardResolution(ALIGNMENT_TRIAGE_PAGE);
    }

    @ValidationMethod(on = {UPDATE_IN_SPEC, UPDATE_OOS_ACTION})
    public void validateSamplesSelected() {
        if (selectedSamples.isEmpty()) {
            addValidationError("selectedIds", "Please select a sample.");
        } else {
            List<TriageDto> allDtos = ListUtils.union(passingTriageDtos, oosTriageDtos);
            for (TriageDto dto: allDtos) {
                if (dto != null) {
                    if (selectedSamples.contains(dto.getPdoSample())) {
                        selectedDtos.add(dto);
                    }
                }
            }
            Set<Long> selectedTaskIds = selectedDtos.stream().map(TriageDto::getTaskId).collect(Collectors.toSet());
            selectedTasks = waitForReviewTaskDao.findListByList(
                    WaitForReviewTask.class, WaitForReviewTask_.taskId, selectedTaskIds);
            mapDtoToTask = new HashMap<>();
            for (WaitForReviewTask waitForReviewTask: selectedTasks) {
                for (TriageDto triageDto: selectedDtos) {
                    if (triageDto.getTaskId() == waitForReviewTask.getTaskId()) {
                        mapDtoToTask.put(triageDto, waitForReviewTask);
                    }
                }
            }
        }
    }

    @ValidationMethod(on = UPDATE_OOS_ACTION, priority = 1, when = ValidationState.NO_ERRORS)
    public void validateUpdateOos() {
        Set<String> pdos = new HashSet<>();
        Set<String> labVesselBarcodes = new HashSet<>();
        for (TriageDto dto: oosTriageDtos) {
            if (dto != null) {
                if (selectedSamples.contains(dto.getPdoSample())) {
                    pdos.add(dto.getPdo());
                }
            }
        }

        mapNameToSample = mercurySampleDao.findMapIdToMercurySample(selectedSamples);

        List<ProductOrder> productOrders = productOrderDao.findListByBusinessKeys(new ArrayList<>(pdos));
        mapKeyToProductOrder = new HashMap<>();
        for (ProductOrder productOrder: productOrders) {
            for (TriageDto dto: selectedDtos) {
                if (dto.getPdo().equalsIgnoreCase(productOrder.getBusinessKey())) {
                    mapKeyToProductOrder.put(dto.getPdoSample(), productOrder);
                }
            }
        }

        for (MercurySample mercurySample: mapNameToSample.values()) {
            for (LabVessel labVessel : mercurySample.getLabVessel()) {
                LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                        = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                        LabVesselSearchDefinition.POND_LAB_EVENT_TYPES);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
                Set<LabVessel> ponds = eval.getPositions().keySet();
                if (ponds == null || ponds.isEmpty()) {
                    // TODO Probably a dev set, Ignoring since there won't be a normal topoff loop
                    continue;
                }
                LabVessel pond = ponds.iterator().next();
                labVesselBarcodes.add(pond.getLabel());
                mapKeyToPond.put(mercurySample.getSampleKey(), pond);
            }
        }
    }

    @HandlesEvent(UPDATE_OOS_ACTION)
    public Resolution updateOos() {
        if (decision == OutOfSpecCommands.OVERRIDE_IN_SPEC) {
            updateDecision(TaskDecision.Decision.OVERRIDE_TO_IN_SPEC, false);
            addMessage("Successfully saved decision.");
            return view();
        }
        String reworkReason = "Other..."; // TODO JW
        List<ReworkEjb.BucketCandidate> bucketCandidates = new ArrayList<>();
        TaskDecision.Decision taskDecision = (decision == OutOfSpecCommands.REWORK_FROM_STOCK) ? TaskDecision.Decision.REWORK : TaskDecision.Decision.TOP_OFF;
        for (String sample: mapNameToSample.keySet()) {
            ProductOrder productOrder = mapKeyToProductOrder.get(sample);
            LabVessel library = mapKeyToPond.get(sample);
            if (decision == OutOfSpecCommands.REWORK_FROM_STOCK) {
                library = mapNameToSample.get(sample).getLabVessel().iterator().next();
            }
            String lastEventName = library.getLastEventName();
            String tubeBarcode = library.getLabel();
            ReworkEjb.BucketCandidate bucketCandidate = new ReworkEjb.BucketCandidate(
                    sample, tubeBarcode, productOrder, library, lastEventName
            );
            bucketCandidates.add(bucketCandidate);
        }

        try {
            Collection<String> validationMessages = reworkEjb.addAndValidateCandidates(bucketCandidates,
                    reworkReason, commentText, getUserBean().getLoginUserName(), decision.getBucketDefName());

            if (CollectionUtils.isNotEmpty(validationMessages)) {
                for (String validationMessage : validationMessages) {
                    addGlobalValidationError(validationMessage);
                }
            } else {
                updateDecision(taskDecision, true);
                addMessage("{0} vessel(s) have been added to the {1} bucket.", bucketCandidates.size(), decision.getBucketDefName());
            }

        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
        }

        return view();
    }

    private void updateDecision(TaskDecision.Decision decision, boolean cancelMachine) {
        Date now = new Date();
        for (WaitForReviewTask selectedTask : selectedTasks) {
            TaskDecision taskDecision = selectedTask.getTaskDecision();
            if (selectedTask.getTaskDecision() == null) {
                taskDecision = new TaskDecision(decision, selectedTask,
                        commentText, now, userBean.getBspUser().getUserId(), null);
                selectedTask.setTaskDecision(taskDecision);
            } else if (taskDecision.getDecision().isEditable()) {
                taskDecision.setDecidedDate(now);
                taskDecision.setDeciderUserId(userBean.getBspUser().getUserId());
                taskDecision.setDecision(decision);
                taskDecision.setOverrideReason(commentText);
            }

            if (cancelMachine) {
                selectedTask.setStatus(Status.COMPLETE);
                selectedTask.setEndTime(now);
                selectedTask.getState().getFiniteStateMachine().setStatus(Status.CANCELLED);
                selectedTask.getState().getFiniteStateMachine().setDateCompleted(now);
            }
        }

        waitForReviewTaskDao.flush();
    }

    @HandlesEvent(UPDATE_IN_SPEC)
    public Resolution updateInSpec() {
        MessageCollection messageCollection = new MessageCollection();
        TaskDecision.Decision taskDecision = null;
        if (decision == OutOfSpecCommands.SEND_TO_CLOUD) {
            taskDecision = TaskDecision.Decision.PASS;
            for (Map.Entry<TriageDto, WaitForReviewTask> entry: mapDtoToTask.entrySet()) {
                incrementMachine(entry.getValue(), entry.getKey().getPdoSample(), messageCollection);
            }
        } else if (decision == OutOfSpecCommands.MARK_OOS) {
            taskDecision = TaskDecision.Decision.MARK_OUT_OF_SPEC;
        }
        updateDecision(taskDecision, false);
        addMessages(messageCollection);
        return view();
    }

    private void incrementMachine(WaitForReviewTask task, String sampleKey, MessageCollection messageCollection) {
        FiniteStateMachine finiteStateMachine = task.getState().getFiniteStateMachine();
        MessageCollection stateMessageCollection = new MessageCollection();
        task.setStatus(Status.COMPLETE);
        task.setEndTime(new Date());
        finiteStateMachine.setStatus(Status.RUNNING);
        finiteStateMachineEngine.incrementStateMachine(finiteStateMachine, stateMessageCollection);
        if (!stateMessageCollection.hasErrors()) {
            messageCollection.addInfo("Incremented %s sample to next state.", sampleKey);
        } else {
            messageCollection.addErrors(stateMessageCollection.getErrors());
        }
    }

    @HandlesEvent("expandSample")
    public Resolution expandSample() {
        // Fetch all flowcells/lanes for this sample and find lanes that we are waiting on.
        MercurySample mercurySample = mercurySampleDao.findBySampleKey(pdoSample);
        dto = new TriageDto();
        Set<LabVessel> labVessels = mercurySample.getLabVessel();
        LabVessel labVessel = labVessels.iterator().next();
        dto.setSampleVessel(labVessel.getLabel());

        // Grab all the aggregated lanes for this sample
        Optional<AggregationState> optAggregationState = mercurySample.getMostRecentStateOfType(AggregationState.class);
        AggregationState aggregationState = optAggregationState.get();
        Set<IlluminaSequencingRunChamber> aggregatedLanes = aggregationState.getSequencingRunChambers();

        Map<String, List<VesselPosition>> mapFlowcellBarcodeToLanes = aggregatedLanes.stream()
                .collect(Collectors.groupingBy(l -> l.getIlluminaSequencingRun().getSampleCartridge().getLabel(),
                        Collectors.mapping(IlluminaSequencingRunChamber::getLanePosition, Collectors.toList())));

        LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(LabVesselSearchDefinition.FLOWCELL_LAB_EVENT_TYPES);
        labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
        int numLanesOnFlowcells = eval.getPositions().size();
        Map<LabVessel, Collection<VesselPosition>> labVesselCollectionMap = eval.getPositions().asMap();

        Map<LabVessel, List<VesselPosition>> mapFcToMissingLanes = new HashMap<>();
        Map<LabVessel, List<VesselPosition>> mapFcToCompletedLanes = new HashMap<>();
        for (Map.Entry<LabVessel, Collection<VesselPosition>> entry: labVesselCollectionMap.entrySet()) {
            LabVessel flowcellVessel = entry.getKey();
            String fcBarcode = flowcellVessel.getLabel();
            List<VesselPosition> missingLanes = new ArrayList<>();
            List<VesselPosition> completedLanes = new ArrayList<>();
            if (!mapFlowcellBarcodeToLanes.containsKey(fcBarcode)) {
                missingLanes.addAll(entry.getValue());
            } else {
                List<VesselPosition> lanes = mapFlowcellBarcodeToLanes.get(fcBarcode);
                for (VesselPosition lane: entry.getValue()) {
                    if (!lanes.contains(lane)) {
                        missingLanes.add(lane);
                    } else {
                        completedLanes.add(lane);
                    }
                }
            }

            if (!missingLanes.isEmpty()) {
                mapFcToMissingLanes.put(flowcellVessel, missingLanes);
            }
            if (!completedLanes.isEmpty()) {
                mapFcToCompletedLanes.put(flowcellVessel, completedLanes);
            }
        }

        LabVesselSearchDefinition.VesselBatchTraverserCriteria downstreamBatchFinder =
                new LabVesselSearchDefinition.VesselBatchTraverserCriteria();
        labVessel.evaluateCriteria(downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Descendants);

        Set<LabBatch> fctSet = downstreamBatchFinder.getLabBatches().stream()
                .filter(lb -> lb.getLabBatchType() == LabBatch.LabBatchType.FCT)
                .collect(Collectors.toSet());

        dto.setNumberOfReadGroupsAggregated(aggregatedLanes.size());
        dto.setNumberOfLanesDesignated(fctSet.size());
        dto.setNumberOfReadGroupsOnFlowcell(numLanesOnFlowcells);

        List<FlowcellStatus> missingFlowcellStatus = new ArrayList<>();
        for (Map.Entry<LabVessel, List<VesselPosition>> entry: mapFcToMissingLanes.entrySet()) {
            buildFlowcellStatusInfo(downstreamBatchFinder, missingFlowcellStatus, entry);
        }

        List<FlowcellStatus> completedFlowcellStatus = new ArrayList<>();
        for (Map.Entry<LabVessel, List<VesselPosition>> entry: mapFcToCompletedLanes.entrySet()) {
            buildFlowcellStatusInfo(downstreamBatchFinder, completedFlowcellStatus, entry);
        }

        dto.setCompletedFlowcellStatuses(completedFlowcellStatus);
        dto.setMissingFlowcellStatus(missingFlowcellStatus);
        return new ForwardResolution(TRIAGE_FLOWCELL_DETAIL);
    }

    private TriageDto createTriageDto(String sampleKey, MercurySample mercurySample,
                                      AlignmentMetric alignmentMetric) {
        SampleData sampleData = mercurySample.getSampleData();
        String gender = sampleData.getGender();
        ProductOrderSample productOrderSample = findProductOrderSample(mercurySample);
        String pdo = productOrderSample.getProductOrder().getBusinessKey();

        BigDecimal value = alignmentMetric.getPctCov20x();
        String pctCov20 = value == null ? "" : value.toString();

        value = alignmentMetric.getEstimatedSampleContamination();
        String contamination = value == null ? "" : value.toString();

        TriageDto dto = new TriageDto();
        dto.setContamination(contamination);
        dto.setCoverage20x(pctCov20);
        dto.setGender(gender);
        dto.setPdo(pdo);
        dto.setPdoSample(sampleKey);
        dto.setSampleVessel(mercurySample.getLabVessel().iterator().next().getLabel());

        // Gender Concordance
        Fingerprint.Gender dragenGender = Fingerprint.Gender.byChromosome(alignmentMetric.getPredictedSexChromosomePloidy());
        Optional<Fingerprint> fluidigm = mercurySample.getFingerprints().stream()
                .filter(fingerprint -> fingerprint.getPlatform() == Fingerprint.Platform.FLUIDIGM)
                .min(Comparator.comparing(Fingerprint::getDateGenerated));
        Fingerprint.Gender fpGender = Fingerprint.Gender.UNKNOWN;
        if (fluidigm.isPresent()) {
            fpGender = fluidigm.get().getGender();
        }
        Optional<Fingerprint> arrays = mercurySample.getFingerprints().stream()
                .filter(fingerprint -> fingerprint.getPlatform() == Fingerprint.Platform.GENERAL_ARRAY)
                .min(Comparator.comparing(Fingerprint::getDateGenerated));
        Fingerprint.Gender arraysGender = Fingerprint.Gender.UNKNOWN;
        if (arrays.isPresent()) {
            arraysGender = arrays.get().getGender();
        }

        String genderConcordance = "";
        if (dragenGender != Fingerprint.Gender.UNKNOWN && fpGender != Fingerprint.Gender.UNKNOWN &&
            arraysGender != Fingerprint.Gender.UNKNOWN) {
            String concordant = Boolean.toString(dragenGender == fpGender && dragenGender == arraysGender);
            genderConcordance = String.format("%s (%s/%s/%s)", concordant, dragenGender.getChromsome(),
                    fpGender.getChromsome(), arraysGender.getChromsome());
        }

        dto.setGenderConcordance(genderConcordance);

        return dto;
    }

    private void buildFlowcellStatusInfo(LabVesselSearchDefinition.VesselBatchTraverserCriteria downstreamBatchFinder,
                                         List<FlowcellStatus> flowcellStatuses,
                                         Map.Entry<LabVessel, List<VesselPosition>> entry) {
        FlowcellStatus flowcellStatus = new FlowcellStatus();
        LabVessel flowcell = entry.getKey();
        String flowcellBarcode = flowcell.getLabel();
        flowcellStatus.setFlowcell(flowcellBarcode);
        String pendingLanes = entry.getValue().stream()
                .map(VesselPosition::name)
                .sorted()
                .collect(Collectors.joining(","));
        flowcellStatus.setPendingLanes(pendingLanes);

        LabVesselSearchDefinition.VesselBatchTraverserCriteria upstreamBatchFinder =
                new LabVesselSearchDefinition.VesselBatchTraverserCriteria();
        flowcell.getContainerRole().applyCriteriaToAllPositions(
                upstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);

        Map<String, String> mapFctToLink = downstreamBatchFinder.getLabBatches().stream()
                .filter(lb -> lb.getLabBatchType() == LabBatch.LabBatchType.FCT)
                .collect(Collectors.toMap(LabBatch::getBatchName, lb -> jiraUrl(lb.getBusinessKey())));

        flowcellStatus.setJiraTickets(mapFctToLink);
        flowcellStatus.setDashboardLink(dashboardLink.runStatusPage(flowcellBarcode));
        flowcellStatuses.add(flowcellStatus);
    }

    public static ProductOrderSample findProductOrderSample(MercurySample mercurySample) {
        ProductOrderSample productOrderSample = mercurySample.getProductOrderSamples().stream()
                .max(Comparator.comparingLong(ProductOrderSample::getProductOrderSampleId))
                .orElse(null);

        if (productOrderSample != null) {
            return productOrderSample;
        }

        Set<ProductOrderSample> productOrderSamples = new HashSet<>();
        for (LabVessel vessel: mercurySample.getLabVessel()) {
            for (SampleInstanceV2 sampleInstanceV2 : vessel.getSampleInstancesV2()) {
                for (ProductOrderSample pdoSample : sampleInstanceV2.getAllProductOrderSamples()) {
                    if (pdoSample.getProductOrder().getOrderStatus().readyForLab() &&
                        StringUtils.isNotBlank(pdoSample.getProductOrder().getProduct().getWorkflowName())) {
                        productOrderSamples.add(pdoSample);
                    }
                }
            }
        }
        return productOrderSamples.stream()
                .max(Comparator.comparingLong(ProductOrderSample::getProductOrderSampleId))
                .orElse(null);
    }

    private static Predicate<TriageDto> isAggregationInSpec(Set<String> sampleKeys) {
        return p -> sampleKeys.contains(p.getPdoSample()) || (compareLess(p.getContamination(), MAX_CONTAM) &&
                                                              compareGreater(p.getCoverage20x(), MIN_COV_20));
    }

    private static boolean compareGreater(String value, double min) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return Double.parseDouble(value) > min;
    }

    private static boolean compareLess(String value, double min) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return Double.parseDouble(value) < min;
    }

    public List<TriageDto> getPassingTriageDtos() {
        return passingTriageDtos;
    }

    public void setPassingTriageDtos(List<TriageDto> passingTriageDtos) {
        this.passingTriageDtos = passingTriageDtos;
    }

    public List<TriageDto> getOosTriageDtos() {
        return oosTriageDtos;
    }

    public void setOosTriageDtos(List<TriageDto> oosTriageDtos) {
        this.oosTriageDtos = oosTriageDtos;
    }

    public String getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(String reworkReason) {
        this.reworkReason = reworkReason;
    }

    public List<String> getSelectedSamples() {
        return selectedSamples;
    }

    public void setSelectedSamples(List<String> selectedSamples) {
        this.selectedSamples = selectedSamples;
    }

    public String getPdoSample() {
        return pdoSample;
    }

    public void setPdoSample(String pdoSample) {
        this.pdoSample = pdoSample;
    }

    public OutOfSpecCommands getDecision() {
        return decision;
    }

    public void setDecision(OutOfSpecCommands decision) {
        this.decision = decision;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public TriageDto getDto() {
        return dto;
    }

    public void setDto(TriageDto dto) {
        this.dto = dto;
    }

    public List<OutOfSpecCommands> getOutOfSpecOptions() {
        return OutOfSpecCommands.oosDecisions;
    }

    public List<OutOfSpecCommands> getInSpecOptions() {
        return OutOfSpecCommands.inSpecDecisions;
    }

    public static class TriageDto {
        private long taskId;
        private String library;
        private String pdoSample;
        private String sampleVessel;
        private String pdo;
        private String coverage20x;
        private String contamination;
        private String alignedQ20Bases;
        private String gender;
        private int numberOfReadGroupsAggregated;
        private int numberOfReadGroupsOnFlowcell;
        private int numberOfLanesDesignated;
        private String lod;
        private String genderConcordance;
        private List<FlowcellStatus> missingFlowcellStatuses;
        private List<FlowcellStatus> completedFlowcellStatuses;
        private TaskDecision taskDecision;

        public long getTaskId() {
            return taskId;
        }

        public void setTaskId(long taskId) {
            this.taskId = taskId;
        }

        public String getLibrary() {
            return library;
        }

        public void setLibrary(String library) {
            this.library = library;
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

        public String getCoverage20x() {
            return coverage20x;
        }

        public void setCoverage20x(String coverage20x) {
            this.coverage20x = coverage20x;
        }

        public String getContamination() {
            return contamination;
        }

        public void setContamination(String contamination) {
            this.contamination = contamination;
        }

        public String getAlignedQ20Bases() {
            return alignedQ20Bases;
        }

        public void setAlignedQ20Bases(String alignedQ20Bases) {
            this.alignedQ20Bases = alignedQ20Bases;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public int getNumberOfReadGroupsAggregated() {
            return numberOfReadGroupsAggregated;
        }

        public void setNumberOfReadGroupsAggregated(int numberOfReadGroupsAggregated) {
            this.numberOfReadGroupsAggregated = numberOfReadGroupsAggregated;
        }

        public int getNumberOfReadGroupsOnFlowcell() {
            return numberOfReadGroupsOnFlowcell;
        }

        public void setNumberOfReadGroupsOnFlowcell(int numberOfReadGroupsOnFlowcell) {
            this.numberOfReadGroupsOnFlowcell = numberOfReadGroupsOnFlowcell;
        }

        public int getNumberOfLanesDesignated() {
            return numberOfLanesDesignated;
        }

        public void setNumberOfLanesDesignated(int numberOfLanesDesignated) {
            this.numberOfLanesDesignated = numberOfLanesDesignated;
        }

        public String getLod() {
            return lod;
        }

        public void setLod(String lod) {
            this.lod = lod;
        }

        public List<FlowcellStatus> getMissingFlowcellStatuses() {
            return missingFlowcellStatuses;
        }

        public void setMissingFlowcellStatus(
                List<FlowcellStatus> flowcellStatuses) {
            this.missingFlowcellStatuses = flowcellStatuses;
        }

        public List<FlowcellStatus> getCompletedFlowcellStatuses() {
            return completedFlowcellStatuses;
        }

        public void setCompletedFlowcellStatuses(
                List<FlowcellStatus> completedFlowcellStatuses) {
            this.completedFlowcellStatuses = completedFlowcellStatuses;
        }

        public String getSampleVessel() {
            return sampleVessel;
        }

        public void setSampleVessel(String sampleVessel) {
            this.sampleVessel = sampleVessel;
        }

        public String getGenderConcordance() {
            return genderConcordance;
        }

        public void setGenderConcordance(String genderConcordance) {
            this.genderConcordance = genderConcordance;
        }

        public void setTaskDecision(TaskDecision taskDecision) {
            this.taskDecision = taskDecision;
        }

        public TaskDecision getTaskDecision() {
            return taskDecision;
        }
    }

    public enum OutOfSpecCommands implements Displayable {
        SEND_TO_TOP_OFFS("Send To Topoffs", "Pooling Bucket"),
        REWORK_FROM_STOCK("Rework From Stock", "Pico/Plating Bucket", IsRework.TRUE),
        OVERRIDE_IN_SPEC("Override As In Spec"),
        MARK_OOS("Mark as Out of Spec",  null, IsRework.FALSE, VisibleInSpec.TRUE),
        SEND_TO_CLOUD("Send To Cloud",  null, IsRework.FALSE, VisibleInSpec.TRUE);

        private final String displayName;
        private final String bucketDefName;
        private final IsRework isRework;
        private final VisibleInSpec visibleInSpec;

        OutOfSpecCommands(String displayName) {
            this(displayName, null, IsRework.FALSE);
        }

        OutOfSpecCommands(String displayName, String bucketDefName) {
            this(displayName, bucketDefName, IsRework.FALSE);
        }

        OutOfSpecCommands(String displayName, String bucketDefName, IsRework isRework) {
            this(displayName, bucketDefName, isRework, VisibleInSpec.FALSE);
        }

        OutOfSpecCommands(String displayName, String bucketDefName, IsRework isRework, VisibleInSpec visibleInSpec) {
            this.displayName = displayName;
            this.bucketDefName = bucketDefName;
            this.isRework = isRework;
            this.visibleInSpec = visibleInSpec;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public String getBucketDefName() {
            return bucketDefName;
        }

        public boolean getIsRework() {
            return isRework == IsRework.TRUE;
        }

        public boolean isVisibleInSpec() {
            return visibleInSpec == VisibleInSpec.TRUE;
        }

        private static List<OutOfSpecCommands> oosDecisions = new ArrayList<>();
        private static List<OutOfSpecCommands> inSpecDecisions = new ArrayList<>();

        static {
            for (OutOfSpecCommands outOfSpecCommands: OutOfSpecCommands.values()) {
                if (outOfSpecCommands.isVisibleInSpec()) {
                    inSpecDecisions.add(outOfSpecCommands);
                } else {
                    oosDecisions.add(outOfSpecCommands);
                }
            }
        }
    }

    public enum IsRework {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        IsRework(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }

    public enum VisibleInSpec {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        VisibleInSpec(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }

    public static class FlowcellStatus {
        private String flowcell;
        private String pendingLanes;
        private Map<String, String> jiraTickets;
        private List<String> hsaStatus;
        private String dashboardLink;

        public FlowcellStatus() {
        }

        public String getFlowcell() {
            return flowcell;
        }

        public void setFlowcell(String flowcell) {
            this.flowcell = flowcell;
        }

        public String getPendingLanes() {
            return pendingLanes;
        }

        public void setPendingLanes(String pendingLanes) {
            this.pendingLanes = pendingLanes;
        }

        public Map<String, String> getJiraTickets() {
            return jiraTickets;
        }

        public void setJiraTickets(Map<String, String> jiraTickets) {
            this.jiraTickets = jiraTickets;
        }

        public List<String> getHsaStatus() {
            return hsaStatus;
        }

        public void setHsaStatus(List<String> hsaStatus) {
            this.hsaStatus = hsaStatus;
        }

        public void setDashboardLink(String dashboardLink) {
            this.dashboardLink = dashboardLink;
        }

        public String getDashboardLink() {
            return dashboardLink;
        }
    }
}
