package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@UrlBinding(value = "/view/metricsView.action")
public class MetricsViewActionBean extends HeatMapActionBean {
    private static final String VIEW_PAGE = "/vessel/vessel_metrics_view.jsp";

    private static final String SEARCH_ACTION = "search";

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ArraysQcDao arraysQcDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Validate(required = true, on = {SEARCH_ACTION})
    private String labVesselIdentifier;

    private LabVessel labVessel;
    private Map<String, Map<String, Object>> metricToPositionToValue;
    private String metricToPositionToValueJson;
    private boolean foundResults;
    private Map<String, Plot> metricToPlot;
    private Map<String, Plot> metricToPlotJson;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (labVesselIdentifier != null) {
            labVessel = labVesselDao.findByIdentifier(labVesselIdentifier);
            buildHeatMap();
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() {
        buildHeatMap();
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SEARCH_ACTION)
    public void validateData()
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException {
        foundResults = false;
        setLabVessel(labVesselDao.findByIdentifier(labVesselIdentifier));
        if (getLabVessel() == null) {
            addValidationError("labVesselIdentifier", "Could not find lab vessel " + labVesselIdentifier);
        } else if (!OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
            addValidationError("labVesselIdentifier", "Only plates or chips currently allowed");
        }

        StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);

        Product product = null;
        for(SampleInstanceV2 sampleInstance: labVessel.getSampleInstancesV2()) {
            // Controls don't have bucket entries, but we assume that the non-control samples dominate.
            if (sampleInstance.getSingleBucketEntry() != null) {
                Product otherProduct = sampleInstance.getSingleBucketEntry().getProductOrder().getProduct();
                if (product != null && !otherProduct.getProductName().equals(product.getProductName())) {
                    addValidationError("labVesselIdentifier", "Cannot have a mix of products");
                    return;
                } else {
                    product = otherProduct;
                }
            } else {
                //TODO code duplication fix
                //TODO handle multiple products
                List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findBySamples(
                        Collections.singletonList(sampleInstance.getRootOrEarliestMercurySampleName()));
                for (ProductOrderSample productOrderSample: productOrderSamples) {
                    if (productOrderSample.getProductOrder() != null) {
                        Product otherProduct = productOrderSample.getProductOrder().getProduct();
                        if (product != null && !otherProduct.getProductName().equals(product.getProductName())) {
//                            addValidationError("labVesselIdentifier", "Cannot have a mix of products");
//                            return;
                        } else {
                            product = otherProduct;
                        }
                    }
                }
            }
        }

        if (product == null) {
            addValidationError("labVesselIdentifier", "Couldn't find product for lab vessel");
            return;
        }

        boolean isInfinium = product.getProductName().startsWith("Infinium");
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
                List<LabEventType> infiniumRootEventTypes = Collections.singletonList(LabEventType.INFINIUM_HYBRIDIZATION);
                TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                        = new TransferTraverserCriteria.VesselForEventTypeCriteria(infiniumRootEventTypes, true);

                if( labVessel.getContainerRole() != null ) {
                    labVessel.getContainerRole().applyCriteriaToAllPositions(eventTypeCriteria,
                            TransferTraverserCriteria.TraversalDirection.Descendants);
                }

                for(Map.Entry<LabEvent, Set<LabVessel>> eventEntry : eventTypeCriteria.getVesselsForLabEventType().entrySet()) {
                    for (LabVessel labVessel: eventEntry.getValue()) {
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

            for (LabEvent labEvent: hybEvents) {
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
            List<String> metrics = Arrays.asList("callRate", "autocallGender", "fpGender", "reportedGender",
                    "genderConcordancePf", "hetPct");
            for (String field : metrics) {
                if (!metricToPositionToValue.containsKey(field)) {
                    metricToPositionToValue.put(field, new HashMap<String, Object>());
                }
            }

            MetricsTable metricsTable = new MetricsTable();
            Dataset dataset = new Dataset();
            dataset.setType(ChartType.Category);
            dataset.setLabel("Call Rate");
            metricsTable.getDatasets().add(dataset);

            ObjectMapper mapper = new ObjectMapper();
            for (String metric : metrics) {
                for (ArraysQc arraysQc: arraysQcList) {
                    Map<String,Object> props = mapper.convertValue(arraysQc, Map.class);
                    if (props.containsKey(metric)) {
                        Data data = new Data();
                        data.setValue(props.get(metric));

                    }
                }
            }

            // Build map of Metric -> Position -> Value
            List<Data> callRateData = new ArrayList<>();
            for (ArraysQc arraysQc: arraysQcList) {
                Map<String,Object> props = mapper.convertValue(arraysQc, Map.class);
                String sourcePosition = chipWellToSourcePosition.get(arraysQc.getChipWellBarcode());
                for (Map.Entry<String, Object> entry: props.entrySet()) {
                    if (metrics.contains(entry.getKey())) {
                        if (!metricToPositionToValue.containsKey(entry.getKey())) {
                            metricToPositionToValue.put(entry.getKey(), new HashMap<String, Object>());
                        }
                        Map<String, Object> positionToValue = metricToPositionToValue.get(entry.getKey());
                        positionToValue.put(sourcePosition, entry.getValue());
                    }
                }
            }
            dataset.setData();
            metricToPositionToValueJson = mapper.writeValueAsString(metricToPositionToValue);
        }

        setHeatMapFields(new ArrayList<>(metricToPositionToValue.keySet()));
        foundResults = true;
    }

    private void buildHeatMap() {
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

    public String getMetricToPositionToValueJson() {
        return metricToPositionToValueJson;
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

    // TODO JW schema
    public class MetricsTable
    {

        private List<Dataset> datasets;

        private VesselGeometry vesselGeometry;

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

        public VesselGeometry getVesselGeometry ()
        {
            return vesselGeometry;
        }

        public void setVesselGeometry (VesselGeometry vesselGeometry)
        {
            this.vesselGeometry = vesselGeometry;
        }
    }

    public class Dataset
    {
        private String eval;

        private List<Data> data;

        private String label;

        private ChartType type;

        private List<Options> options;



        public String getEval ()
        {
            return eval;
        }

        public void setEval (String eval)
        {
            this.eval = eval;
        }

        public List<Data> getData() {
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
    }

    public class Metadata
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
