package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles Mayo Manifest Admin functionality.
 */
@UrlBinding(MayoAdminActionBean.ACTION_BEAN_URL)
public class MayoAdminActionBean extends CoreActionBean {
    public static final String ACTION_BEAN_URL = "/receiving/mayo_manifest_admin.action";
    private static final String MANIFEST_ADMIN_PAGE = "mayo_manifest_admin.jsp";
    private static final String TEST_ACCESS_BTN = "testAccessBtn";
    private static final String SHOW_BUCKETLIST_BTN = "showBucketListBtn";
    private static final String SHOW_FAILED_FILES_LIST_BTN = "showFailedFilesListBtn";
    private static final String PULL_ALL_BTN = "pullAllFilesBtn";
    private static final String PULL_ONE_BTN = "pullFileBtn";
    private static final String VIEW_FILE_BTN = "viewFileBtn";
    private static final String ROTATE_KEY_BTN = "rotateKeyBtn";

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    private MessageCollection messageCollection = new MessageCollection();
    private List<List<String>> manifestCellGrid = new ArrayList<>();

    @Validate(required = true, on = {VIEW_FILE_BTN, PULL_ONE_BTN})
    private String filename;

    private boolean rotateAcknowledgement;
    private List<String> bucketList = new ArrayList<>();
    private List<String> failedFilesList = new ArrayList<>();

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution adminPage() {
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
        if (rotateAcknowledgement) {
            mayoManifestEjb.rotateServiceAccountKey(this);
            addMessages(messageCollection);
        } else {
            addValidationError("rotateAcknowledgement", "Acknowledgement is required.");
        }
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
    }

    @HandlesEvent(SHOW_BUCKETLIST_BTN)
    public Resolution showBucketList() {
        final byte[] bytes = bucketList.stream().sorted().collect(Collectors.joining("\n")).getBytes();
        return (request, response) -> {
            response.setContentType("application/text");
            response.setContentLength(bytes.length);
            response.setHeader("Expires:", "0"); // eliminates browser caching
            response.setHeader("Content-Disposition", "attachment; filename=allFiles.txt");
            OutputStream outStream = response.getOutputStream();
            outStream.write(bytes);
            outStream.flush();
        };
    }

    @HandlesEvent(SHOW_FAILED_FILES_LIST_BTN)
    public Resolution showFailedFilesList() {
        mayoManifestEjb.getFailedFiles(this);
        final byte[] bytes = failedFilesList.stream().sorted().collect(Collectors.joining("\n")).getBytes();
        return (request, response) -> {
            response.setContentType("application/text");
            response.setContentLength(bytes.length);
            response.setHeader("Expires:", "0"); // eliminates browser caching
            response.setHeader("Content-Disposition", "attachment; filename=failedFiles.txt");
            OutputStream outStream = response.getOutputStream();
            outStream.write(bytes);
            outStream.flush();
        };
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

    public List<List<String>> getManifestCellGrid() {
        return manifestCellGrid;
    }

    public void setManifestCellGrid(List<List<String>> manifestCellGrid) {
        this.manifestCellGrid = manifestCellGrid;
    }

    public MessageCollection getMessageCollection() {
        return messageCollection;
    }

    public void setMessageCollection(MessageCollection messageCollection) {
        this.messageCollection = messageCollection;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isRotateAcknowledgement() {
        return rotateAcknowledgement;
    }

    public void setRotateAcknowledgement(boolean rotateAcknowledgement) {
        this.rotateAcknowledgement = rotateAcknowledgement;
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
}
