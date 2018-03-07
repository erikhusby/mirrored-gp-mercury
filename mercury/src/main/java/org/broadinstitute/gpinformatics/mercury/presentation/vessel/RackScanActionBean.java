package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.ValidationError;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.RackScanner;
import org.broadinstitute.bsp.client.rackscan.RackScannerType;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.rackscan.geometry.Dimension;
import org.broadinstitute.bsp.client.rackscan.geometry.Geometry;
import org.broadinstitute.bsp.client.rackscan.geometry.index.AlphaNumeric;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.RackScannerEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    protected static final Log log = LogFactory.getLog(CoreActionBean.class);

    public static final String SHOW_SCANNER_SELECTION_JSP = "/vessel/rack_scanner_list.jsp";
    public static final String SCAN_RESULTS_JSP = "/vessel/rack_scan_results.jsp";
    public static final String SHOW_LAB_SELECTION_JSP = "/vessel/rack_scan_lab_select.jsp";
    public static final String SCAN_EVENT = "scan";
    public static final String SHOW_SCAN_SELECTION_EVENT = "showScanSelection";
    public static final String SHOW_LAB_SELECTION_EVENT = "showLabSelection";

    public RackScanActionBean() {
    }

    public RackScanActionBean(String createTitle, String editTitle, String editBusinessKeyName) {
        super(createTitle, editTitle, editBusinessKeyName);
    }

    /**
     * Loads the rack scanners based upon the lab to filter by.
     */
    @After(stages = LifecycleStage.BindingAndValidation)
    public void loadRackScanners() {
        rackScanners.clear();
        rackScanners.addAll(RackScanner.getRackScannersByLab(labToFilterBy, deployment != Deployment.PROD));
    }

    /** Returns the rack scanner devices formatted into html dropdown option elements. */
    @HandlesEvent("getScannersForLab")
    public Resolution getScannerOptionTags() {
        StringBuilder optionTags = new StringBuilder("<option value=''>Select One</option>");
        for (RackScanner rs : rackScanners) {
            optionTags.append("<option value='").append(rs.getName()).append("'>").
                    append(rs.getScannerName()).append("</option>");
        }
        return new StreamingResolution("application/html", optionTags.toString());
    }

    private SortedSet<RackScanner> rackScanners = new TreeSet<>(RackScanner.BY_NAME);

    /** FileBean of the uploaded user-defined csv file used for rack scan simulation. */
    private FileBean simulatedScanCsv;

    @Inject
    protected RackScannerEjb rackScannerEjb;

    @Inject
    protected Deployment deployment;


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
        return new ForwardResolution(SHOW_SCANNER_SELECTION_JSP);
    }

    /** Does a rack scan and sets the rackScan variable, rack barcode is excluded. Returns a default results jsp. */
    @HandlesEvent(SCAN_EVENT)
    public Resolution scan() throws ScannerException {
        runRackScan( false );
        return new ForwardResolution(SCAN_RESULTS_JSP);
    }

    /**
     * Allow a sub-class to override scan logic to optionally include the rack barcode in the rackScan variable.
     * */
    protected void runRackScan( boolean includeRackBarcode ) throws ScannerException {
        Reader reader = null;
        try {
            if (simulatedScanCsv == null) {
                if (rackScanner.getRackScannerLab() == RackScanner.RackScannerLab.LOCALHOST) {
                    String ipAddress = getContext().getRequest().getHeader("X-FORWARDED-FOR");
                    if (ipAddress == null) {
                        ipAddress = getContext().getRequest().getRemoteAddr();
                    }
                    ipAddress = ipAddress.replaceAll(":", ".");
                    rackScanner.getRackScannerConfig().setIpAddress(ipAddress);
                }
                rackScan = rackScannerEjb.runRackScanner(rackScanner, null, includeRackBarcode);
            } else {
                reader = simulatedScanCsv.getReader();
                rackScan = rackScannerEjb.runRackScanner(rackScanner, reader, includeRackBarcode);
            }
        } catch (Exception e) {
            log.error(e);
            addGlobalValidationError("Error connecting to the rack scanner. " + e.getMessage());

            rackScan = new LinkedHashMap<>();
            for (String position : getMatrixPositions()) {
                rackScan.put(position, "");
            }
        } finally {
            IOUtils.closeQuietly(reader);
            if (simulatedScanCsv != null) {
                try {
                    simulatedScanCsv.delete();
                } catch (IOException e) {
                    log.error("Fail deleting tmp file.", e);
                }
            }
        }
    }

    /** Shows the possible labs rack scanners are in. */
    public List<RackScanner.RackScannerLab> getAllLabs() {

        List<RackScanner.RackScannerLab> labsBySoftwareSystems;
        if (deployment != Deployment.PROD) {
            labsBySoftwareSystems = RackScanner.RackScannerLab.getLabsBySoftwareSystems(RackScanner.SoftwareSystem.BSP,
                    RackScanner.SoftwareSystem.MERCURY_NON_PROD, RackScanner.SoftwareSystem.MERCURY);
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
        if( this.labToFilterBy == RackScanner.RackScannerLab.RACK_SCAN_SIMULATOR_LAB ){
            setRackScanner(RackScanner.RACK_SCAN_SIMULATOR);
        }
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

    public FileBean getSimulatedScanCsv() {
        return simulatedScanCsv;
    }

    public void setSimulatedScanCsv(FileBean simulatedScanCsv) {
        this.simulatedScanCsv = simulatedScanCsv;
    }
}
