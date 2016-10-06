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
import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@UrlBinding(value = "/view/metricsView.action")
public class MetricsViewActionBean extends HeatMapActionBean {
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

    @Validate(required = true, on = {SEARCH_ACTION})
    private String labVesselIdentifier;

    private LabVessel labVessel;
    private StaticPlate staticPlate;
    private Map<String, Map<String, Object>> metricToPositionToValue;
    private String metricsTableJson;
    private boolean foundResults;
    private boolean isInfinium;
    private final static String GREEN = "#dff0d8";
    private final static String YELLOW = "#fcf8e3";
    private final static String RED = "#f2dede";
    private final static String BLUE = "#d9edf7";


    public final Map<String, List<Options>> INF_METRIC_TYPES = new HashMap<String, List<Options>>() {{
        put("callRate", new OptionsBuilder().
                addOption(">= 98", "98", GREEN).
                addOption(">=95", "95", YELLOW).
                addOption("Fail", "0", RED).build());
        put("fpGender", new OptionsBuilder().
                addOption("M", "M", BLUE).
                addOption("F", "F", RED).build());
        put("hetPct", new OptionsBuilder().
                addOption(">= 25", "25", RED).
                addOption(">= 20", "20", YELLOW).
                addOption("Pass", "0", GREEN).build());
    }};

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
        } else if (!OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
            addValidationError("labVesselIdentifier", "Only plates or chips currently allowed");
        }

        staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);

        Pair<String, String> chipPair = null;
        for(SampleInstanceV2 sampleInstance: labVessel.getSampleInstancesV2()) {
            if (sampleInstance.getSingleBucketEntry() != null) {
                ProductOrder productOrder = sampleInstance.getSingleBucketEntry().getProductOrder();
                Date effectiveDate =  productOrder.getCreatedDate();
                chipPair = productEjb.getGenotypingChip(productOrder, effectiveDate);
            } else {
                List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findBySamples(
                        Collections.singletonList(sampleInstance.getRootOrEarliestMercurySampleName()));
                for (ProductOrderSample productOrderSample: productOrderSamples) {
                    if (productOrderSample.getProductOrder() != null) {
                        ProductOrder productOrder = productOrderSample.getProductOrder();
                        Date effectiveDate =  productOrder.getCreatedDate();
                        chipPair = productEjb.getGenotypingChip(productOrder, effectiveDate);
                    }
                }
            }
        }

        isInfinium = chipPair != null && chipPair.getLeft() != null && chipPair.getRight() != null;
        foundResults = true;
    }

    private void buildMetricsTable() {
        if (isInfinium) {
            buildInfiniumMetricsTable();
        }
    }

    private void buildInfiniumMetricsTable() {
        boolean isHybChip = false;
        metricToPositionToValue = new HashMap<>();
        if (isInfinium) {
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
            Set<String> metrics = INF_METRIC_TYPES.keySet();
            for (String field : metrics) {
                if (!metricToPositionToValue.containsKey(field)) {
                    metricToPositionToValue.put(field, new HashMap<String, Object>());
                }
            }

            MetricsTable metricsTable = new MetricsTable(
                    labVessel.getVesselGeometry().getColumnNames(), labVessel.getVesselGeometry().getRowNames());

            ObjectMapper mapper = new ObjectMapper();
            for (String metric : metrics) {
                Dataset dataset = new Dataset();
                dataset.setType(ChartType.Category);
                dataset.setLabel(metric);
                List<Options> options = INF_METRIC_TYPES.get(metric);
                dataset.setOptions(options);
                metricsTable.getDatasets().add(dataset);
                for (ArraysQc arraysQc : arraysQcList) {
                    Map<String, Object> props = mapper.convertValue(arraysQc, Map.class);
                    if (props.containsKey(metric)) {
                        String chipWellbarcode = arraysQc.getChipWellBarcode();
                        String startPosition = chipWellToSourcePosition.get(chipWellbarcode);
                        Data data = new Data();
                        Object value = props.get(metric);
                        if (value instanceof Double) {
                            value = formatNoPercentSign(value);
                        }
                        data.setValue(value);
                        data.setWell(startPosition);
                        dataset.getData().add(data);

                        data.getMetadata().add(Metadata.create("Well Name", startPosition));
                        data.getMetadata().add(Metadata.create("Sample Alias", arraysQc.getSampleAlias()));
                        data.getMetadata().add(Metadata.create("Chip Well Barcode", arraysQc.getChipWellBarcode()));
                        data.getMetadata().add(Metadata.create("Call Rate", String.valueOf(arraysQc.getCallRate())));
                    }
                }
            }

            try {
                metricsTableJson = mapper.writeValueAsString(metricsTable);
            } catch (IOException e) {
                logger.error("Failed to generate JSON", e);
                addGlobalValidationError("Failed to generate JSON");
                return;
            }
            setHeatMapFields(new ArrayList<>(metrics));
        }
    }

    private static String formatNoPercentSign(Object value) {
        NumberFormat formatter = NumberFormat.getPercentInstance();
        formatter.setMinimumFractionDigits(2);
        return formatter.format(value).replace("%", "");
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

    public String getMetricsTableJson() {
        return metricsTableJson;
    }

    public boolean isFoundResults() {
        return foundResults;
    }

    public void setFoundResults(boolean foundResults) {
        this.foundResults = foundResults;
    }

    public enum ChartType {
        Category, Heatmap
    }

    private class OptionsBuilder {
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

    // TODO JW schema
    public class MetricsTable
    {

        private List<Dataset> datasets;

        private final String[] columnNames;
        private final String[] rowNames;

        public MetricsTable(String[] columnNames, String[] rowNames) {
            this.columnNames = columnNames;
            this.rowNames = rowNames;
        }

        public List<Dataset> getDatasets() {
            if (datasets == null) {
                datasets = new ArrayList<>();
            }
            return datasets;
        }

        public void setDatasets(
                List<Dataset> datasets) {
            this.datasets = datasets;
        }

        public String[] getColumnNames() {
            return columnNames;
        }

        public String[] getRowNames() {
            return rowNames;
        }
    }

    public class Dataset
    {
        private List<Data> data;

        private String label;

        private ChartType type;

        private List<Options> options;

        public List<Data> getData() {
            if (data == null) {
                data = new ArrayList<>();
            }
            return data;
        }

        public void setData(
                List<Data> data) {
            this.data = data;
        }

        public void setOptions(
                List<Options> options) {
            this.options = options;
        }

        public List<Options> getOptions() {
            return options;
        }

        public String getLabel ()
        {
            return label;
        }

        public void setLabel (String label)
        {
            this.label = label;
        }

        public ChartType getType() {
            return type;
        }

        public void setType(ChartType type) {
            this.type = type;
        }
    }

    public class Data
    {
        private Object value;

        private String well;

        private List<Metadata> metadata;

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String getWell ()
        {
            return well;
        }

        public void setWell (String well)
        {
            this.well = well;
        }

        public List<Metadata> getMetadata() {
            if (metadata == null) {
                metadata = new ArrayList<>();
            }
            return metadata;
        }

        public void setMetadata(
                List<Metadata> metadata) {
            this.metadata = metadata;
        }
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

    public class Options
    {
        private String color;

        private String name;

        private String value;

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

        public String getValue ()
        {
            return value;
        }

        public void setValue (String value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return "ClassPojo [color = "+color+", name = "+name+", value = "+value+"]";
        }
    }
}
