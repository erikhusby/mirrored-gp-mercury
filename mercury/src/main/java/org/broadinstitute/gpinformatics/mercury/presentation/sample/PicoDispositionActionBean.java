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
import org.broadinstitute.gpinformatics.mercury.control.vessel.MetricReworkDisposition;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
    public static final int MAX_TUBES_PER_DESTINATION = 96;

    public static final String ACTION_BEAN_URL = "/sample/PicoDisposition.action";

    // From rack scan
    public static final String EVT_AJAX_VESSEL_LIST = "buildScanTableData";
    // Fwd from initial pico upload
    public static final String EVT_FWD_VESSEL_LIST = "buildFwdTableData";
    public static final String EVT_DOWNLOAD_PICKER_CSV = "buildPickFile";
    public static final String DEFAULT_PAGE = "/sample/pico_disp.jsp";

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    private boolean isRackScanEnabled = true;
    // Forward from quant upload page
    private Long labMetricRunId;

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
    private Set<String> destRacks;

    private static final Map<VesselPosition, BarcodedTube> positionToTubeMap = new HashMap<>();

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

    public boolean isRackScanEnabled() {
        return isRackScanEnabled;
    }

    /**
     * Build simple JSON text: <br/>
     * If no errors, an array of ListItem elements to display in a DataTable <br/>
     * If validation errors, a single 'errors' property with an array of error messages
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

    /**
     * Forward from quant upload page
     */
    public void setLabMetricRunId(Long labMetricRunId) {
        this.labMetricRunId = labMetricRunId;
    }

    public Set<String> getDestRacks() {
        if (destRacks == null) {
            // Page from menu pick is for a rack scan, max of 1 of each destination type is required
            destRacks = new HashSet<String>(Arrays.asList("NORM", "UNDILUTED", "REPEAT"));
        }
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
        private String sampleId;
        private BigDecimal concentration;
        private BigDecimal volume;
        // TODO riskOverride wasn't/isn't used here anymore to my knowledge
        private boolean riskOverride = false;
        private LabMetricDecision.Decision decision;
        private MetricReworkDisposition reworkDisposition;
        private DestinationRackType destinationRackType;
        private String srcRackBarcode;
        private String sysDestRackBarcode;
        private String userDestRackBarcode;
        private boolean toBePicked = false;

        /**
         * Constructor to support JSON unmarshall for incoming data
         */
        public ListItem() {
        }

        /**
         * Constructor for outgoing data
         */
        public ListItem(Long metricId, String position, String barcode, BigDecimal volume,
                        String sampleId, BigDecimal concentration, LabMetricDecision decision, String srcRackBarcode) {
            this.metricId = metricId;
            this.position = position;
            this.barcode = barcode;
            this.volume = volume;
            this.sampleId = sampleId;
            // Sets the number of decimal digits to display.
            this.concentration = (concentration != null) ? MathUtils.scaleTwoDecimalPlaces(concentration) : null;
            this.decision = decision.getDecision();
            if (decision != null) {
                this.reworkDisposition = decision.getReworkDisposition();
                this.riskOverride = decision.equals(LabMetricDecision.Decision.RISK);
                determineDestinationRackType();
            }
            this.srcRackBarcode = srcRackBarcode;
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

        public BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(BigDecimal volume) {
            this.volume = volume;
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

        public String getSrcRackBarcode() {
            return srcRackBarcode;
        }

        public void setSrcRackBarcode(String srcRackBarcode) {
            this.srcRackBarcode = srcRackBarcode;
        }

        public String getSysDestRackBarcode() {
            return sysDestRackBarcode;
        }

        public void setSysDestRackBarcode(String sysDestRackBarcode) {
            this.sysDestRackBarcode = sysDestRackBarcode;
        }

        public String getUserDestRackBarcode() {
            return userDestRackBarcode;
        }

        public void setUserDestRackBarcode(String userDestRackBarcode) {
            this.userDestRackBarcode = userDestRackBarcode;
        }

        private void determineDestinationRackType() {
            if (reworkDisposition == null || decision.equals(LabMetricDecision.Decision.PASS)) {
                destinationRackType = DestinationRackType.NONE;
                sysDestRackBarcode = "";
            } else {
                switch (reworkDisposition) {
                    case NORM_IN_TUBE:
                    case NORM_ADJUSTED_DOWN:
                    case TUBE_SPLIT:
                    case TUBE_SPLIT_ADJUSTED_DOWN:
                    case BAD_TRIP_OVERFLOW:
                        destinationRackType = DestinationRackType.NORM;
                        sysDestRackBarcode = "NORM";
                        break;
                    case UNDILUTED:
                        destinationRackType = DestinationRackType.UNDILUTED;
                        sysDestRackBarcode = "UNDILUTED";
                        break;
                    case BAD_TRIP_LOW:
                    case BAD_TRIP_READS:
                    case BAD_TRIP_HIGH:
                        destinationRackType = DestinationRackType.REPEAT;
                        sysDestRackBarcode = "REPEAT";
                        break;
                    default:
                        destinationRackType = DestinationRackType.NONE;
                        sysDestRackBarcode = "";
                        break;
                }
            }
            // Gets overwritten on forward from quant upload page
            userDestRackBarcode = sysDestRackBarcode;
        }
    }

    /**
     * Displays initial page with no vessels
     */
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution displayList() {
        isRackScanEnabled = true;
        buildStripesValidationErrors();
        return new ForwardResolution(DEFAULT_PAGE);
    }

    /**
     * Builds JSON for vessel list from rack scan
     */
    @HandlesEvent(EVT_AJAX_VESSEL_LIST)
    public Resolution buildScanVesselListJson() {
        return new StreamingResolution("text/json", getListItemsJson());
    }

    /**
     * Use forward from quant upload page to build JSON for vessel list
     */
    @HandlesEvent(EVT_FWD_VESSEL_LIST)
    public Resolution buildFwdVesselListJson() {
        if (labMetricRunId == null) { // Something went wrong - this is required for forward
            addGlobalValidationError("Lab metric run ID is required");
            isRackScanEnabled = true;
            return new ForwardResolution(DEFAULT_PAGE);
        }

        LabMetricRun labMetricRun = barcodedTubeDao.findById(LabMetricRun.class, labMetricRunId);
        if (labMetricRun == null) {
            addGlobalValidationError("No lab metric run exists for ID = " + labMetricRunId);
            isRackScanEnabled = true;
            return new ForwardResolution(DEFAULT_PAGE);
        }

        // Need the source container, either a tube formation or a static plate (germline process) and it's corresponding barcode (rack of tubes or static plate)
        Map<VesselContainer, String> containerLabelMap = buildSourceContainerMap(labMetricRun);

        isRackScanEnabled = false;
        for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
            // Capture only metrics associated with source tubes or wells vs. (e.g.) pico plates
            if (labMetric.getLabMetricDecision() != null) {
                LabVessel metricVessel = labMetric.getLabVessel();
                LabMetricDecision labMetricDecision = labMetric.getLabMetricDecision();
                String sampleId = metricVessel.getMercurySamples().size() == 0 ? ""
                        : metricVessel.getMercurySamples().iterator().next().getSampleKey();
                String srcRackBarcode = "";
                for (Map.Entry<VesselContainer, String> mapEntry : containerLabelMap.entrySet()) {
                    if (mapEntry.getKey().getContainedVessels().contains(metricVessel)) {
                        srcRackBarcode = mapEntry.getValue();
                        break;
                    }
                }
                ListItem listItem = new ListItem(labMetric.getLabMetricId(), labMetric.getVesselPosition().name(),
                        metricVessel.getLabel(), metricVessel.getVolume(),
                        sampleId, labMetric.getValue(), labMetricDecision, srcRackBarcode);
                listItems.add(listItem);
            }
        }

        int normDestCount = 0;
        int undilutedDestCount = 0;
        int repeatDestCount = 0;
        destRacks = new TreeSet<>();
        String destBarcode;

        for (ListItem listItem : listItems) {
            switch (listItem.getDestinationRackType()) {
                case NORM:
                    destBarcode = "NORM" + (Math.floorDiv(normDestCount, MAX_TUBES_PER_DESTINATION) + 1);
                    listItem.setSysDestRackBarcode(destBarcode);
                    destRacks.add(destBarcode);
                    normDestCount++;
                    break;
                case UNDILUTED:
                    destBarcode = "UNDILUTED" + (Math.floorDiv(undilutedDestCount, MAX_TUBES_PER_DESTINATION) + 1);
                    listItem.setSysDestRackBarcode(destBarcode);
                    destRacks.add(destBarcode);
                    undilutedDestCount++;
                    break;
                case REPEAT:
                    destBarcode = "REPEAT" + (Math.floorDiv(repeatDestCount, MAX_TUBES_PER_DESTINATION) + 1);
                    listItem.setSysDestRackBarcode(destBarcode);
                    destRacks.add(destBarcode);
                    repeatDestCount++;
                    break;
                default:
                    listItem.setSysDestRackBarcode("");
                    break;
            }
            listItem.setUserDestRackBarcode(listItem.getSysDestRackBarcode());
        }

        return new ForwardResolution(DEFAULT_PAGE);
    }

    /**
     * For a forward from quant upload page, rack (or plate) barcodes need to be presented in the data table <br/>
     * There is no way a source vessel can be in more than one container going into a single quant run <br/>
     * Builds out all the pico transfer source containers (tube formations or static plates) and their barcodes
     */
    private Map<VesselContainer, String> buildSourceContainerMap(LabMetricRun labMetricRun) {
        Map<VesselContainer, String> containerLabelMap = new HashMap<>();
        for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
            // Capture only metrics associated with pico plate wells
            if (labMetric.getLabMetricDecision() == null) {
                LabVessel metricVessel = labMetric.getLabVessel();
                // Always going to be a PlateWell --> StaticPlate relationship
                VesselContainer<?> picoPlate = metricVessel.getContainers().iterator().next().getContainerRole();
                VesselContainer<?> srcContainer;
                LabVessel ancillaryVessel;
                for (SectionTransfer sectionTransfer : picoPlate.getSectionTransfersTo()) {
                    srcContainer = sectionTransfer.getSourceVesselContainer();
                    ancillaryVessel = sectionTransfer.getAncillarySourceVessel();
                    containerLabelMap.put(srcContainer,
                            ancillaryVessel == null ? srcContainer.getEmbedder().getLabel() : ancillaryVessel.getLabel());
                }
                for (CherryPickTransfer cherryPickTransfer : picoPlate.getCherryPickTransfersTo()) {
                    srcContainer = cherryPickTransfer.getSourceVesselContainer();
                    ancillaryVessel = cherryPickTransfer.getAncillarySourceVessel();
                    containerLabelMap.put(srcContainer,
                            ancillaryVessel == null ? srcContainer.getEmbedder().getLabel() : ancillaryVessel.getLabel());
                }
                // Ignore VesselToSectionTransfer, no rack barcode available
            }
        }
        return containerLabelMap;
    }

    /**
     * Downloads CSV picker file
     */
    @HandlesEvent(EVT_DOWNLOAD_PICKER_CSV)
    public Resolution downloadPickerCsv() {
        StringBuilder csv = new StringBuilder();
        // 3 counters for NORM, UNDILUTED, REPEAT
        int normcount = 0;
        int undilCount = 0;
        int repeatCount = 0;

        VesselPosition[] positions = VesselGeometry.G12x8.getVesselPositions();

        // Destination racks
        MultiValuedMap<String, String> destSrcMultiMap = new ArrayListValuedHashMap<>();
        for (ListItem item : listItems) {
            destSrcMultiMap.put(item.getUserDestRackBarcode(), item.getBarcode());
        }

        // Src to destination positions  TODO:  JMS Validate user did not duplicate destination barcodes !!
        Map<String, String> srcToDestPosMap = new HashMap<>();
        for (String destBarcode : destSrcMultiMap.keySet()) {
            int count = 0;
            for (String srcBarcode : destSrcMultiMap.get(destBarcode)) {
                srcToDestPosMap.put(srcBarcode, positions[count++].name());
            }
        }

        for (ListItem item : listItems) {
            csv.append(item.getSrcRackBarcode()).append(",")
                    .append(item.getPosition()).append(",")
                    .append(item.getBarcode()).append(",")
                    .append(item.getUserDestRackBarcode()).append(",")
                    .append(srcToDestPosMap.get(item.getBarcode())).append("\r\n");
        }

        String fileName = getUserBean().getLoginUserName() + "_" + TIMESTAMP_FORMATTER.format(new Date()) + "_pico_pick.csv";
        setFileDownloadHeaders("application/octet-stream", fileName);
        StreamingResolution stream = new StreamingResolution("application/octet-stream",
                new ByteArrayInputStream(csv.toString().getBytes()));
        return stream;
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
        makeScanListItems(positionToTubeMap);
    }

    /**
     * Makes listItems from ajax position->tube map.
     */
    private void makeScanListItems(Map<VesselPosition, BarcodedTube> map) {
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
                        tube.getVolume(), sampleId, concentration, decision, "SOURCE"));
            } else {
                listItems.add(new ListItem(null, vesselPosition.name(), tube.getLabel(),
                        tube.getVolume(), sampleId, null, null, "SOURCE"));
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
