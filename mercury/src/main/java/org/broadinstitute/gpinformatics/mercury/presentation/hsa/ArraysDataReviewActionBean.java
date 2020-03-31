package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections4.CollectionUtils;
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
import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcContamination;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcGtConcordance;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ControlEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.TaskDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.WaitForReviewTaskDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineEngine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean.OTHER_REASON_REFERENCE;

@UrlBinding(ArraysDataReviewActionBean.ACTION_BEAN_URL)
public class ArraysDataReviewActionBean extends CoreActionBean {

    private static final Log log = LogFactory.getLog(ArraysDataReviewActionBean.class);

    public static final String REVIEW_PAGE = "/hsa/workflows/array/review.jsp";
    public static final String ACTION_BEAN_URL = "/hsa/workflows/array_review.action";
    public static final String DECIDE_ACTION = "decide";
    private static final String INFINIUM_BUCKET = "Infinium Bucket";

    @Inject
    private WaitForReviewTaskDao waitForReviewTaskDao;

    @Inject
    private ArraysQcDao arraysQcDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private FiniteStateMachineEngine finiteStateMachineEngine;

    @Inject
    private ControlEjb controlEjb;

    @Inject
    private ReworkReasonDao reworkReasonDao;

    @Inject
    private TaskDao taskDao;

    private List<MetricsDto> metricsDtos = new ArrayList<>();

    private List<String> selectedSamples = new ArrayList<>();

    private ArraysReviewDecision decision;

    private String reworkReason;

    private String commentText;

    private String userReworkReason;

    private MessageCollection messageCollection = new MessageCollection();

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        metricsDtos = new ArrayList<>();
        List<WaitForReviewTask> tasks = waitForReviewTaskDao.findAllByStatus(Status.RUNNING);

        Set<String> chipWells = tasks.stream().map(Task::getState)
                .map(State::getLabVessels)
                .flatMap(Collection::stream)
                .map(lv -> toChipWell(lv.getLabel()))
                .collect(Collectors.toSet());

        List<ArraysQc> arraysQc = arraysQcDao.findByBarcodes(new ArrayList<>(chipWells));
        Map<String, ArraysQc> mapChipWellToMetric =
                arraysQc.stream().collect(Collectors.toMap(ArraysQc::getChipWellBarcode, Function.identity()));

        for (WaitForReviewTask task: tasks) {
            Set<LabVessel> labVessels = task.getState().getLabVessels();
            if (labVessels.isEmpty()) {
                // Sequencing Data Review
                continue;
            }
            LabVessel labVessel = labVessels.iterator().next();
            PlateWell plateWell = OrmUtil.proxySafeCast(labVessel, PlateWell.class);
            createDto(plateWell, mapChipWellToMetric.get(toChipWell(plateWell.getLabel())));
        }

        // Lookup controls
        Map<String, MetricsDto> mapSampleToMetric = metricsDtos.stream()
                .filter(metric -> StringUtils.isBlank(metric.getPdoSampleName()))
                .collect(Collectors.toMap(MetricsDto::getNearestSampleName, Function.identity()));
        List<String> controlSamples = mapSampleToMetric.values().stream()
                .map(MetricsDto::getNearestSampleName)
                .collect(Collectors.toList());
        Map<String, SampleData> mapSampleToData = sampleDataFetcher.fetchSampleData(controlSamples);
        for (Map.Entry<String, SampleData> entry: mapSampleToData.entrySet()) {
            SampleData sampleData = entry.getValue();
            Control control = controlEjb.evaluateAsControl(sampleData);
            if (control != null) {
                MetricsDto dto = mapSampleToMetric.get(entry.getKey());
                dto.setPdoSampleName(control.getType().getDisplayName());
            } else {
                String errMsg = "Found a sample with no PDO and is not a control " + entry.getKey();
                log.error(errMsg);
                messageCollection.addError(errMsg);
            }
        }

        addMessages(messageCollection);
        return new ForwardResolution(REVIEW_PAGE);
    }

    @ValidationMethod(on = DECIDE_ACTION)
    public void validateSamplesSelected() {
        if (selectedSamples.isEmpty()) {
            addValidationError("selectedSamples", "Must select at least one sample.");
        }

        if (decision == ArraysReviewDecision.REWORK) {
            if (reworkReason == null) {
                addValidationError("reworkReason", "A reason is required for rework vessels");
            } else {
                if (reworkReason.equals(OTHER_REASON_REFERENCE) && StringUtils.isBlank(userReworkReason)) {
                    addValidationError("reworkReason",
                            "When choosing 'Other...' for a reason, you must enter an alternate reason");
                }
            }

            if (StringUtils.isEmpty(commentText)) {
                addValidationError("commentText", "A Comment is required for rework vessels");
            }
        }
    }

    @HandlesEvent(DECIDE_ACTION)
    public Resolution decide() {
        Map<String, WaitForReviewTask> mapSampleToTask = new HashMap<>();
        List<Task> controlTasks = new ArrayList<>();
        List<WaitForReviewTask> tasks = waitForReviewTaskDao.findAllByStatus(Status.RUNNING);

        List<MetricsDto> selectedDtos = metricsDtos.stream()
                .filter(Objects::nonNull)
                .filter(metricsDto -> selectedSamples.contains(metricsDto.getPdoSampleName()))
                .collect(Collectors.toList());

        List<MetricsDto> selectedControls = metricsDtos.stream()
                .filter(metric -> metric.getPdoSampleName() == null)
                .collect(Collectors.toList());

        for (WaitForReviewTask task: tasks) {
            LabVessel labVessel = task.getState().getLabVessels().iterator().next();
            for (MetricsDto metricsDto: selectedDtos) {
                if (labVessel.getLabel().equals(metricsDto.getChipWellBarcode())) {
                    mapSampleToTask.put(metricsDto.getPdoSampleName(), task);
                }
            }
        }

        for (WaitForReviewTask task: tasks) {
            LabVessel labVessel = task.getState().getLabVessels().iterator().next();
            for (MetricsDto metricsDto: selectedControls) {
                if (labVessel.getLabel().equals(metricsDto.getChipWellBarcode())) {
                    controlTasks.add(task);
                }
            }
        }

        if (decision == ArraysReviewDecision.READY_TO_DELIVER) {
            for (Map.Entry<String, WaitForReviewTask> entry: mapSampleToTask.entrySet()) {
                incrementMachine(entry);
            }
        } else {
            List<ReworkEjb.BucketCandidate> bucketCandidates = new ArrayList<>();

            Map<String, MercurySample> mapNameToSample =
                    mercurySampleDao.findMapIdToMercurySample(selectedSamples);

            Set<String> pdoNames = selectedDtos.stream().map(MetricsDto::getProductOrder).collect(Collectors.toSet());
            List<ProductOrder> productOrders = productOrderDao.findListByBusinessKeys(pdoNames);
            Map<String, ProductOrder> mapNameToPdo =
                    productOrders.stream().collect(Collectors.toMap(ProductOrder::getBusinessKey, Function.identity()));

            for (MetricsDto dto : selectedDtos) {
                String sample = dto.getPdoSampleName();
                MercurySample mercurySample = mapNameToSample.get(sample);
                ProductOrder productOrder = mapNameToPdo.get(dto.getProductOrder());
                LabVessel library = mercurySample.getLabVessel().iterator().next();
                String lastEventName = library.getLastEventName();
                String tubeBarcode = library.getLabel();
                ReworkEjb.BucketCandidate bucketCandidate = new ReworkEjb.BucketCandidate(
                        sample, tubeBarcode, productOrder, library, lastEventName
                );
                bucketCandidates.add(bucketCandidate);
            }

            try {
                Collection<String> validationMessages = reworkEjb.addAndValidateCandidates(bucketCandidates,
                        reworkReason, commentText, getUserBean().getLoginUserName(), INFINIUM_BUCKET);

                if (CollectionUtils.isNotEmpty(validationMessages)) {
                    for (String validationMessage : validationMessages) {
                        messageCollection.addError(validationMessage);
                    }
                } else {
                    List<Task> tasksToUpdate = new ArrayList<>();
                    for (WaitForReviewTask task : mapSampleToTask.values()) {
                        task.setStatus(Status.COMPLETE);
                        task.setEndTime(new Date());
                        FiniteStateMachine finiteStateMachine = task.getState().getFiniteStateMachine();
                        finiteStateMachine.setStatus(Status.CANCELLED);
                        tasksToUpdate.add(task);
                    }
                    taskDao.persistAll(tasksToUpdate);
                    taskDao.flush();
                    addMessage("{0} vessel(s) have been added to the {1} bucket.", bucketCandidates.size(),
                            INFINIUM_BUCKET);
                }

            } catch (ValidationException e) {
                log.error("Failed to add to bucket", e);
                messageCollection.addError(e.getMessage());
            }
        }

        if (!messageCollection.hasErrors()) {
            // Complete Controls
            for (Task task: controlTasks) {
                task.setStatus(Status.COMPLETE);
                task.setEndTime(new Date());
                task.getState().getFiniteStateMachine().setStatus(Status.COMPLETE);
            }

            if (!controlTasks.isEmpty()) {
                taskDao.persistAll(controlTasks);
                taskDao.flush();
            }
        }

        return view();
    }

    private void incrementMachine(Map.Entry<String, WaitForReviewTask> entry) {
        WaitForReviewTask task = entry.getValue();
        String sampleKey = entry.getKey();
        FiniteStateMachine finiteStateMachine = task.getState().getFiniteStateMachine();
        MessageCollection stateMessageCollection = new MessageCollection();
        task.setStatus(Status.COMPLETE);
        task.setEndTime(new Date());
        finiteStateMachineEngine.incrementStateMachine(finiteStateMachine, stateMessageCollection);
        if (!stateMessageCollection.hasErrors()) {
            messageCollection.addInfo("Moved %s sample to the review state.", sampleKey);
        } else {
            messageCollection.addErrors(stateMessageCollection.getErrors());
        }
    }

    private String toChipWell(String chipWellBarcode) {
        int indexOf = chipWellBarcode.indexOf('R');
        return chipWellBarcode.substring(0, indexOf) + "_" + chipWellBarcode.substring(indexOf);
    }

    private void createDto(PlateWell plateWell, ArraysQc arraysQc) {
        MetricsDto dto = new MetricsDto();

        if (arraysQc == null) {
            messageCollection.addError("Failed to find arrays qc for " + plateWell.getLabel());
            return;
        } else {
            dto.setChipWellBarcode(plateWell.getLabel());
            String value = null;
            value = ColumnValueType.THREE_PLACE_DECIMAL.format(
                    arraysQc.getCallRate().multiply(BigDecimal.valueOf(100)), "");
            dto.setCallRate(value);

            for (ArraysQcGtConcordance arraysQcGtConcordance: arraysQc.getArraysQcGtConcordances()) {
                if (arraysQcGtConcordance.getVariantType().equals("SNP")) {
                    value = ColumnValueType.THREE_PLACE_DECIMAL.format(
                            arraysQcGtConcordance.getGenotypeConcordance().multiply(BigDecimal.valueOf(100)), "");
                }
            }
            dto.setHapMapConcordance(value);

            value = ColumnValueType.THREE_PLACE_DECIMAL.format(arraysQc.getHetPct().multiply(BigDecimal.valueOf(100)), "");
            dto.setHetPct(value);

            ArraysQcContamination qcContamination = arraysQc.getArraysQcContamination();
            if (qcContamination != null) {
                value = ColumnValueType.TWO_PLACE_DECIMAL.format(qcContamination.getPctMix().multiply(BigDecimal.valueOf(100)), "");
                dto.setContamination(value);
            }

            dto.setGenderConcordance(arraysQc.getGenderConcordancePf());
        }

        SampleInstanceV2 sampleInstance = plateWell.getSampleInstancesV2().iterator().next();
        dto.setNearestSampleName(sampleInstance.getNearestMercurySampleName());
        ProductOrderSample productOrderSampleForSingleBucket = sampleInstance.getProductOrderSampleForSingleBucket();
        if (productOrderSampleForSingleBucket != null) {
            dto.setProductOrderSample(productOrderSampleForSingleBucket);
            dto.setProductOrder(productOrderSampleForSingleBucket.getProductOrder().getBusinessKey());
        }
        dto.setSampleInstance(sampleInstance);
        if (dto.getProductOrderSample() != null) {
            dto.setPdoSampleName(dto.getProductOrderSample().getSampleKey());
            LabVessel labVessel = dto.getProductOrderSample().getMercurySample().getLabVessel().iterator().next();
            dto.setTryCount(calculateTryCount(labVessel));
        }
        metricsDtos.add(dto);
    }

    public int calculateTryCount(LabVessel labVessel) {
        TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                = new TransferTraverserCriteria.VesselForEventTypeCriteria(Collections.singletonList(LabEventType.INFINIUM_HYBRIDIZATION), true);
        labVessel.evaluateCriteria(eventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        return eventTypeCriteria.getVesselsForLabEventType().size() - 1;
    }

    public List<ReworkReason> getAllReworkReasons() {
        return reworkReasonDao.findAll();
    }

    public List<MetricsDto> getMetricsDtos() {
        return metricsDtos;
    }

    public void setMetricsDtos(List<MetricsDto> metricsDtos) {
        this.metricsDtos = metricsDtos;
    }

    public List<String> getSelectedSamples() {
        return selectedSamples;
    }

    public void setSelectedSamples(List<String> selectedSamples) {
        this.selectedSamples = selectedSamples;
    }

    public ArraysReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(ArraysReviewDecision decision) {
        this.decision = decision;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(String reworkReason) {
        this.reworkReason = reworkReason;
    }

    public String getUserReworkReason() {
        return userReworkReason;
    }

    public void setUserReworkReason(String userReworkReason) {
        this.userReworkReason = userReworkReason;
    }

    public static class MetricsDto {
        private String pdoSampleName;
        private String callRate;
        private String hetPct;
        private String contamination;
        private String hapMapConcordance;
        private ProductOrderSample productOrderSample;
        private SampleInstanceV2 sampleInstance;
        private String nearestSampleName;
        private String productOrder;
        private String chipWellBarcode;
        private String genderConcordance;
        private int tryCount;

        public MetricsDto() {
        }

        public String getPdoSampleName() {
            return pdoSampleName;
        }

        public void setPdoSampleName(String sampleName) {
            this.pdoSampleName = sampleName;
        }

        public String getCallRate() {
            return callRate;
        }

        public void setCallRate(String callRate) {
            this.callRate = callRate;
        }

        public String getHetPct() {
            return hetPct;
        }

        public void setHetPct(String hetPct) {
            this.hetPct = hetPct;
        }

        public String getContamination() {
            return contamination;
        }

        public void setContamination(String contamination) {
            this.contamination = contamination;
        }

        public String getHapMapConcordance() {
            return hapMapConcordance;
        }

        public void setHapMapConcordance(String hapMapConcordance) {
            this.hapMapConcordance = hapMapConcordance;
        }

        public ProductOrderSample getProductOrderSample() {
            return productOrderSample;
        }

        public void setProductOrderSample(ProductOrderSample productOrderSample) {
            this.productOrderSample = productOrderSample;
        }

        public void setSampleInstance(SampleInstanceV2 sampleInstance) {
            this.sampleInstance = sampleInstance;
        }

        public SampleInstanceV2 getSampleInstance() {
            return sampleInstance;
        }

        public void setNearestSampleName(String nearestSampleName) {
            this.nearestSampleName = nearestSampleName;
        }

        public String getNearestSampleName() {
            return nearestSampleName;
        }

        public void setProductOrder(String productOrder) {
            this.productOrder = productOrder;
        }

        public String getProductOrder() {
            return productOrder;
        }

        public String getChipWellBarcode() {
            return chipWellBarcode;
        }

        public void setChipWellBarcode(String chipWellBarcode) {
            this.chipWellBarcode = chipWellBarcode;
        }

        public String getGenderConcordance() {
            return genderConcordance;
        }

        public void setGenderConcordance(String genderConcordance) {
            this.genderConcordance = genderConcordance;
        }

        public int getTryCount() {
            return tryCount;
        }

        public void setTryCount(int tryCount) {
            this.tryCount = tryCount;
        }
    }

    public enum ArraysReviewDecision implements Displayable {
        READY_TO_DELIVER("Ready For Delivery"),
        REWORK("Rework");

        private String displayName;

        ArraysReviewDecision(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
}
