package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.analytics.AlignmentMetricsDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.FingerprintScoreDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AggregationStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AggregationTaskDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@UrlBinding(AggregationTriageActionBean.ACTION_BEAN_URL)
public class AggregationTriageActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(AggregationTriageActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/workflows/aggregation_triage.action";

    private static final String ALIGNMENT_TRIAGE_PAGE = "/hsa/workflows/aggregation/triage.jsp";

    private static final String UPDATE_OOS_ACTION = "updateOutOfSpec";

    private List<TriageDto> passingTriageDtos = new ArrayList<>();

    private List<TriageDto> oosTriageDtos = new ArrayList<>();

    private String reworkReason;

    private String commentText;

    private List<String> selectedSamples;

    private List<TriageDto> selectedDtos = new ArrayList<>();

    private Map<String, MercurySample> mapNameToSample = new HashMap<>();

    private Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<>();

    private Map<String, LabVessel> mapKeyToPond = new HashMap<>();

    @Inject
    private AggregationStateDao aggregationStateDao;

    @Inject
    private AggregationTaskDao aggregationTaskDao;

    @Inject
    private AlignmentMetricsDao alignmentMetricsDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private FingerprintScoreDao fingerprintScoreDao;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BucketEjb bucketEjb;

    @Inject
    private ReworkReasonDao reworkReasonDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        List<AggregationTask> inTriage = aggregationTaskDao.findByStatus(Status.COMPLETE);
        Set<String> sampleIds = inTriage.stream()
                .map(AggregationTask::getFastQSampleId)
                .collect(Collectors.toSet());

        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIds);

        List<AlignmentMetric> alignmentMetrics = alignmentMetricsDao.findBySampleAlias(sampleIds);
        Map<String, AlignmentMetric> mapAliasToMetric = alignmentMetrics.stream()
                .collect(Collectors.toMap(AlignmentMetric::getSampleAlias, Function.identity()));

        // For each tube grab most recent pond
        for (MercurySample mercurySample: mapIdToMercurySample.values()) {
            for (LabVessel labVessel : mercurySample.getLabVessel()) {
                LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                        = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(
                        LabVesselSearchDefinition.POND_LAB_EVENT_TYPES);
                labVessel.evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);
                Set<LabVessel> ponds = eval.getPositions().keySet();
                LabVessel pond = ponds.iterator().next(); // TODO JW grab latest?
                mapKeyToPond.put(mercurySample.getSampleKey(), pond);
            }
        }

        List<TriageDto> dtos = new ArrayList<>();
        for (Map.Entry<String, MercurySample> entry: mapIdToMercurySample.entrySet()) {
            String sampleKey = entry.getKey();
            MercurySample mercurySample = entry.getValue();
            if (mercurySample == null) {
                log.debug("Failed to find mercury sample " + sampleKey);
                continue;
            }

            AlignmentMetric alignmentMetric = mapAliasToMetric.get(sampleKey);
            if (alignmentMetric == null) {
                log.error("Failed to find alignment metric for sample in 'triage' " + sampleKey);
                continue;
            }

            TriageDto dto = createTriageDto(sampleKey, mercurySample, alignmentMetric);
            dtos.add(dto);
        }

        Map<Boolean, List<TriageDto>> mapPassToDtos = dtos.stream().collect(Collectors.partitioningBy(isAggregationInSpec()));
        passingTriageDtos = mapPassToDtos.get(Boolean.TRUE);
        oosTriageDtos = mapPassToDtos.get(Boolean.FALSE);

        return new ForwardResolution(ALIGNMENT_TRIAGE_PAGE);
    }

    @ValidationMethod(on = UPDATE_OOS_ACTION)
    public void validateUpdateOos() {
        if (selectedSamples.isEmpty()) {
            addValidationError("selectedIds", "Please select a sample.");
        }

        Set<String> pdos = new HashSet<>();
        for (TriageDto dto: oosTriageDtos) {
            if (selectedSamples.contains(dto.getPdoSample())) {
                selectedDtos.add(dto);
                pdos.add(dto.getPdo());
            }
        }

        mapNameToSample = mercurySampleDao.findMapIdToMercurySample(selectedSamples);

        List<ProductOrder> productOrders = productOrderDao.findListByBusinessKeys(new ArrayList<>(pdos));
        mapKeyToProductOrder = new HashMap<>();
        for (ProductOrder productOrder: productOrders) {
            for (TriageDto dto: selectedDtos) {
                if (dto.getPdo().equalsIgnoreCase(productOrder.getBusinessKey())) {
                    mapKeyToProductOrder.put(dto.getLibrary(), productOrder);
                }
            }
        }
    }

    @HandlesEvent(UPDATE_OOS_ACTION)
    public Resolution updateOos() {
        Bucket bucket = bucketEjb.findOrCreateBucket("Top Off");
        ReworkReason reason = reworkReasonDao.findByReason("Top off");
        if (reason == null) {
            reason = new ReworkReason(reworkReason);
        }

        // TODO Add to bucket or queue or whatever

        return view();
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
        dto.setContaminination(contamination);
        dto.setCoverage20x(pctCov20);
        dto.setGender(gender);
        dto.setPdo(pdo);
        dto.setPdoSample(sampleKey);
        dto.setLibrary(mapKeyToPond.get(sampleKey).getLabel());
        return dto;
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

    private static Predicate<TriageDto> isAggregationInSpec() {
        return p -> compareLess(p.getContaminination(), 0.01) &&
                    compareGreater(p.getCoverage20x(), 95);
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

    public static class TriageDto {
        private String library;
        private String pdoSample;
        private String pdo;
        private String coverage20x;
        private String contaminination;
        private String alignedQ20Bases;
        private String gender;
        private String processingStatus;
        private String lod;

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

        public String getContaminination() {
            return contaminination;
        }

        public void setContaminination(String contaminination) {
            this.contaminination = contaminination;
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

        public String getProcessingStatus() {
            return processingStatus;
        }

        public void setProcessingStatus(String processingStatus) {
            this.processingStatus = processingStatus;
        }

        public String getLod() {
            return lod;
        }

        public void setLod(String lod) {
            this.lod = lod;
        }
    }

    public enum OutOfSpecCommands implements Displayable {
        SEND_TO_TOP_OFFS("Send To Topoffs"),
        REWORK_FROM_STOCK("Rework From Stock"),
        OVERRIDE_IN_SPEC("Override As In Spec");

        private final String displayname;

        OutOfSpecCommands(String displayname) {

            this.displayname = displayname;
        }

        @Override
        public String getDisplayName() {
            return this.displayname;
        }
    }
}
