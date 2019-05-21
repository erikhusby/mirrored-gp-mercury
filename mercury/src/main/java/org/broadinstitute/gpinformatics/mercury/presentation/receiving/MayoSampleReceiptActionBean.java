package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles receipt of a Mayo rack.
 * Also handles the Mayo Manifest Admin functionality.
 */
@UrlBinding(MayoSampleReceiptActionBean.ACTION_BEAN_URL)
public class MayoSampleReceiptActionBean extends RackScanActionBean {
    public static final String ACTION_BEAN_URL = "/receiving/mayo_sample_receipt.action";

    private static final String PAGE1 = "mayo_sample_receipt1.jsp";
    private static final String SCAN_BTN = "scanBtn";

    private static final String PAGE2 = "mayo_sample_receipt2.jsp";
    private static final String RECEIVE_BTN = "receiveBtn";
    private static final String ACCESSION_BTN = "accessionBtn";

    private static final String MANIFEST_ADMIN_PAGE = "mayo_manifest_admin.jsp";
    private static final String STORAGE_UTILITIES = "storageUtilities";
    private static final String TEST_ACCESS_BTN = "testAccessBtn";
    private static final String SHOW_BUCKETLIST_BTN = "showBucketListBtn";
    private static final String SHOW_FAILED_FILES_LIST_BTN = "showFailedFilesListBtn";
    private static final String PULL_ALL_BTN = "pullAllFilesBtn";
    private static final String PULL_ONE_BTN = "pullFileBtn";
    private static final String REACCESSION_BTN = "reaccessionBtn";
    private static final String VIEW_FILE_BTN = "viewFileBtn";
    private static final String ROTATE_KEY_BTN = "rotateKeyBtn";

    private MessageCollection messageCollection = new MessageCollection();
    private List<String> rackScanEntries = new ArrayList<>();
    private List<List<String>> manifestCellGrid = new ArrayList<>();
    private List<String> bucketList = new ArrayList<>();
    private List<String> failedFilesList = new ArrayList<>();
    private Long manifestSessionId;
    private String quarantineReason;
    private String rctUrl;

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    @Validate(required = true, on = {SCAN_BTN, RECEIVE_BTN, ACCESSION_BTN, REACCESSION_BTN})
    private String rackBarcode;

    @Validate(required = true, on = {VIEW_FILE_BTN, PULL_ONE_BTN})
    private String filename;

    @DefaultHandler
    public Resolution page1() {
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(SCAN_BTN)
    public Resolution scanEvent() throws ScannerException {
        // Performs the rack scan and validates it.
        runRackScan(false);
        if (rackScan == null || rackScan.isEmpty()) {
            messageCollection.addError("The rack scan is empty.");
            addMessages(messageCollection);
            return new ForwardResolution(PAGE1);
        } else {
            mayoManifestEjb.validateAndScan(this);
            addMessages(messageCollection);
            return new ForwardResolution(PAGE2);
        }
    }

    @HandlesEvent(RECEIVE_BTN)
    public Resolution receiveEvent() {
        reconstructScan();
        mayoManifestEjb.receive(this);
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    /**
     * Makes receipt artifacts for the rack and tubes and accessions
     * samples if a ManifestSession is found and the data matches up.
     */
    @HandlesEvent(ACCESSION_BTN)
    public Resolution accessionEvent() {
        reconstructScan();
        mayoManifestEjb.receive(this);
        if (!messageCollection.hasErrors()) {
            mayoManifestEjb.lookupManifestSession(this);
            mayoManifestEjb.accession(this);
        }
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    /** Access to the admin page. */
    @HandlesEvent(STORAGE_UTILITIES)
    public Resolution storageUtilities() {
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
    }

    /**
     * Displays connection and storage bucket access status in the process of
     * listing the storage bucket and reading a bucket file chosen at random.
     */
    @HandlesEvent(TEST_ACCESS_BTN)
    public Resolution testAccess() {
        mayoManifestEjb.testAccess(this);
        addMessages(messageCollection);
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
    }

    @HandlesEvent(ROTATE_KEY_BTN)
    public Resolution rotateServiceAccountKey() {
        mayoManifestEjb.rotateServiceAccountKey(this);
        addMessages(messageCollection);
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
    }

    @HandlesEvent(SHOW_BUCKETLIST_BTN)
    public Resolution showBucketList() {
        final byte[] bytes = bucketList.stream().sorted().collect(Collectors.joining("\n")).getBytes();
        Resolution resolution = (request, response) -> {
            response.setContentType("application/text");
            response.setContentLength(bytes.length);
            response.setHeader("Expires:", "0"); // eliminates browser caching
            response.setHeader("Content-Disposition", "attachment; filename=allFiles.txt");
            OutputStream outStream = response.getOutputStream();
            outStream.write(bytes);
            outStream.flush();
        };
        return resolution;
    }

    @HandlesEvent(SHOW_FAILED_FILES_LIST_BTN)
    public Resolution showFailedFilesList() {
        mayoManifestEjb.getFailedFiles(this);
        final byte[] bytes = failedFilesList.stream().sorted().collect(Collectors.joining("\n")).getBytes();
        Resolution resolution = (request, response) -> {
            response.setContentType("application/text");
            response.setContentLength(bytes.length);
            response.setHeader("Expires:", "0"); // eliminates browser caching
            response.setHeader("Content-Disposition", "attachment; filename=failedFiles.txt");
            OutputStream outStream = response.getOutputStream();
            outStream.write(bytes);
            outStream.flush();
        };
        return resolution;
    }

    /**
     * Processes all new files found in the storage bucket.
     */
    @HandlesEvent(PULL_ALL_BTN)
    public Resolution pullAll() {
        mayoManifestEjb.pullAll(this);
        addMessages(messageCollection);
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
    }

    /**
     * Re-reads and re-processes one file, for the purpose of updating
     * (i.e. creating a new) manifest session for the file.
     */
    @HandlesEvent(PULL_ONE_BTN)
    public Resolution pullOne() {
        mayoManifestEjb.pullOne(this);
        addMessages(messageCollection);
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
    }

    /**
     * Re-accessions the rack and its tubes using the most recent manifest.
     * Any tube or position changes must be fixed up before running this because
     * the tubes used are the ones that Mercury has in the specified rack.
     */
    @HandlesEvent(REACCESSION_BTN)
    public Resolution reaccession() {
        mayoManifestEjb.lookupManifestSession(this);
        mayoManifestEjb.accession(this);
        addMessages(messageCollection);
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
    }

    /**
     * Generates a cell grid for the contents of the specified manifest file.
     */
    @HandlesEvent(VIEW_FILE_BTN)
    public Resolution viewFile() {
        mayoManifestEjb.readManifestFileCellGrid(this);
        if (manifestCellGrid.isEmpty()) {
            messageCollection.addError("Cannot find %s in manifest file storage.", filename);
        }
        addMessages(messageCollection);
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return "Mayo Sample Receipt";
    }

    /** Reconstructs the rackScan from the jsp's hidden variables. */
    private void reconstructScan() {
        rackScan = new LinkedHashMap<>();
        for (String rackScanEntry : rackScanEntries) {
            String[] tokens = rackScanEntry.split(" ");
            String value = (tokens.length > 1) ? tokens[1] : "";
            rackScan.put(tokens[0], value);
        }
    }

    /** Return an ordered list of the rack column names. */
    public List<String> getRackColumns() {
        return Arrays.asList(MayoManifestEjb.DEFAULT_RACK_TYPE.getVesselGeometry().getColumnNames());
	}

    /** Return an ordered list of the rack row names. */
	public List<String> getRackRows() {
        return Arrays.asList(MayoManifestEjb.DEFAULT_RACK_TYPE.getVesselGeometry().getRowNames());
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

    public List<List<String>> getManifestCellGrid() {
        return manifestCellGrid;
    }

    public void setManifestCellGrid(List<List<String>> manifestCellGrid) {
        this.manifestCellGrid = manifestCellGrid;
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

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<String> getBucketList() {
        return bucketList;
    }

    public void setBucketList(List<String> bucketList) {
        this.bucketList = bucketList;
    }

    public List<String> getFailedFilesList() {
        return failedFilesList;
    }

    public void setFailedFilesList(List<String> failedFilesList) {
        this.failedFilesList = failedFilesList;
    }

    public Long getManifestSessionId() {
        return manifestSessionId;
    }

    public void setManifestSessionId(Long manifestSessionId) {
        this.manifestSessionId = manifestSessionId;
    }

    public String getQuarantineReason() {
        return quarantineReason;
    }

    public void setQuarantineReason(String quarantineReason) {
        this.quarantineReason = quarantineReason;
    }

    public String getRctUrl() {
        return rctUrl;
    }

    public void setRctUrl(String rctUrl) {
        this.rctUrl = rctUrl;
    }

}
