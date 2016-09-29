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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
    private boolean foundResults;

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
    public void validateData() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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
            Field[] fields = ArraysQc.class.getDeclaredFields();
            for (Field field : fields) {
                String name = field.getName();
                if (!metricToPositionToValue.containsKey(name)) {
                    metricToPositionToValue.put(name, new HashMap<String, Object>());
                }
            }

            // Build map of Metric -> Position -> Value
            ObjectMapper mapper = new ObjectMapper();
            for (ArraysQc arraysQc: arraysQcList) {
                Map<String,Object> props = mapper.convertValue(arraysQc, Map.class);
                String sourcePosition = chipWellToSourcePosition.get(arraysQc.getChipWellBarcode());
                for (Map.Entry<String, Object> entry: props.entrySet()) {
                    if (!metricToPositionToValue.containsKey(entry.getKey())) {
                        metricToPositionToValue.put(entry.getKey(), new HashMap<String, Object>());
                    }
                    Map<String, Object> positionToValue = metricToPositionToValue.get(entry.getKey());
                    positionToValue.put(sourcePosition, entry.getValue());
                }
            }
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

    public Map<String, Map<String, Object>> getMetricToPositionToValue() {
        return metricToPositionToValue;
    }

    public void setMetricToPositionToValue(
            Map<String, Map<String, Object>> metricToPositionToValue) {
        this.metricToPositionToValue = metricToPositionToValue;
    }

    public boolean isFoundResults() {
        return foundResults;
    }

    public void setFoundResults(boolean foundResults) {
        this.foundResults = foundResults;
    }
}
