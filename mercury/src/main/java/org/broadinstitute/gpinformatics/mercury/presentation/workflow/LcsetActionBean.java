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
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows a user to declare the positive and negative controls in an LCSET.
 */
@UrlBinding(value = LcsetActionBean.ACTION_BEAN_URL)
public class LcsetActionBean extends RackScanActionBean {
    public static final String ACTION_BEAN_URL = "/workflow/Lcset.action";
    public static final String PAGE_TITLE = "LCSET";

    public static final String LCSET_PAGE = "/workflow/lcset_controls.jsp";

    public static final String SCAN_CONTROLS_EVENT = "scanControls";
    public static final String CONFIRM_CONTROLS_EVENT = "confirmControls";

    /** Entered by the user. */
    @Validate(required = true, on = {SCAN_CONTROLS_EVENT, CONFIRM_CONTROLS_EVENT})
    private String lcsetName;

    /** Ask the user to confirm that these should be added to the LCSET. */
    private List<String> controlBarcodes = new ArrayList<>();

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
        controlBarcodes = labBatchEjb.findControlsInRackScan(lcsetName, rackScan, messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(LCSET_PAGE);
    }

    @HandlesEvent(CONFIRM_CONTROLS_EVENT)
    public Resolution confirmControls() throws ScannerException {
        labBatchEjb.addControlsToLcset(lcsetName, controlBarcodes);
        addMessage("Added controls to LCSET");
        return new ForwardResolution(LCSET_PAGE);
    }

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

    public void setLcsetName(String lcsetName) {
        this.lcsetName = lcsetName;
    }

    public List<String> getControlBarcodes() {
        return controlBarcodes;
    }

    public void setControlBarcodes(List<String> controlBarcodes) {
        this.controlBarcodes = controlBarcodes;
    }
}
