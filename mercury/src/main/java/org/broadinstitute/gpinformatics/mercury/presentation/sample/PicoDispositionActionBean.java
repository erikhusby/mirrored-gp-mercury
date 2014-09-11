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
import java.util.Iterator;
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
    public static final String PAGE_TITLE = "Initial Pico Sample Disposition";

    public static final String NEXT_STEPS_PAGE = "/sample/pico_disp.jsp";
    public static final String NEXT_STEPS_SCAN_PAGE = "/sample/pico_disp_scan.jsp";
    public static final String CONFIRM_REARRAY_PAGE = "/sample/pico_disp_rearray.jsp";
    public static final String CONFIRM_REARRAY_RACK_SCAN_PAGE = "/sample/pico_disp_rearray_scan.jsp";

    public static final String REVIEW_SCANNED_RACK_EVENT = "reviewScannedRack";
    public static final String CONFIRM_REARRAY_EVENT = "confirmRearray";
    public static final String CONFIRM_REARRAY_SCAN_EVENT = "confirmRearrayScan";

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

    // Indicates the selection of next step by the user during rearray confirm.
    private NextStep nextStepSelect;

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

    /** The list of next steps that can possibly apply to tubes after a manual rearray. */
    public List<NextStep> getConfirmableNextSteps() {
        return new ArrayList<NextStep>() {{
            add(NextStep.SHEARING_DAUGHTER);
            add(NextStep.FP_DAUGHTER);
        }};
    }

    public String getNextStepSelect() {
        return nextStepSelect != null ? nextStepSelect.getStepName() : "";
    }

    public void setNextStepSelect(String nextStepName) {
        nextStepSelect = NextStep.getNextStep(nextStepName);
    }

    public String getShowLabSelectionEvent() {
        return SHOW_LAB_SELECTION_EVENT;
    }

    public String getConfirmRearrayPage() {
        return CONFIRM_REARRAY_PAGE;
    }

    public String getConfirmRearrayScanEvent() {
        return CONFIRM_REARRAY_SCAN_EVENT;
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
        FP_DAUGHTER("FP Daughter", 2, 2),
        SHEARING_DAUGHTER("Shearing Daughter", 1, 1),
        SHEARING_DAUGHTER_AT_RISK("Shearing Daughter (At Risk)", 1, 1),
        EXCLUDE("Exclude", 0, 0),
        NULL("", -1, 0);  // Out of band enum used when metric is missing.

        private String stepName;
        private int sortOrder;
        // nextStepConfirmationGroup
        private int nextStepConfirmationGroup;

        private NextStep(String stepName, int sortOrder, int nextStepConfirmationGroup) {
            this.stepName = stepName;
            this.sortOrder = sortOrder;
            this.nextStepConfirmationGroup = nextStepConfirmationGroup;
        }

        public String getStepName() {
            return stepName;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        public int getNextStepConfirmationGroup() {
            return nextStepConfirmationGroup;
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

        public static NextStep getNextStep(String stepName) {
            for (NextStep nextStep : values()) {
                if (nextStep.getStepName().equals(stepName)) {
                    return nextStep;
                }
            }
            return null;
        }
    }

    /**
     * Populates the list of pico sample dispositions for the jsp to display.
     */
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution displayList() {
        makeListItems(tubeFormationLabel, tubeFormation);
        return new ForwardResolution(NEXT_STEPS_PAGE);
    }

    @HandlesEvent(REVIEW_SCANNED_RACK_EVENT)
    public Resolution reviewScannedRack() {
        return new ForwardResolution(NEXT_STEPS_SCAN_PAGE);
    }

    @HandlesEvent(CONFIRM_REARRAY_EVENT)
    public Resolution confirmRearray() {
        return new ForwardResolution(CONFIRM_REARRAY_RACK_SCAN_PAGE);
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
     * Uses a rack scanner to find tubes and generates their next step dispositions.
     * @throws org.broadinstitute.bsp.client.rackscan.ScannerException
     */
    @Override
    @HandlesEvent(SCAN_EVENT)
    public Resolution scan() throws ScannerException {
        boolean ok = scanAndMakeListItems();
        return new ForwardResolution(ok ? NEXT_STEPS_PAGE : NEXT_STEPS_SCAN_PAGE);
    }

    /**
     * Uses a rack scanner to find rearrayed tubes to confirm.
     * @throws org.broadinstitute.bsp.client.rackscan.ScannerException
     */
    @HandlesEvent(CONFIRM_REARRAY_SCAN_EVENT)
    public Resolution confirmRearrayScan() throws ScannerException {
        // Default is to revisit scan input page.
        String nextPage = CONFIRM_REARRAY_RACK_SCAN_PAGE;
        if (nextStepSelect == null) {
            addMessage("Missing Next Step selection.");
            setLabToFilterBy(null);
        } else if (scanAndMakeListItems()) {
            // Removes tubes with the expected Next Step confirmationGroup value,
            // leaving only incorrect ones in listItems.
            final int expectedGroup = nextStepSelect.getNextStepConfirmationGroup();
            for (Iterator<ListItem> iter = listItems.iterator(); iter.hasNext(); ) {
                if (iter.next().getDisposition().getNextStepConfirmationGroup() == expectedGroup) {
                    iter.remove();
                }
            }
            nextPage = CONFIRM_REARRAY_PAGE;
        }
        return new ForwardResolution(nextPage);
    }

    private boolean scanAndMakeListItems() throws ScannerException {
        if (getRackScanner() == null) {
            addMessage("Missing rack scanner selection.");
            setLabToFilterBy(null);
            return false;
        }
        // Runs the rack scanner.  Ignores the returned Stripes Resolution and
        // uses the map of position->tubeBarcode.
        super.scan();
        makeListItems(rackScan);
        return true;
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
            } else {
                listItems.add(new ListItem(vesselPosition.name(), tube.getLabel(), null, null, false));
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
                int o1DispositionOrder = (o1.getDisposition() != null) ?
                        o1.getDisposition().getSortOrder() : NextStep.NULL.getSortOrder();
                int o2DispositionOrder = (o2.getDisposition() != null) ?
                        o2.getDisposition().getSortOrder() : NextStep.NULL.getSortOrder();
                int compareResult = Integer.compare(o1DispositionOrder, o2DispositionOrder);
                return (compareResult != 0) ? compareResult :  o1.getPosition().compareTo(o2.getPosition());
            }};

}
