package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action bean that shows per-sample disposition after initial pico.
 * Sorting by multiple columns is the main trick by this class.
 */
@UrlBinding(value = "/sample/PicoDisposition.action")
public class PicoDispositionActionBean extends CoreActionBean {
    public static String PICO_DISPOSTION_PAGE = "/sample/picoDisposition.jsp";

    @Inject
    private TubeFormationDao tubeFormationDao;

    @Inject
    private LabVesselDao labVesselDao;

    // Spreadsheet upload page invokes this action bean and passes in the tubeFormationLabel parameter.
    private String tubeFormationLabel;

    // ListItem is one row of the "next step" table shown by the jsp.
    private List<ListItem> listItems = new ArrayList<>();

    // With the "rack scan" button, the rack barcode is used to find tubes to display.
    private String rackBarcode = null;

    // Sorting options set by jsp checkboxes.
    private boolean sortOnPosition = true;
    private boolean sortOnConcentration = false;
    private boolean sortOnNextStep = true;
    private boolean reversePositionOrder = false;
    private boolean reverseConcentrationOrder = false;
    private boolean reverseNextStepOrder = false;

    // TubeFormation accessible only for test purposes.
    private TubeFormation tubeFormation;


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

    public boolean isSortOnPosition() {
        return sortOnPosition;
    }

    public void setSortOnPosition(boolean sortOnPosition) {
        this.sortOnPosition = sortOnPosition;
    }

    public boolean isSortOnConcentration() {
        return sortOnConcentration;
    }

    public void setSortOnConcentration(boolean sortOnConcentration) {
        this.sortOnConcentration = sortOnConcentration;
    }

    public boolean isSortOnNextStep() {
        return sortOnNextStep;
    }

    public void setSortOnNextStep(boolean sortOnNextStep) {
        this.sortOnNextStep = sortOnNextStep;
    }

    public boolean isReversePositionOrder() {
        return reversePositionOrder;
    }

    public void setReversePositionOrder(boolean reversePositionOrder) {
        this.reversePositionOrder = reversePositionOrder;
    }

    public boolean isReverseConcentrationOrder() {
        return reverseConcentrationOrder;
    }

    public void setReverseConcentrationOrder(boolean reverseConcentrationOrder) {
        this.reverseConcentrationOrder = reverseConcentrationOrder;
    }

    public boolean isReverseNextStepOrder() {
        return reverseNextStepOrder;
    }

    public void setReverseNextStepOrder(boolean reverseNextStepOrder) {
        this.reverseNextStepOrder = reverseNextStepOrder;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public void setRackBarcode(String rackBarcode) {
        this.rackBarcode = rackBarcode;
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
        private String row;
        private int column;
        private String barcode;
        private BigDecimal concentration;
        private NextStep disposition;
        private boolean override;

        public ListItem(String position, String barcode, BigDecimal concentration,  NextStep disposition,
                        boolean override) {
            this.position = position;
            row = StringUtils.isAlpha(position.substring(0,1)) ? position.substring(0,1) : "";
            String col = StringUtils.isAlpha(position.substring(0,1)) ? position.substring(1) : position;
            column = StringUtils.isNumeric(col) ? Integer.parseInt(col) : 0;
            this.barcode = barcode;
            this.concentration = concentration;
            this.disposition = disposition;
            this.override = override;
        }

        public String getPosition() {
            return position;
        }
        public String getRow() {
            return row;
        }
        public int getColumn() {
            return column;
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
        // Indicates which NextStep to use when concentration is below range (-1), in range (0), above range (+1).
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

        public static NextStep getNextStepForRangeCompare(int compareResult) {
            return mapRangeCompareToNextStep.get(compareResult);
        }
    }

    /**
     * Populates the list of pico sample dispositions for the jsp to display.
     */
    @DefaultHandler
    public Resolution displayList() {

        // If no ListItems exist, finds the tubes and creates a ListItem for each one.
        if (CollectionUtils.isEmpty(listItems) &&
            (tubeFormation != null || StringUtils.isNotBlank(tubeFormationLabel))) {

            if (tubeFormation == null) {
                tubeFormation = tubeFormationDao.findByDigest(tubeFormationLabel);
            }
            if (tubeFormation == null) {
                createSafeErrorMessage("Cannot find tube formation having label '" + tubeFormationLabel + "'");
            } else {
                makeListItems(tubeFormation);
            }
        }

        sortList();
        return new ForwardResolution(PICO_DISPOSTION_PAGE);
    }

    /**
     * Does a rack scan and uses the rack as input to the sample disposition list.
     */
    public Resolution scanRack() {
        listItems.clear();

        // xxx todo get rackBarcode from somewhere

        if (StringUtils.isBlank(rackBarcode)) {
            createSafeErrorMessage("Rack scan barcode is null.");
        } else {

            Map<String,LabVessel> map = labVesselDao.findByBarcodes(Collections.singletonList(rackBarcode));
            if (map == null) {
                createSafeErrorMessage("Cannot find rack for barcode '" + rackBarcode + "'");
            }
            LabVessel rack = map.get(rackBarcode);

            if (OrmUtil.proxySafeIsInstance(rack, VesselContainerEmbedder.class)) {
                makeListItems(OrmUtil.proxySafeCast(rack, VesselContainerEmbedder.class));
                sortList();
            } else {
                createSafeErrorMessage("Barcode '" + rackBarcode + "' is not from a rack of tubes.");
            }
        }
        return new ForwardResolution(PICO_DISPOSTION_PAGE);
    }

    // Makes a ListItem for every tube in the rack.
    private void makeListItems(VesselContainerEmbedder vesselContainer) {
        listItems.clear();
        if (vesselContainer != null && vesselContainer.getContainerRole() != null) {
            Map<VesselPosition, BarcodedTube> mapPositionToTube =
                    vesselContainer.getContainerRole().getMapPositionToVessel();

            for (VesselPosition vesselPosition : mapPositionToTube.keySet()) {
                BarcodedTube tube = mapPositionToTube.get(vesselPosition);
                int rangeCompare = tube.concentrationInFingerprintRange();
                boolean override = false; //xxx todo get this from somewhere in labVessel

                listItems.add(new ListItem(vesselPosition.name(), tube.getLabel(), tube.getConcentration(),
                        NextStep.getNextStepForRangeCompare(rangeCompare), override));
            }
        }
    }

    // Sorts the ListItems according to the current sort settings.
    private void sortList() {
        // Builds a comparator for sorting the list.  Applies multiple key sorting in this order:
        // first by nextStep, then by concentration, then by position, then by barcode.
        Comparator<ListItem> listItemComparator = new Comparator<ListItem>() {
            @Override
            public int compare(ListItem o1, ListItem o2) {
                if (sortOnNextStep) {
                    int compareResult = Integer.compare(o1.getDisposition().getRangeCompare(),
                            o2.getDisposition().getRangeCompare());
                    if (compareResult != 0) {
                        return reverseNextStepOrder ? -compareResult : compareResult;
                    }
                }
                if (sortOnConcentration) {
                    int compareResult = o1.getConcentration().compareTo(o2.getConcentration());
                    if (compareResult != 0) {
                        return reverseConcentrationOrder ? -compareResult : compareResult;
                    }
                }
                if (sortOnPosition) {
                    int compareRow = o1.getRow().compareTo(o2.getRow());
                    int compareCol = Integer.compare(o1.getColumn(), o2.getColumn());
                    if (reversePositionOrder) {
                        return (compareRow != 0) ? compareRow : compareCol;
                    } else {
                        return (compareCol != 0) ? compareCol : compareRow;
                    }
                }
                return o1.getBarcode().compareTo(o2.getBarcode());
            }};

        Collections.sort(listItems, listItemComparator);
    }


}
