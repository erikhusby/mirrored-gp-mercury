package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
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
     * ListItem represents a lab vessel displayed in the jsp's list.
     */
    public class ListItem {
        private String position;
        private String barcode;
        private BigDecimal concentration;
        private NextStep disposition;
        private boolean override;

        public ListItem(String position, String barcode, BigDecimal concentration,  NextStep disposition,
                        boolean override) {
            this.position = position;
            this.barcode = barcode;
            // Limits the decimal digits displayed.
            this.concentration = (concentration != null) ? concentration.setScale(2, RoundingMode.HALF_EVEN) : null;
            this.disposition = disposition;
            this.override = override;
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
        public boolean isOverride() {
            return override;
        }
    }
    private static Map<Integer, NextStep> mapRangeCompareToNextStep = new HashMap<>();

    /**
     * NextStep describes what should happen next to a tube after initial pico.
     */
    public enum NextStep {
        FP_DAUGHTER("FP Daughter", 1),
        SHEARING_DAUGHTER("Shearing Daughter", 0),
        EXCLUDE("Exclude", -1);

        private String stepName;

        // rangeCompare indicates below range (-1), in range (0), above range (+1).
        // It is also used for sorting a list of NextStep.
        private int rangeCompare;

        private NextStep(String stepName, int rangeCompare) {
            this.stepName = stepName;
            this.rangeCompare = rangeCompare;
            mapRangeCompareToNextStep.put(rangeCompare, this);
        }

        public String getStepName() {
            return stepName;
        }

        public int getRangeCompare() {
            return rangeCompare;
        }

        /** Returns the NextStep for the given NextStep.rangeCompare, or null if not found. */
        public static NextStep getNextStepForRangeCompare(int compareResult) {
            return mapRangeCompareToNextStep.get(compareResult);
        }
    }

    /**
     * Populates the list of pico sample dispositions for the jsp to display.
     */
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution displayList() {
        if (StringUtils.isBlank(tubeFormationLabel) && tubeFormation == null) {
            addMessage("tubeFormationLabel is missing");
        }
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



    // Makes all listItems from a tube formation label, or the tube formation entity.
    private void makeListItems(String label, TubeFormation container) {
        listItems.clear();
        if (container != null || StringUtils.isNotBlank(label)) {
            if (container == null) {
                container = tubeFormationDao.findByDigest(label);
            }
            if (container == null || container.getContainerRole() == null) {
                addMessage("Cannot find tube formation having label '" + label + "'");
            } else {
                makeListItems(container.getContainerRole().getMapPositionToVessel());
            }
        }
    }

    // Makes all listItems from a position->barcode map.
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
        if (positionToTubeMap.size() == 0) {
            addMessage("Scanned rack scan has no tubes.");
        }
        makeListItems(positionToTubeMap);
    }

    // Makes all listItems from a position->tube map.
    private void makeListItems(Map<VesselPosition, BarcodedTube> map) {
        for (VesselPosition vesselPosition : map.keySet()) {
            BarcodedTube tube = map.get(vesselPosition);
            int rangeCompare = tube.concentrationInFingerprintRange();
            boolean override = false; //xxx todo get this from somewhere in labVessel?
            listItems.add(new ListItem(vesselPosition.name(), tube.getLabel(), tube.getConcentration(),
                    NextStep.getNextStepForRangeCompare(rangeCompare), override));
        }
        Collections.sort(listItems, BY_DISPOSITION_THEN_POSITION);
    }

    private static final Comparator<ListItem> BY_DISPOSITION_THEN_POSITION = new Comparator<ListItem>() {
            @Override
            public int compare(ListItem o1, ListItem o2) {
                int compareResult = Integer.compare(o1.getDisposition().getRangeCompare(),
                        o2.getDisposition().getRangeCompare());
                return (compareResult != 0) ? compareResult :  o1.getPosition().compareTo(o2.getPosition());
            }};

}
