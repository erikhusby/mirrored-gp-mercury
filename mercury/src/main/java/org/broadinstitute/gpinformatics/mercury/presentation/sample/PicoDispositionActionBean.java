package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.MetricReworkDisposition;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action bean that shows per-sample disposition after initial pico.
 */
@UrlBinding(value = PicoDispositionActionBean.ACTION_BEAN_URL)
public class PicoDispositionActionBean extends CoreActionBean {
    protected static final Log log = LogFactory.getLog(CoreActionBean.class);

    /**
     * Format a timestamp for file name.
     */
    public static final Format TIMESTAMP_FORMATTER = FastDateFormat.getInstance("yyyy_MM_dd_HH_mm_ss");

    public static final String ACTION_BEAN_URL = "/sample/PicoDisposition.action";

    public static final String EVT_AJAX_VESSEL_LIST = "buildTableData";
    public static final String EVT_DOWNLOAD_PICKER_CSV = "buildPickFile";
    public static final String DEFAULT_PAGE = "/sample/pico_disp.jsp";

    @Inject
    private TubeFormationDao tubeFormationDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    // Spreadsheet upload page invokes this action bean and passes in the tubeLabels parameter.
    private List<String> tubeLabels;
    private String srcRackBarcode;

    /**
     * Have to hold these because ajax calls blow up in stripes if there's global validation errors
     */
    private List<String> validationErrors;

    /**
     * ListItem translate to JSON for datatable display and from JSON for picker CSV file
     */
    private List<ListItem> listItems = new ArrayList<>();

    /**
     * Picker destination racks
     */
    private Map<String, String> destRacks;

    private static final Map<VesselPosition, BarcodedTube> positionToTubeMap = new HashMap<>();

    public void setTubeLabels(List<String> tubeLabels) {
        this.tubeLabels = tubeLabels;
    }

    public String getSrcRackBarcode() {
        return srcRackBarcode;
    }

    public void setSrcRackBarcode(String srcRackBarcode) {
        this.srcRackBarcode = srcRackBarcode;
    }

    /**
     * Consumes scanner JSON and builds positionToTubeMap out of it
     */
    public void setRackScanJson(String json) {
        Map<String, String> positionToBarcodeMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(json);
            for (JsonNode posBarcodeNode : root.withArray("scans")) {
                positionToBarcodeMap.put(posBarcodeNode.at("/position").textValue(), posBarcodeNode.at("/barcode").textValue());
            }
        } catch (Exception ex) {
            log.error("Failure to process scan data.", ex);
            queueValidationError("Failure to process scan data: " + ex.getMessage());
        }
        makeListItemsFromBarcodeMap(positionToBarcodeMap);
    }

    public List<ListItem> getListItems() {
        return listItems;
    }

    /**
     * Build simple JSON text to display in a DataTable
     */
    public String getListItemsJson() {
        if (listItems == null) {
            return "[]";
        }

        try {
            // Handle validation error in JSON
            if (validationErrors != null && !validationErrors.isEmpty()) {
                JSONObject jItem = new JSONObject();
                JSONArray errs = new JSONArray();
                for (String err : validationErrors) {
                    errs.put(err);
                }
                jItem.put("errors", errs);
                return jItem.toString();
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(listItems);

        } catch (Exception jse) {
            log.error("Failed to build vessel list JSON", jse);
            return "{\"errors\":[\"Failed to build vessel list JSON:  " + jse.getMessage() + "\"]}";
        }
    }

    /**
     * Upload JSON data for picklist (pruned on client side)
     */
    public void setListItemsJson(String pickListJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            listItems = mapper.readValue(pickListJson, new TypeReference<List<ListItem>>() {
            });
        } catch (Exception jse) {
            log.error("Failed to build vessel list JSON", jse);
        }
    }

    public void setDestRacks(Map<String, String> destRacks) {
        this.destRacks = destRacks;
    }

    /**
     * Stripes won't call setter without this useless getter
     */
    public Map<String, String> getDestRacks() {
        return destRacks;
    }

    /**
     * Hold onto global error messages - ajax JSON call will blow up if stripes errors exist
     */
    private void queueValidationError(String msg) {
        if (validationErrors == null) {
            validationErrors = new ArrayList<>();
        }
        validationErrors.add(msg);
    }

    /**
     * Build out stripes errors for a non-ajax web page request
     */
    private void buildStripesValidationErrors() {
        if (validationErrors == null) {
            return;
        }
        for (String msg : validationErrors) {
            addGlobalValidationError(msg);
        }
    }

    /**
     * For display, excludes NONE
     */
    public String[] getDestRackTypesForDisplay() {
        DestinationRackType[] enums = DestinationRackType.values();
        String[] vals = new String[enums.length - 1];
        for (int i = 0, count = 0; i < enums.length; i++) {
            if (enums[i] == DestinationRackType.NONE) {
                continue;
            }
            vals[count] = enums[i].name();
            count++;
        }
        return vals;
    }

    // ***************
    // Begin TubeFormation accessible only for test purposes.
    // ***************
    private List<TubeFormation> tubeFormations;
    List<TubeFormation> getTubeFormations() {
        return tubeFormations;
    }
    void setTubeFormation(List<TubeFormation>  tubeFormations) {
        this.tubeFormations = tubeFormations;
    }
    void setBarcodedTubeDao(BarcodedTubeDao dao) {
        barcodedTubeDao = dao;
    }
    void setTubeFormationDao(TubeFormationDao tubeFormationDao) {
        this.tubeFormationDao = tubeFormationDao;
    }
    // ***************
    // End TubeFormation for test purposes.
    // ***************

    public enum DestinationRackType implements Displayable {
        NORM("Normalization"),
        UNDILUTED("Undiluted"),
        REPEAT("Repeat"),
        NONE("------------");

        DestinationRackType(String displayName) {
            this.displayName = displayName;
        }

        private String displayName;

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * UI element: <br/>
     * Outgoing data for vessel/sample list displayed in JSP <br/>
     * Incoming data with user options to build picker csv file
     */
    public static class ListItem {
        private Long metricId;
        private String position;
        private String barcode;
        private String[] collaboratorPatientIds;
        private String sampleId;
        private BigDecimal concentration;
        // TODO riskOverride wasn't/isn't used here anymore to my knowledge
        private boolean riskOverride = false;
        private LabMetricDecision.Decision decision;
        private MetricReworkDisposition reworkDisposition;
        private DestinationRackType destinationRackType;
        private boolean toBePicked = false;

        /**
         * Constructor to support JSON unmarshall for incoming data
         */
        public ListItem() {
        }

        /**
         * Constructor for outgoing data
         */
        public ListItem(Long metricId, String position, String barcode, String[] collaboratorPatientIds,
                        String sampleId, BigDecimal concentration, LabMetricDecision decision) {
            this.metricId = metricId;
            this.position = position;
            this.barcode = barcode;
            this.collaboratorPatientIds = collaboratorPatientIds;
            this.sampleId = sampleId;
            // Sets the number of decimal digits to display.
            this.concentration = (concentration != null) ? MathUtils.scaleTwoDecimalPlaces(concentration) : null;
            this.decision = decision.getDecision();
            if (decision != null) {
                this.reworkDisposition = decision.getReworkDisposition();
                this.riskOverride = decision.equals(LabMetricDecision.Decision.RISK);
            }
            determineDestinationRackType();
            toBePicked = destinationRackType != DestinationRackType.NONE;
        }

        public void setMetricId(Long metricId) {
            this.metricId = metricId;
        }

        public Long getMetricId() {
            return metricId;
        }

        public void setPosition(String position) {
            this.position = position;
        }
        public String getPosition() {
            return position;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }
        public String getBarcode() {
            return barcode;
        }

        public void setCollaboratorPatientIds(String[] collaboratorPatientIds) {
            this.collaboratorPatientIds = collaboratorPatientIds;
        }
        public String[] getCollaboratorPatientIds() {
            return collaboratorPatientIds;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getSampleId() {
            return sampleId;
        }

        public void setConcentration(BigDecimal concentration) {
            this.concentration = concentration;
        }
        public BigDecimal getConcentration() {
            return concentration;
        }

        public void setDecision(LabMetricDecision.Decision decision) {
            this.decision = decision;
        }

        public String getDecision() {
            return decision.name();
        }

        public String getReworkDisposition() {
            return reworkDisposition == null ? null : reworkDisposition.getDisplayName();
        }

        @JsonSetter
        public void setReworkDisposition(String reworkDispositionDisplay) {
            this.reworkDisposition = MetricReworkDisposition.fromDisplayValue(reworkDispositionDisplay);
        }

        public void setDestinationRackType(DestinationRackType destinationRackType) {
            this.destinationRackType = destinationRackType;
        }

        public DestinationRackType getDestinationRackType() {
            return destinationRackType;
        }

        public boolean isRiskOverride() {
            return riskOverride;
        }

        public void setRiskOverride(boolean riskOverride) {
            this.riskOverride = riskOverride;
        }

        public boolean isToBePicked() {
            return toBePicked;
        }

        public void setToBePicked(boolean toBePicked) {
            this.toBePicked = toBePicked;
        }

        private void determineDestinationRackType() {
            if (decision == null || decision.equals(LabMetricDecision.Decision.PASS)) {
                destinationRackType = DestinationRackType.NONE;
            } else {
                switch (reworkDisposition) {
                    case NORM_IN_TUBE:
                    case NORM_ADJUSTED_DOWN:
                    case TUBE_SPLIT:
                    case TUBE_SPLIT_ADJUSTED_DOWN:
                    case BAD_TRIP_OVERFLOW:
                        destinationRackType = DestinationRackType.NORM;
                        break;
                    case UNDILUTED:
                        destinationRackType = DestinationRackType.UNDILUTED;
                        break;
                    case BAD_TRIP_LOW:
                    case BAD_TRIP_READS:
                    case BAD_TRIP_HIGH:
                        destinationRackType = DestinationRackType.REPEAT;
                        break;
                    default:
                        destinationRackType = DestinationRackType.NONE;
                        break;
                }
            }
        }
    }

    /**
     * Displays initial page with no vessels or allows link from quant upload with tubeLabels parameter
     */
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution displayList() {
        // Clears some data, otherwise does basically nothing
        makeListItems(tubeLabels);
        buildStripesValidationErrors();
        return new ForwardResolution(DEFAULT_PAGE);
    }

    /**
     * Builds JSON for vessel list
     */
    @HandlesEvent(EVT_AJAX_VESSEL_LIST)
    public Resolution buildPicoVesselListJson() {
        return new StreamingResolution("text/json", getListItemsJson());
    }

    /**
     * Downloads CSV picker file
     */
    @HandlesEvent(EVT_DOWNLOAD_PICKER_CSV)
    public Resolution downloadPickerCsv() {
        StringBuilder csv = new StringBuilder();

        // Build out targetType to barcode map
        MultiValuedMap<DestinationRackType, String> targetTypeBarcodeMap = new ArrayListValuedHashMap<>();
        for (ListItem item : listItems) {
            targetTypeBarcodeMap.put(item.getDestinationRackType(), item.getBarcode());
        }

        // Barcode to destination position map
        Map<String, VesselPosition> barcodePositionMap = new HashMap<>();
        VesselPosition[] positions = VesselGeometry.G12x8.getVesselPositions();
        for (DestinationRackType targetType : targetTypeBarcodeMap.keySet()) {
            int counter = 0;
            for (String barcode : targetTypeBarcodeMap.get(targetType)) {
                barcodePositionMap.put(barcode, positions[counter++]);
            }
        }

        for (ListItem item : listItems) {
            csv.append(srcRackBarcode).append(",")
                    .append(item.getPosition()).append(",")
                    .append(item.getBarcode()).append(",")
                    .append(destRacks.get(item.getDestinationRackType().name())).append(",")
                    .append(barcodePositionMap.get(item.getBarcode())).append("\r\n");
        }
        String fileName = getUserBean().getLoginUserName() + "_" + TIMESTAMP_FORMATTER.format(new Date()) + "_pico_pick.csv";
        setFileDownloadHeaders("application/octet-stream", fileName);
        StreamingResolution stream = new StreamingResolution("application/octet-stream",
                new ByteArrayInputStream(csv.toString().getBytes()));
        return stream;
    }

    /** Makes listItems from either a tube formation or its label. */
    private void makeListItems(List<String> labels) {
        listItems.clear();
        if (labels == null) {
            return;
        }
        for (String label: labels) {
            TubeFormation container = null;
            if (StringUtils.isNotBlank(label)) {
                container = tubeFormationDao.findByDigest(label);
                if (container == null || container.getContainerRole() == null) {
                    queueValidationError("Cannot find tube formation having label '" + label + "'");
                }
            }
            if (container != null) {
                makeListItems(container.getContainerRole().getMapPositionToVessel());
            }
        }
    }

    /** Makes listItems from a position->barcode map. */
    private void makeListItemsFromBarcodeMap(Map<String, String> positionToBarcodeMap) {
        // First makes a map of position->tube.
        positionToTubeMap.clear();
        for (String position : positionToBarcodeMap.keySet()) {
            String tubeBarcode = positionToBarcodeMap.get(position);
            if (StringUtils.isNotBlank(tubeBarcode)) {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(tubeBarcode);
                if (tube == null) {
                    queueValidationError("Cannot find tube having barcode '" + tubeBarcode + "'");
                } else {
                    VesselPosition vesselPosition = VesselPosition.getByName(position);
                    if (vesselPosition == null) {
                        queueValidationError("Unknown vessel position '" + position + "'");
                    } else {
                        positionToTubeMap.put(VesselPosition.getByName(position), tube);
                    }
                }
            }
        }
        makeListItems(positionToTubeMap);
    }

    /** Makes listItems from a position->tube map. */
    private void makeListItems(Map<VesselPosition, BarcodedTube> map) {
        for (VesselPosition vesselPosition : map.keySet()) {
            BarcodedTube tube = map.get(vesselPosition);
            LabMetric labMetric = tube.findMostRecentLabMetric(LabMetric.MetricType.INITIAL_PICO);
            // 1: 1 for all picos other than some very early instances
            String sampleId = tube.getMercurySamples().size() == 0 ? "" : tube.getMercurySamples().iterator().next().getSampleKey();
            if (labMetric != null) {
                BigDecimal concentration = labMetric.getValue();
                LabMetricDecision decision = labMetric.getLabMetricDecision();
                int rangeCompare = labMetric.initialPicoDispositionRange();
                listItems.add(new ListItem(labMetric.getLabMetricId(), vesselPosition.name(), tube.getLabel(),
                        tube.getMetadataValues(Metadata.Key.PATIENT_ID), sampleId,
                        concentration, decision));

            } else {
                listItems.add(new ListItem(null, vesselPosition.name(), tube.getLabel(),
                        tube.getMetadataValues(Metadata.Key.PATIENT_ID),
                        sampleId, null, null));
            }
        }
        Collections.sort(listItems, BY_POSITION);
    }

    private static final Comparator<ListItem> BY_POSITION = new Comparator<ListItem>() {
            @Override
            public int compare(ListItem o1, ListItem o2) {
                return o1.getPosition().compareTo(o2.getPosition());
            }};

}
