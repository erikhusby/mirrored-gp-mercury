package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.RackScanner;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.rackscan.geometry.Dimension;
import org.broadinstitute.bsp.client.rackscan.geometry.Geometry;
import org.broadinstitute.bsp.client.rackscan.geometry.index.AlphaNumeric;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.RackScannerEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Action bean for rack scanning things.  This can be extended so as to avoid code duplication.
 * @see org.broadinstitute.gpinformatics.mercury.presentation.sample.ReceiveSamplesActionBean
 *
 * Additionally, this is setup for AJAX usage and returns the scan results jsp on a normal scan if you want to show
 * that as your results.  Or you can ignore that resolution and return your own (as the ReceiveSamplesActionBean does).
 */
public abstract class RackScanActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(CoreActionBean.class);

    public static final String SHOW_SCANNING_JSP = "/vessel/rack_scanner_list.jsp";
    public static final String SCAN_RESULTS_JSP = "/vessel/rack_scan_results.jsp";
    public static final String SHOW_LAB_SELECTION_JSP = "/vessel/rack_scan_lab_select.jsp";
    public static final String SCAN_EVENT = "scan";
    public static final String SHOW_SCAN_SELECTION_EVENT = "showScanSelection";
    public static final String SHOW_LAB_SELECTION_EVENT = "showLabSelection";

    /**
     * Loads the rack scanners based upon the lab to filter by.
     */
    @After(stages = LifecycleStage.BindingAndValidation)
    public void loadRackScanners() {

        if (labToFilterBy == null) {
            rackScanners.addAll(Arrays.asList(RackScanner.values()));
        } else {
            rackScanners.addAll(RackScanner.getRackScannersByLab(labToFilterBy));
        }
    }

    private SortedSet<RackScanner> rackScanners = new TreeSet<>(RackScanner.BY_NAME);

    @Inject
    protected RackScannerEjb rackScannerEjb;

    /** Selected rack scanner. */
    private RackScanner rackScanner;

    /** Lab to filter the rack scanners by. */
    private RackScanner.RackScannerLab labToFilterBy;

    /** Resulting rack scan. */
    protected LinkedHashMap<String,String> rackScan;

    /**
     * Determines whether jsp will append results to existing page,
     * or do essentially a page forward to the results page.
     */
    protected boolean appendScanResults = true;

    public boolean isAppendScanResults() {
        return appendScanResults;
    }

    public void setAppendScanResults(boolean appendScanResults) {
        this.appendScanResults = appendScanResults;
    }

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

        try {
            rackScan = rackScannerEjb.runRackScanner(rackScanner);
        } catch (Exception e) {
            log.error(e);
            addGlobalValidationError("Error connecting to the rack scanner. " + e.getMessage());

            rackScan = new LinkedHashMap<>();
            for (String position : getMatrixPositions()) {
                rackScan.put(position, "");
            }
        }

        return new ForwardResolution(SCAN_RESULTS_JSP);
    }

    /** Shows the possible labs rack scanners are in. */
    public List<RackScanner.RackScannerLab> getAllLabs() {

        List<RackScanner.RackScannerLab> labsBySoftwareSystems;
        if (ApplicationInstance.CRSP.isCurrent()) {
            labsBySoftwareSystems =
                    RackScanner.RackScannerLab.getLabsBySoftwareSystems(RackScanner.SoftwareSystem.CRSP_BSP);
        } else {
            labsBySoftwareSystems = RackScanner.RackScannerLab.getLabsBySoftwareSystems(RackScanner.SoftwareSystem.BSP,
                    RackScanner.SoftwareSystem.MERCURY);
        }
        return labsBySoftwareSystems;
    }

    public List<String> getMatrixPositions() {
        Geometry geometry = new Geometry();
        geometry.setDimension(new Dimension(8, 12));
        geometry.setIndexing(new AlphaNumeric('A', 1));
        return geometry.getValidPositions();
    }

    /** Event for the lab selection. */
    public String getShowScanSelectionEvent() {
        return SHOW_SCAN_SELECTION_EVENT;
    }

    public String getScanEvent() {
        return SCAN_EVENT;
    }

    /** Url for the rack scanning page.  */
    public abstract String getRackScanPageUrl();

    public abstract String getPageTitle();

    public String getRackScanPageJsp() {
        return SHOW_LAB_SELECTION_JSP;
    }

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
