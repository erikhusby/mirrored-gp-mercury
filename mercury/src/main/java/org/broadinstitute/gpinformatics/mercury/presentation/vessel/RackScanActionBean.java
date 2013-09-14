package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.RackScannerEjb;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackScanner;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Action bean for rack scanning things.  This can be extended so as to avoid code duplication.
 * @see org.broadinstitute.gpinformatics.mercury.presentation.sample.ReceiveSamplesActionBean
 *
 * Additionally, this is setup for AJAX usage and retunrs the scan results jsp on a normal scan if you want to show
 * that as your results.  Or you can ignore that resolution and return your own (as the ReceiveSamplesActionBean does).
 */
public abstract class RackScanActionBean extends CoreActionBean {

    public static final String SHOW_SCANNING_JSP = "/vessel/rack_scan_summary.jsp";
    public static final String SCAN_RESULTS_JSP = "/vessel/rack_scan_results.jsp";
    public static final String SHOW_LAB_SELECTION_JSP = "/vessel/rack_scan_lab.jsp";
    public static final String SCAN_EVENT = "scan";
    public static final String SHOW_SCAN_SELECTION_EVENT = "showScanSelection";
    public static final String SHOW_LAB_SELECTION_EVENT = "showLabSelection";

    private SortedSet<RackScanner> rackScanners = new TreeSet<>(RackScanner.BY_NAME);

    /**
     * Loads the rack scanners based upon the lab to filter by.
     */
    @After(stages = LifecycleStage.BindingAndValidation)
    public void loadRackScanners() {

        if (labToFilterBy == null) {
            rackScanners.addAll(Arrays.asList(RackScanner.values()));
        } else {
            for (RackScanner scanner : RackScanner.values()) {
                if (labToFilterBy == scanner.getRackScannerLab()) {
                    rackScanners.add(scanner);
                }
            }
        }
    }

    @Inject
    protected RackScannerEjb rackScannerEjb;

    /** Selected rack scanner. */
    private RackScanner rackScanner;

    /** Lab to filter the rack scanners by. */
    private RackScanner.RackScannerLab labToFilterBy;

    /** Resulting rack scan. */
    protected LinkedHashMap<String,String> rackScan;

    /**
     * Shows a page where you can select the lab you're using.
     */
    @DefaultHandler
    @HandlesEvent(SHOW_LAB_SELECTION_EVENT)
    public Resolution showLabSelection() {
        return new ForwardResolution(SHOW_LAB_SELECTION_JSP);
    }

    /**
     * Shows the scan selection page filtered by the lab selected.
     *
     * @throws Exception
     */
    @HandlesEvent(SHOW_SCAN_SELECTION_EVENT)
    public Resolution showScanSelection() throws Exception {
        return new ForwardResolution(SHOW_SCANNING_JSP);
    }

    /** Does a rack scan and sets the rackScan variable. Returns a default results jsp. */
    @HandlesEvent(SCAN_EVENT)
    public Resolution scan() throws ScannerException {

        rackScan = rackScannerEjb.runRackScanner(rackScanner);

        return new ForwardResolution(SCAN_RESULTS_JSP);
    }

    /** Shows the possible labs rack scanners are in. */
    public RackScanner.RackScannerLab[] getAllLabs() {
        return RackScanner.RackScannerLab.values();
    }

    /** Url for the rack scanning page.  */
    public abstract String getRackScanPageUrl();

    public RackScanner.RackScannerLab getLabToFilterBy() {
        return labToFilterBy;
    }

    public void setLabToFilterBy(RackScanner.RackScannerLab labToFilterBy) {
        this.labToFilterBy = labToFilterBy;
    }

    public LinkedHashMap<String, String> getRackScan() {
        return rackScan;
    }

    public void setRackScan(LinkedHashMap<String, String> rackScan) {
        this.rackScan = rackScan;
    }

    public RackScanner getRackScanner() {
        return rackScanner;
    }

    public void setRackScanner(RackScanner rackScanner) {
        this.rackScanner = rackScanner;
    }

    public SortedSet<RackScanner> getRackScanners() {
        return rackScanners;
    }

    public void setRackScanners(SortedSet<RackScanner> rackScanners) {
        this.rackScanners = rackScanners;
    }
}
