package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Allows a user to declare the positive and negative controls in an LCSET.
 */
@UrlBinding(LcsetActionBean.ACTION_BEAN_URL)
public class LcsetActionBean extends RackScanActionBean {
    static final String ACTION_BEAN_URL = "/workflow/Lcset.action";
    private static final String PAGE_TITLE = "LCSET";

    private static final String LCSET_PAGE = "/workflow/lcset_controls.jsp";

    public static final String SCAN_CONTROLS_EVENT = "scanControls";
    public static final String CONFIRM_CONTROLS_EVENT = "confirmControls";

    /** Entered by the user. */
    @Validate(required = true, on = {SCAN_CONTROLS_EVENT, CONFIRM_CONTROLS_EVENT})
    private String lcsetName;
    // todo jmt error if rack barcode is needed, and not supplied.
    private String rackBarcode;

    // todo jmt prevent these from accumulating barcodes for successive scans
    /** Ask the user to confirm that these should be added to the LCSET. */
    private List<String> controlBarcodes = new ArrayList<>();
    private List<String> addBarcodes = new ArrayList<>();
    private List<String> removeBarcodes = new ArrayList<>();

    private VesselGeometry vesselGeometry = RackOfTubes.RackType.Matrix96.getVesselGeometry();

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    /**
     * Populates the list of pico sample dispositions for the jsp to display.
     */
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(LCSET_PAGE);
    }

    @HandlesEvent(SCAN_CONTROLS_EVENT)
    public Resolution scanControls() throws ScannerException {
        scan();
        MessageCollection messageCollection = new MessageCollection();
        Iterator<Map.Entry<String, String>> it = rackScan.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            if (pair.getValue().isEmpty()) {
                it.remove();
            }
        }

        LabBatchEjb.ValidateRackScanReturn validateRackScanReturn = labBatchEjb.validateRackScan(lcsetName,
                rackScan, messageCollection);
        for (LabVessel labVessel : validateRackScanReturn.getControlTubes()) {
            controlBarcodes.add(labVessel.getLabel());
        }
        for (LabVessel labVessel : validateRackScanReturn.getAddTubes()) {
            addBarcodes.add(labVessel.getLabel());
        }
        for (LabVessel labVessel : validateRackScanReturn.getRemoveTubes()) {
            removeBarcodes.add(labVessel.getLabel());
        }

        addMessages(messageCollection);
        return new ForwardResolution(LCSET_PAGE);
    }

    @HandlesEvent(CONFIRM_CONTROLS_EVENT)
    public Resolution confirmControls() throws ScannerException {
        labBatchEjb.updateLcsetFromScan(lcsetName, controlBarcodes, this, addBarcodes, removeBarcodes, rackScan,
                rackBarcode, userBean);
        addMessage("Made modifications to LCSET");
        return new ForwardResolution(LCSET_PAGE);
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    public String getLcsetName() {
        return lcsetName;
    }

    @SuppressWarnings("unused")
    public void setLcsetName(String lcsetName) {
        this.lcsetName = lcsetName;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public void setRackBarcode(String rackBarcode) {
        this.rackBarcode = rackBarcode;
    }

    public List<String> getControlBarcodes() {
        return controlBarcodes;
    }

    @SuppressWarnings("unused")
    public void setControlBarcodes(List<String> controlBarcodes) {
        this.controlBarcodes = controlBarcodes;
    }

    public List<String> getAddBarcodes() {
        return addBarcodes;
    }

    @SuppressWarnings("unused")
    public void setAddBarcodes(List<String> addBarcodes) {
        this.addBarcodes = addBarcodes;
    }

    public List<String> getRemoveBarcodes() {
        return removeBarcodes;
    }

    @SuppressWarnings("unused")
    public void setRemoveBarcodes(List<String> removeBarcodes) {
        this.removeBarcodes = removeBarcodes;
    }

    public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }
}
