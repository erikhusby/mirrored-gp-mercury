package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcFingerprint;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcGtConcordance;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@UrlBinding(value = "/view/metricsView.action")
public class MetricsViewActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(LabEventFactory.class);

    private static final String VIEW_PAGE = "/vessel/vessel_metrics_view.jsp";

    private static final String SEARCH_ACTION = "search";

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ArraysQcDao arraysQcDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Validate(required = true, on = {SEARCH_ACTION})
    private String labVesselIdentifier;

    private LabVessel labVessel;
    private Set<ProductOrder> productOrders;
    private StaticPlate staticPlate;
    private GenotypingChip genotypingChip;
    private Set<String> foundChipTypes;
    private PlateMap plateMap;

    private String metricsTableJson;
    private boolean foundResults;
    private boolean isInfinium;
    private boolean isClinical;
    private final static String GREEN = "#dff0d8";
    private final static String YELLOW = "#fcf8e3";
    private final static String RED = "#f2dede";
    private final static String BLUE = "#d9edf7";
    private final static String SLATE_GRAY = "slategray";

    @JsonSerialize(using = PlateMapMetricsJsonSerializer.class)
    public enum PlateMapMetrics {
        CALL_RATE("Call Rate", true, ChartType.Category, "greaterThanOrEqual"),
        HET_PCT("Heterozygosity (%)", true, ChartType.Category, "greaterThanOrEqual"),
        FP_GENDER("FP Gender", false, ChartType.Category, "equals"),
        REPORTED_GENDER("Reported Gender", false, ChartType.Category, "equals"),
        AUTOCALL_GENDER("Autocall Gender", false, ChartType.Category, "equals"),
        GENDER_CONCORDANCE_PF("Gender Concordance PF", false, ChartType.Category, "equals"),
        P95_GREEN("P95 Green", true, ChartType.Category, "equals"),
        P95_GREEN_PF("P95 Green PF", false, ChartType.Category, "equals"),
        P95_RED("P95 Red", true, ChartType.Category, "equals"),
        P95_RED_PF("P95 Red PF", false, ChartType.Category, "equals"),
        HAPLOTYPE_DIFFERENCE("Haplotype Difference", true, ChartType.Category, "equals"),
        FINGERPRINT_CONCORDANCE("Fingerprint Concordance", false, ChartType.Category, "greaterThan"),
        HAPMAP_CONCORDANCE("HapMap Concordance", false, ChartType.Category, "greaterThanOrEqual");

        private String displayName;
        private final boolean displayValue;
        private final ChartType chartType;
        private String evalType;

        PlateMapMetrics(String displayName, boolean displayValue, ChartType chartType, String evalType) {
            this.displayName = displayName;
            this.displayValue = displayValue;
            this.chartType = chartType;
            this.evalType = evalType;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isDisplayValue() {
            return displayValue;
        }

        public ChartType getChartType() {
            return chartType;
        }

        public String getEvalType() {
            return evalType;
        }
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (labVesselIdentifier != null) {
            validateData();
            if (foundResults)
                buildMetricsTable();
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() {
        if (foundResults) {
            buildMetricsTable();
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SEARCH_ACTION)
    public void validateData() {
        foundResults = false;
        setLabVessel(labVesselDao.findByIdentifier(labVesselIdentifier));
        if (getLabVessel() == null) {
            addValidationError("labVesselIdentifier", "Could not find lab vessel " + labVesselIdentifier);
            return;
        } else if (!OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
            addValidationError("labVesselIdentifier", "Only plates or chips currently allowed");
            return;
        }

        staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);

        productOrders = new HashSet<>();
        Set<Pair<String, String>> chipPairs = new HashSet<>();
        for(SampleInstanceV2 sampleInstance: labVessel.getSampleInstancesV2()) {
            ProductOrderSample productOrderSample = sampleInstance.getProductOrderSampleForSingleBucket();
            if (productOrderSample != null) {
                ProductOrder productOrder = productOrderSample.getProductOrder();
                Date effectiveDate =  productOrder.getCreatedDate();
                Pair<String, String> chipPair = productEjb.getGenotypingChip(productOrder, effectiveDate);
                chipPairs.add(chipPair);
                productOrders.add(productOrder);
            } else {
                List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findBySamples(
                        Collections.singletonList(sampleInstance.getRootOrEarliestMercurySampleName()));
                for (ProductOrderSample pdoSample: productOrderSamples) {
                    if (pdoSample.getProductOrder() != null) {
                        ProductOrder productOrder = pdoSample.getProductOrder();
                        productOrders.add(productOrder);
                        Date effectiveDate =  productOrder.getCreatedDate();
                        Pair<String, String> chipPair = productEjb.getGenotypingChip(productOrder, effectiveDate);
                        chipPairs.add(chipPair);
                    }
                }
            }
        }

        foundChipTypes = new HashSet<>();
        if (!chipPairs.isEmpty()) {
            Pair<String, String> firstChipPair = chipPairs.iterator().next();
            isInfinium = firstChipPair != null && firstChipPair.getLeft() != null && firstChipPair.getRight() != null;
            if (isInfinium) {
                for (Pair<String, String> chipPair : chipPairs) {
                    if (chipPair.getLeft() != null && chipPair.getRight() != null) {
                        genotypingChip = attributeArchetypeDao.findGenotypingChip(chipPair.getLeft(),
                                chipPair.getRight());
                        if (genotypingChip == null) {
                            addGlobalValidationError("Chip " + chipPair.getRight() + " is not configured");
                            return;
                        }
                        foundChipTypes.add(genotypingChip.getChipName());
                    }
                }
            }
        }

        // Check if clinical
        isClinical = true;
        for (ProductOrder productOrder : productOrders) {
            ResearchProject researchProject = productOrder.getResearchProject();
            if (!researchProject.getRegulatoryDesignation().isClinical()) {
                isClinical = false;
            }
        }
        foundResults = true;
    }

    private void buildMetricsTable() {
        if (isInfinium) {
            try {
                buildInfiniumMetricsTable();
            } catch (IOException e) {
                logger.error("Error building Infinium Metrics Table", e);
                addGlobalValidationError("Failed to generate metrics view");
            }
        } else {
            addGlobalValidationError("Currently only used to display Arrays QC data");
        }
    }

    public void buildInfiniumMetricsTable() throws IOException {
        boolean isHybChip = false;
        Set<LabVessel> chips = new HashSet<>();
        Set<String> chipWellBarcodes = new HashSet<>();
        Map<String, String> chipWellToSourcePosition = new HashMap<>();

        Set<LabEvent> hybEvents = new HashSet<>();
        if (staticPlate.getVesselGeometry().name().contains("CHIP")) {
            chips.add(labVessel);
            hybEvents.addAll(labVessel.getTransfersTo());
            isHybChip = true;
        } else {
            List<LabEventType> infiniumRootEventTypes =
                    Collections.singletonList(LabEventType.INFINIUM_HYBRIDIZATION);
            TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                    = new TransferTraverserCriteria.VesselForEventTypeCriteria(infiniumRootEventTypes, true);

            if (labVessel.getContainerRole() != null) {
                labVessel.getContainerRole().applyCriteriaToAllPositions(eventTypeCriteria,
                        TransferTraverserCriteria.TraversalDirection.Descendants);
            }

            for (Map.Entry<LabEvent, Set<LabVessel>> eventEntry : eventTypeCriteria.getVesselsForLabEventType()
                    .entrySet()) {
                for (LabVessel labVessel : eventEntry.getValue()) {
                    if (labVessel.getVesselGeometry().name().contains("CHIP")) {
                        chips.addAll(eventEntry.getValue());
                        hybEvents.add(eventEntry.getKey());
                    }
                }
            }
        }
        if (chips.isEmpty()) {
            addValidationError("labVesselIdentifier", "No infinium metrics found for lab vessel");
            return;
        }

        for (LabEvent labEvent : hybEvents) {
            Set<CherryPickTransfer> cherryPickTransfers = labEvent.getCherryPickTransfers();
            for (CherryPickTransfer cherryPickTransfer : cherryPickTransfers) {
                VesselPosition sourcePosition = cherryPickTransfer.getSourcePosition();
                VesselPosition destinationPosition = cherryPickTransfer.getTargetPosition();
                LabVessel chip = cherryPickTransfer.getTargetVesselContainer().getEmbedder();
                String chipWellBarcode = chip.getLabel() + "_" + destinationPosition.name();
                chipWellBarcodes.add(chipWellBarcode);
                if (isHybChip) {
                    chipWellToSourcePosition.put(chipWellBarcode, destinationPosition.name());
                } else {
                    chipWellToSourcePosition.put(chipWellBarcode, sourcePosition.name());
                }
            }
        }

        List<ArraysQc> arraysQcList = arraysQcDao.findByBarcodes(new ArrayList<>(chipWellBarcodes));
        if (arraysQcList.isEmpty()) {
            addGlobalValidationError("Failed to find any Arrays QC data for vessel " + labVessel.getLabel());
            return;
        }

        // Call Rate threshold depends on the Genotyping Chip
        List<Options> callRateOptions = null;
        if (foundChipTypes.size() == 1 && genotypingChip != null) {
            Map<String, String> chipAttributes = genotypingChip.getAttributeMap();
            if (productOrders.size() == 1) {
                ProductOrder productOrder = productOrders.iterator().next();
                GenotypingProductOrderMapping genotypingProductOrderMapping =
                        attributeArchetypeDao.findGenotypingProductOrderMapping(productOrder.getJiraTicketKey());
                if (genotypingProductOrderMapping != null) {
                    for (ArchetypeAttribute archetypeAttribute : genotypingProductOrderMapping.getAttributes()) {
                        if (chipAttributes.containsKey(archetypeAttribute.getAttributeName()) &&
                            archetypeAttribute.getAttributeValue() != null) {
                            chipAttributes.put(
                                    archetypeAttribute.getAttributeName(), archetypeAttribute.getAttributeValue());
                        }
                    }
                }
            }

            if (chipAttributes.containsKey("call_rate_threshold")) {
                String call_rate_threshold = chipAttributes.get("call_rate_threshold");
                int passingCallRateThreshold = Integer.parseInt(call_rate_threshold);
                int warningCallRateThreshold = passingCallRateThreshold - 3;
                String passingLegendLabel = String.format(">= %d", passingCallRateThreshold);
                String warningLegendLabel = String.format(">= %d", warningCallRateThreshold);
                callRateOptions = new OptionsBuilder().
                        addOption(passingLegendLabel, call_rate_threshold, GREEN).
                        addOption(warningLegendLabel, String.valueOf(passingCallRateThreshold), YELLOW).
                        addOption("Fail", "0", RED).build();
            } else {
                addGlobalValidationError(
                        "Failed to find call rate threshold for genotyping chip " + genotypingChip.getChipName());
                return;
            }
        } else {
            callRateOptions = new OptionsBuilder().
                    addOption(">= 98", "98", GREEN).
                    addOption(">= 95", "95", YELLOW).
                    addOption("Fail", "0", RED).build();
        }

        List<Options> fpGenderOptions = new OptionsBuilder().addOption("M", "M", BLUE).addOption("F", "F", RED).
                addOption("Not Fingerprinted", "U", SLATE_GRAY).build();

        List<Options> genderOptions = new OptionsBuilder().addOption("M", "M", BLUE).addOption("F", "F", RED).build();

        List<Options> trueFalseOption = new OptionsBuilder().addOption("True", Boolean.TRUE.toString(), GREEN)
                .addOption("False",  Boolean.FALSE.toString(), RED).build();

        List<Options> hetPctOptions = new OptionsBuilder().addOption(">= 25", "25", RED).
                addOption(">= 20", "20", YELLOW).
                addOption("Pass", "0", GREEN).build();

        String fingerPrintPassingValue = isClinical ? "10" : "3";
        List<Options> fingerprintingConcordanceOptions = new OptionsBuilder().
                addOption("Pass", fingerPrintPassingValue, GREEN).
                addOption("Fail", "-3", RED).
                addOption("Not Comparable", String.valueOf(Integer.MIN_VALUE), YELLOW).build();

        List<Options> hapMapConcordanceOptions = new OptionsBuilder().addOption(">= 90", "90", RED).
                addOption(">= 85", "85", YELLOW).
                addOption("Fail", "0", GREEN).build();

        List<Options> emptyOptions = new OptionsBuilder().build();

        plateMap = new PlateMap();
        Map<PlateMapMetrics, WellDataset> plateMapToWellDataSet = new HashMap<>();
        for (PlateMapMetrics plateMapMetric: PlateMapMetrics.values()) {
            WellDataset wellDataset = new WellDataset(plateMapMetric);
            plateMapToWellDataSet.put(plateMapMetric, wellDataset);
            plateMap.getDatasets().add(wellDataset);
        }

        for(ArraysQc arraysQc: arraysQcList) {
            String chipWellbarcode = arraysQc.getChipWellBarcode();
            String startPosition = chipWellToSourcePosition.get(chipWellbarcode);

            List<Metadata> metadata = new ArrayList<>();
            metadata.add(Metadata.create("Well Name", startPosition));
            metadata.add(Metadata.create("Sample Alias", arraysQc.getSampleAlias()));
            metadata.add(Metadata.create("Call Rate", String.valueOf(arraysQc.getCallRate())));
            metadata.add(Metadata.create("Total SNPs", String.valueOf(arraysQc.getTotalSnps())));
            metadata.add(Metadata.create("Total Assays", String.valueOf(arraysQc.getTotalAssays())));
            metadata.add(Metadata.create("Chip Well Barcode", (arraysQc.getChipWellBarcode())));

            // Sample ID metadata
            VesselPosition vesselPosition = VesselPosition.getByName(startPosition);
            if (vesselPosition != null) {
                Set<SampleInstanceV2> sampleInstancesAtPositionV2 = staticPlate.getContainerRole()
                        .getSampleInstancesAtPositionV2(vesselPosition);
                if (sampleInstancesAtPositionV2 != null && sampleInstancesAtPositionV2.size() == 1) {
                    SampleInstanceV2 sampleInstanceV2 = sampleInstancesAtPositionV2.iterator().next();
                    String mercuryRootSampleName = sampleInstanceV2.getMercuryRootSampleName();
                    metadata.add(Metadata.create("Sample ID", mercuryRootSampleName));
                }
            }

            // Call Rate
            String value = ColumnValueType.TWO_PLACE_DECIMAL.format(
                    arraysQc.getCallRate().multiply(BigDecimal.valueOf(100)), "");
            WellDataset wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.CALL_RATE);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(callRateOptions);

            // FP Gender
            value = String.valueOf(arraysQc.getFpGender());
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.FP_GENDER);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(fpGenderOptions);

            // Reported Gender
            value = String.valueOf(arraysQc.getReportedGender());
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.REPORTED_GENDER);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(genderOptions);

            // Autocall Gender
            value = String.valueOf(arraysQc.getAutocallGender());
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.AUTOCALL_GENDER);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(genderOptions);

            // Gender Concordance PF
            value = String.valueOf(arraysQc.getGenderConcordancePf());
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.GENDER_CONCORDANCE_PF);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(trueFalseOption);

            // Het PCT
            value = ColumnValueType.TWO_PLACE_DECIMAL.format(
                    arraysQc.getHetPct().multiply(BigDecimal.valueOf(100)), "");
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.HET_PCT);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(hetPctOptions);

            // P95_GREEN
            value = String.valueOf(arraysQc.getP95Green());
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.P95_GREEN);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(emptyOptions);

            // P95 Green PF
            value = String.valueOf(arraysQc.getP95GreenPf());
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.P95_GREEN_PF);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(trueFalseOption);

            // P95 Red
            value = String.valueOf(arraysQc.getP95Red());
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.P95_RED);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(emptyOptions);

            // P95 Red PF
            value = String.valueOf(arraysQc.getP95RedPf());
            wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.P95_RED_PF);
            wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
            wellDataset.setOptions(trueFalseOption);

            // Fingerprinting Concordance
            if (!arraysQc.getArraysQcFingerprints().isEmpty()) {
                ArraysQcFingerprint arraysQcFingerprint = arraysQc.getArraysQcFingerprints().iterator().next();
                value = String.valueOf(arraysQcFingerprint.getLodExpectedSample());
                wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.FINGERPRINT_CONCORDANCE);
                wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
                wellDataset.setOptions(fingerprintingConcordanceOptions);
            }

            // Haplotype Difference
            if (!arraysQc.getArraysQcFingerprints().isEmpty()) {
                ArraysQcFingerprint arraysQcFingerprint = arraysQc.getArraysQcFingerprints().iterator().next();
                value = String.valueOf(Math.abs(arraysQcFingerprint.getHaplotypesConfidentlyChecked() -
                                                arraysQcFingerprint.getHaplotypesConfidentlyMatchin()));
                wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.HAPLOTYPE_DIFFERENCE);
                wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
                wellDataset.setOptions(emptyOptions);
            }

            // HapMap Concordance
            for (ArraysQcGtConcordance arraysQcGtConcordance: arraysQc.getArraysQcGtConcordances()) {
                if (arraysQcGtConcordance.getVariantType().equals("SNP")) {
                    value = ColumnValueType.TWO_PLACE_DECIMAL.format(
                            arraysQcGtConcordance.getGenotypeConcordance().multiply(BigDecimal.valueOf(100)), "");
                    wellDataset = plateMapToWellDataSet.get(PlateMapMetrics.HAPMAP_CONCORDANCE);
                    wellDataset.getWellData().add(new WellData(startPosition, value, metadata));
                    wellDataset.setOptions(hapMapConcordanceOptions);
                }
            }

        }
        ObjectMapper mapper = new ObjectMapper();
        metricsTableJson = mapper.writeValueAsString(plateMap);
    }

    public String getLabVesselIdentifier() {
        return labVesselIdentifier;
    }

    public void setLabVesselIdentifier(String labVesselIdentifier) {
        this.labVesselIdentifier = labVesselIdentifier;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    public PlateMap getPlateMap() {
        return plateMap;
    }

    public String getMetricsTableJson() {
        return metricsTableJson;
    }

    public boolean isFoundResults() {
        return foundResults;
    }

    public void setFoundResults(boolean foundResults) {
        this.foundResults = foundResults;
    }

    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    public void setArraysQcDao(ArraysQcDao arraysQcDao) {
        this.arraysQcDao = arraysQcDao;
    }

    public void setProductOrderSampleDao(
            ProductOrderSampleDao productOrderSampleDao) {
        this.productOrderSampleDao = productOrderSampleDao;
    }

    public void setProductEjb(ProductEjb productEjb) {
        this.productEjb = productEjb;
    }

    public void setAttributeArchetypeDao(
            AttributeArchetypeDao attributeArchetypeDao) {
        this.attributeArchetypeDao = attributeArchetypeDao;
    }

    /**
     * JSON object that feeds into PlateMap.js
     */
    public class PlateMap {
        private List<WellDataset> datasets;

        public List<WellDataset> getDatasets() {
            if (datasets == null) {
                datasets = new ArrayList<>();
            }
            return datasets;
        }

        public void setDatasets(
                List<WellDataset> datasets) {
            this.datasets = datasets;
        }
    }

    public class WellDataset {
        PlateMapMetrics plateMapMetrics;
        List<WellData> wellData;
        List<Options> options;

        public WellDataset(PlateMapMetrics plateMapMetrics) {
            this.plateMapMetrics = plateMapMetrics;
        }

        public PlateMapMetrics getPlateMapMetrics() {
            return plateMapMetrics;
        }

        public List<WellData> getWellData() {
            if (wellData == null) {
                wellData = new ArrayList<>();
            }
            return wellData;
        }

        public List<Options> getOptions() {
            return options;
        }

        public void setOptions(
                List<Options> options) {
            this.options = options;
        }
    }

    public class WellData {
        private String well;
        private String value;
        private List<Metadata> metadata;

        public WellData(String well, String value, List<Metadata> metadata) {
            this.well = well;
            this.value = value;
            this.metadata = metadata;
        }

        public String getWell() {
            return well;
        }

        public String getValue() {
            return value;
        }

        public List<Metadata> getMetadata() {
            return metadata;
        }
    }

    public enum ChartType {
        Category, Heatmap
    }

    private static class OptionsBuilder {
        private List<Options> options;

        public OptionsBuilder() {
            this.options = new ArrayList<>();
        }

        public OptionsBuilder addOption(String name, String value, String color){
            Options opt = new Options();
            opt.setName(name);
            opt.setValue(value);
            opt.setColor(color);
            options.add(opt);
            return this;
        }

        public List<Options> build() {return this.options;}
    }

    public static class Metadata
    {
        private String value;

        private String label;

        public String getValue ()
        {
            return value;
        }

        public void setValue (String value)
        {
            this.value = value;
        }

        public String getLabel ()
        {
            return label;
        }

        public void setLabel (String label)
        {
            this.label = label;
        }

        public static Metadata create(String label, String value) {
            Metadata metadata = new Metadata();
            metadata.setValue(value);
            metadata.setLabel(label);
            return metadata;
        }
    }

    /**
     * Represent possible Categories for a given dataset, e.g. Male vs Female in a gender metric or a
     * ranking of 95% or greater for a percentage metric
     */
    public static class Options
    {
        private String color;

        private String name;

        private Object value;

        public String getColor ()
        {
            return color;
        }

        public void setColor (String color)
        {
            this.color = color;
        }

        public String getName ()
        {
            return name;
        }

        public void setName (String name)
        {
            this.name = name;
        }

        public Object getValue ()
        {
            return value;
        }

        public void setValue (Object value)
        {
            this.value = value;
        }
    }
}
