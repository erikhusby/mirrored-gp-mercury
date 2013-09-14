package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.RackScannerEjb;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackScanner;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeSet;

@UrlBinding(RackScanActionBean.URL)
public class RackScanActionBean extends CoreActionBean {

    public static final String URL = "/view/rackScan.action";
    public static final String SHOW_SCANNING_JSP = "/vessel/rack_scan_summary.jsp";
    public static final String SCAN_RESULTS_JSP = "/vessel/rack_scan_results.jsp";
    public static final String SCAN_EVENT = "scan";
    public static final String SHOW_SCAN_SELECTION_EVENT = "showScanSelection";

    private static final SortedSet<RackScanner> RACK_SCANNERS = new TreeSet<>(RackScanner.BY_NAME);

    private RackScanner rackScanner;

    @Inject
    private RackScannerEjb rackScannerEjb;

    static {
        RACK_SCANNERS.addAll(Arrays.asList(RackScanner.values()));
    }

    protected LinkedHashMap<String,String> rackScan;

    @DefaultHandler
    @HandlesEvent(SHOW_SCAN_SELECTION_EVENT)
    public Resolution showScanSelection() throws Exception {
        return new ForwardResolution(SHOW_SCANNING_JSP);
    }

    @HandlesEvent(SCAN_EVENT)
    public Resolution scan() throws ScannerException {

        rackScan = rackScannerEjb.runRackScanner(rackScanner);

        return showScanResults();
    }

    private Resolution showScanResults() {
        return new ForwardResolution(SCAN_RESULTS_JSP);
    }

    public RackScanner getRackScanner() {
        return rackScanner;
    }

    public void setRackScanner(RackScanner rackScanner) {
        this.rackScanner = rackScanner;
    }

    public static SortedSet<RackScanner> getRackScanners() {
        return RACK_SCANNERS;
    }
}
