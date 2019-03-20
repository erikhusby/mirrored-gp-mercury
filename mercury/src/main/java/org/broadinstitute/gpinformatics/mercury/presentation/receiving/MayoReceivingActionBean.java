package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestImportProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Handles receipt of Mayo samples. There are two receivables from Mayo - a manifest file that describes the
 * samples, and a rack or box of tubes. Mayo puts the manifest file in a Google storage bucket where it can be
 * read by Mercury. At a different time the rack of tubes arrives and a lab tech scans its barcodes in using
 * the Mercury UI.
 *
 * This class queries the lab tech for the package barcode and rack barcode, takes a rack scan of tube barcodes,
 * then looks up the manifest file using the package barcode. Mercury makes vessels, samples, a manifest session,
 * and an RCT jira ticket. If there is no manifest found the vessels and RCT ticket are still made, but not the
 * samples since they require data from the manifest. The lab tech can redo the rack scan after the manifest is
 * present in Google bucket, and this class will then create samples and manifest session and update the tubes.
 *
 * For user friendly do-overs, a rack can be rescanned provided the Overwrite checkbox is selected, and this
 * will update the existing rack to match the new scan. When this is done the manifest is re-read from Google
 * storage, and if found to have different sample metadata content, a new manifest session is created, and the
 * old sample metadata is replaced with the new. The existing RCT ticket gets updated too.
 */
@UrlBinding(MayoReceivingActionBean.ACTION_BEAN_URL)
public class MayoReceivingActionBean extends RackScanActionBean {
    private static final Log logger = LogFactory.getLog(MayoReceivingActionBean.class);
    public static final String ACTION_BEAN_URL = "/receiving/mayo_receiving.action";
    public static final String PAGE1 = "mayo_sample_receipt1.jsp";
    public static final String PAGE2 = "mayo_sample_receipt2.jsp";
    public static final String PAGE3 = "mayo_manifest_display.jsp";
    public static final String SHOW_MANIFEST_EVENT = "showManifestBtn";
    public static final String SCAN_EVENT = "scanBtn";
    public static final String SAVE_EVENT = "saveBtn";

    private MessageCollection messageCollection = new MessageCollection();
    private boolean previousMayoRack;
    private boolean overwriteFlag;
    private String filename;
    private VesselGeometry vesselGeometry = null;
    private List<String> rackScanEntries = new ArrayList<>();
    private List<String> rackScanBarcodes = new ArrayList<>();
    private String[][] manifestArray;

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    @Validate(required = true, on = {SCAN_EVENT, SHOW_MANIFEST_EVENT, SAVE_EVENT})
    private String packageBarcode;

    @Validate(required = true, on = {SCAN_EVENT, SAVE_EVENT})
    private String rackBarcode;

    @DefaultHandler
    public Resolution page1() {
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(SCAN_EVENT)
    public Resolution scanEvent() throws ScannerException {
        // Performs the rack scan.
        runRackScan(false);
        if (rackScan == null || rackScan.isEmpty()) {
            messageCollection.addError("The rack scan is empty.");
        } else {
            // Parses the rack scan into tubes and positions. Looks up the manifest file.
            mayoManifestEjb.processScan(this);
        }
        addMessages(messageCollection);
        return new ForwardResolution(messageCollection.hasErrors() ? PAGE1 : PAGE2);
    }

    /**
     * Retrieves and displays the manifest cell data from Google Storage file.
     */
    @HandlesEvent(SHOW_MANIFEST_EVENT)
    public Resolution showManifest() {
        List<List<String>> manifestCellGrid = mayoManifestEjb.readManifestAsCellGrid(this);
        if (!manifestCellGrid.isEmpty() && !hasErrors()) {
            // Formats the manifest upload spreadsheet cells into an array of strings indexed by
            // row and column for display in the jsp.
            int maxColumnCount = manifestCellGrid.stream().mapToInt(List::size).max().orElse(0);
            manifestArray = new String[manifestCellGrid.size()][maxColumnCount];
            MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
            processor.initHeaders(manifestCellGrid.get(0), filename, null);
            for (int rowIdx = 0; rowIdx < manifestCellGrid.size(); ++rowIdx) {
                List<String> columns = manifestCellGrid.get(rowIdx);
                for (int columnIdx = 0; columnIdx < columns.size(); ++columnIdx) {
                    String value = columns.get(columnIdx);
                    // Calculates the date string when the column is a parsable number that represents a date.
                    if (rowIdx > 0 && processor.isDateColumn(columnIdx) && NumberUtils.isParsable(value)) {
                        value = PoiSpreadsheetParser.convertDoubleStringToDateString(value);
                    }
                    manifestArray[rowIdx][columnIdx] = value;
                }
            }
        } else {
            messageCollection.addError("Cannot find %s in Google Storage.", filename);
        }
        addMessages(messageCollection);
        return new ForwardResolution(hasErrors() ? PAGE1 : PAGE3);
    }

    /**
     * Makes samples and tubes for the rack whose barcode has been scanned. If there is also a
     * ManifestSession for the scanned package barcode, finds the correct ManifestRecords for this
     * rack and links Metadata to the samples and tubes.
     */
    @HandlesEvent(SAVE_EVENT)
    public Resolution saveEvent() {
        // Reconstructs the rackScan from the jsp's mapEntries.
        rackScan = new LinkedHashMap<>();
        for (String rackScanEntry : rackScanEntries) {
            String[] tokens = rackScanEntry.split(" ");
            String value = (tokens.length > 1) ? tokens[1] : "";
            rackScan.put(tokens[0], value);
            if (StringUtils.isNotBlank(value)) {
                rackScanBarcodes.add(value);
            }
        }
        mayoManifestEjb.saveScan(this);
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return "Mayo Sample Receipt";
    }

    public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }

    /** Return an ordered list of the rack column names. */
    public List<String> getRackColumns() {
        return vesselGeometry != null ? Arrays.asList(vesselGeometry.getColumnNames()) : Collections.emptyList();
	}

    /** Return an ordered list of the rack row names. */
	public List<String> getRackRows() {
        return vesselGeometry != null ? Arrays.asList(vesselGeometry.getRowNames()) : Collections.emptyList();
	}

    /** Returns the SampleName or empty string for the given position name. */
	public String getSampleAt(String rowName, String columnName) {
        return StringUtils.trimToEmpty(rackScan.get(rowName + columnName));
    }

    public List<String> getRackScanEntries() {
        return rackScanEntries;
    }

    public void setRackScanEntries(List<String> rackScanEntries) {
        this.rackScanEntries = rackScanEntries;
    }

    public MessageCollection getMessageCollection() {
        return messageCollection;
    }

    public void setMessageCollection(MessageCollection messageCollection) {
        this.messageCollection = messageCollection;
    }

    public boolean isOverwriteFlag() {
        return overwriteFlag;
    }

    public void setOverwriteFlag(boolean overwriteFlag) {
        this.overwriteFlag = overwriteFlag;
    }

    public void setVesselGeometry(VesselGeometry vesselGeometry) {
        this.vesselGeometry = vesselGeometry;
    }

    public String[][] getManifestArray() {
        return manifestArray;
    }

    public String getPackageBarcode() {
        return packageBarcode;
    }

    public void setPackageBarcode(String packageBarcode) {
        this.packageBarcode = packageBarcode;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public void setRackBarcode(String rackBarcode) {
        this.rackBarcode = rackBarcode;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isPreviousMayoRack() {
        return previousMayoRack;
    }

    public void setPreviousMayoRack(boolean previousMayoRack) {
        this.previousMayoRack = previousMayoRack;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<String> getRackScanBarcodes() {
        return rackScanBarcodes;
    }

    public void setMayoManifestEjb(MayoManifestEjb mayoManifestEjb) {
        this.mayoManifestEjb = mayoManifestEjb;
    }
}
