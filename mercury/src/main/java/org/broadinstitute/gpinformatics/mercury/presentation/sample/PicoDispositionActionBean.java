package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanPlateProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Action bean that shows per-sample disposition after initial pico.
 * Sorting by multiple columns is the main trick by this class.
 */
@UrlBinding(value = PicoDispositionActionBean.ACTION_BEAN_URL)
public class PicoDispositionActionBean extends RackScanActionBean {
    public static final String ACTION_BEAN_URL = "/sample/PicoDisposition.action";
    public static final String PICO_DISPOSTION_PAGE = "/sample/picoDisposition.jsp";
    public static final String PICO_DISPOSTION_RACK_SCAN_PAGE = "/sample/picoDispositionRackScan.jsp";
    public static final String PAGE_TITLE = "Initial Pico Sample Disposition";

    @Inject
    private TubeFormationDao tubeFormationDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private LabMetricDao labMetricDao;

    // Spreadsheet upload page invokes this action bean and passes in the tubeFormationLabel parameter.
    private String tubeFormationLabel;

    // ListItem is one row of the "next step" table shown by the jsp.
    private List<ListItem> listItems = new ArrayList<>();

    private TubeFormation tubeFormation;
    private static final Map<VesselPosition, BarcodedTube> positionToTubeMap = new HashMap<>();


    public String getTubeFormationLabel() {
        return tubeFormationLabel;
    }

    public void setTubeFormationLabel(String tubeFormationLabel) {
        this.tubeFormationLabel = tubeFormationLabel;
    }

    public List<ListItem> getListItems() {
        return listItems;
    }

    public void setListItems(List<ListItem> listItems) {
        this.listItems = listItems;
    }

    // TubeFormation accessible only for test purposes.
    TubeFormation getTubeFormation() {
        return tubeFormation;
    }

    // TubeFormation accessible only for test purposes.
    void setTubeFormation(TubeFormation tubeFormation) {
        this.tubeFormation = tubeFormation;
    }

    /**
     * SampleDisposition represents a sample disposition displayed in the jsp's list.
     */
    public class ListItem {
        private String position;
        private String barcode;
        private BigDecimal concentration;
        private NextStep disposition;
        private boolean riskOverride;

        public ListItem(String position, String barcode, BigDecimal concentration,  NextStep disposition,
                        boolean riskOverride) {
            this.position = position;
            this.barcode = barcode;
            // Sets the number of decimal digits to display.
            this.concentration = (concentration != null) ?
                    concentration.setScale(VarioskanPlateProcessor.SCALE, RoundingMode.HALF_EVEN) : null;
            this.disposition = disposition;
            this.riskOverride = riskOverride;
        }

        public String getPosition() {
            return position;
        }
        public String getBarcode() {
            return barcode;
        }
        public BigDecimal getConcentration() {
            return concentration;
        }
        public NextStep getDisposition() {
            return disposition;
        }
        public boolean hasRiskOverride() {
            return riskOverride;
        }
    }

    /**
     * NextStep describes what should happen next to a tube after initial pico.
     */
    public enum NextStep {
        FP_DAUGHTER("FP Daughter", 2),
        SHEARING_DAUGHTER("Shearing Daughter", 1),
        SHEARING_DAUGHTER_AT_RISK("Shearing Daughter (At Risk)", 1),
        EXCLUDE("Exclude", 0);

        private String stepName;
        private int sortOrder;

        private NextStep(String stepName, int sortOrder) {
            this.stepName = stepName;
            this.sortOrder = sortOrder;
        }

        public String getStepName() {
            return stepName;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        /**
         * Returns the NextStep based on quant range comparison and at risk override.
         * @param rangeComparison  +, -, or 0  indicating quant is above, below, or in range.
         * @param override   true if user accepted this sample despite its low quant.
         */
        public static NextStep calculateNextStep(int rangeComparison, boolean override) {
            if (rangeComparison < 0) {
                return override ? SHEARING_DAUGHTER_AT_RISK : EXCLUDE;
            } else if (rangeComparison == 0) {
                return SHEARING_DAUGHTER;
            }
            return FP_DAUGHTER;
        }
    }

    /**
     * Populates the list of pico sample dispositions for the jsp to display.
     */
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution displayList() {
        makeListItems(tubeFormationLabel, tubeFormation);
        return new ForwardResolution(PICO_DISPOSTION_PAGE);
    }

    @HandlesEvent("setupScanner")
    public Resolution setupScanner() {
        setAppendScanResults(false);
        return new ForwardResolution(PICO_DISPOSTION_RACK_SCAN_PAGE);
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    /**
     * Uses a rack scanner to find tubes.
     * @throws org.broadinstitute.bsp.client.rackscan.ScannerException
     */
    @Override
    @HandlesEvent(SCAN_EVENT)
    public Resolution scan() throws ScannerException {
        listItems.clear();
        if (getRackScanner() == null) {
            addMessage("No rack scanner is selected.");
        } else {
            // Runs the rack scanner.  Ignores the returned Stripes Resolution and
            // uses the map of position->tubeBarcode.
            super.scan();
            makeListItems(rackScan);
        }
        return new ForwardResolution(PICO_DISPOSTION_PAGE);
    }

    /** Makes listItems from either a tube formation or its label. */
    private void makeListItems(String label, TubeFormation container) {
        listItems.clear();
        if (StringUtils.isNotBlank(label) && container == null) {
            container = tubeFormationDao.findByDigest(label);
            if (container == null || container.getContainerRole() == null) {
                addMessage("Cannot find tube formation having label '" + label + "'");
            }
        }
        if (container != null) {
            makeListItems(container.getContainerRole().getMapPositionToVessel());
        }
    }

    /** Makes listItems from a position->barcode map. */
    private void makeListItems(LinkedHashMap<String, String> positionToBarcodeMap) {
        // First makes a map of position->tube.
        positionToTubeMap.clear();
        for (String position : positionToBarcodeMap.keySet()) {
            String tubeBarcode = positionToBarcodeMap.get(position);
            if (StringUtils.isNotBlank(tubeBarcode)) {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(tubeBarcode);
                if (tube == null) {
                    addMessage("Cannot find tube having barcode '" + tubeBarcode + "'");
                } else {
                    VesselPosition vesselPosition = VesselPosition.getByName(position);
                    if (vesselPosition == null) {
                        addMessage("Unknown vessel position '" + position + "'");
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
            LabMetric labMetric = findMostRecentInitialPicoMetric(tube);
            if (labMetric != null) {
                BigDecimal concentration = labMetric.getValue();
                boolean riskOverride =
                        labMetric.getLabMetricDecision().getDecision().equals(LabMetricDecision.Decision.RISK);
                int rangeCompare = labMetric.initialPicoDispositionRange();
                listItems.add(new ListItem(vesselPosition.name(), tube.getLabel(), concentration,
                        NextStep.calculateNextStep(rangeCompare, riskOverride), riskOverride));

            }
        }
        Collections.sort(listItems, BY_DISPOSITION_THEN_POSITION);
    }

    /** Looks up the most recent initial pico quant for the tube. */
    private LabMetric findMostRecentInitialPicoMetric(BarcodedTube tube) {
        LabMetric latestInitialPico = null;
        for (LabMetric labMetric : tube.getMetrics()) {
            if (labMetric.getName().equals(LabMetric.MetricType.INITIAL_PICO) &&
                (latestInitialPico == null || latestInitialPico.getCreatedDate() == null ||
                 (labMetric.getCreatedDate() != null &&
                  labMetric.getCreatedDate().after(latestInitialPico.getCreatedDate())))) {
                latestInitialPico = labMetric;
            }
        }
        return latestInitialPico;
    }

    private static final Comparator<ListItem> BY_DISPOSITION_THEN_POSITION = new Comparator<ListItem>() {
            @Override
            public int compare(ListItem o1, ListItem o2) {
                int compareResult = Integer.compare(o1.getDisposition().getSortOrder(),
                        o2.getDisposition().getSortOrder());
                return (compareResult != 0) ? compareResult :  o1.getPosition().compareTo(o2.getPosition());
            }};

}
