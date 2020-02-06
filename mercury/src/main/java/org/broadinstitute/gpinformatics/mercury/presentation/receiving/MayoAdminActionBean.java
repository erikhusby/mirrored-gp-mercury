package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
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
    private static final String VIEW_FILE_BTN = "viewFileBtn";
    private static final String ROTATE_KEY_BTN = "rotateKeyBtn";
    private static final String UPLOAD_CREDENTIAL_BTN = "uploadCredentialBtn";

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    private MessageCollection messageCollection = new MessageCollection();

    @Validate(required = true, on = {VIEW_FILE_BTN})
    private String filename;

    private boolean rotateAcknowledgement = false;
    private boolean uploadCredentialAcknowledgement = false;
    private List<String> bucketList = new ArrayList<>();
    private List<String> failedFilesList = new ArrayList<>();
    private FileBean credentialFile;

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
            addGlobalValidationError("The acknowledgement must be checked.");
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

    /**
     * Generates a cell grid for the contents of the specified manifest file.
     */
    @HandlesEvent(UPLOAD_CREDENTIAL_BTN)
    public Resolution uploadCredential() {
        if (uploadCredentialAcknowledgement) {
            if (credentialFile == null) {
                addGlobalValidationError("Upload file is missing.");
            } else {
                mayoManifestEjb.uploadCredential(this);
            }
        } else {
            addGlobalValidationError("The acknowledgement must be checked.");
        }
        addMessages(messageCollection);
        return new ForwardResolution(MANIFEST_ADMIN_PAGE);
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

    public Reader getCredentialFileReader() throws IOException {
        return credentialFile.getReader();
    }

    public FileBean getCredentialFile() {
        return credentialFile;
    }

    public void setCredentialFile(FileBean credentialFile) {
        this.credentialFile = credentialFile;
    }

    public boolean isUploadCredentialAcknowledgement() {
        return uploadCredentialAcknowledgement;
    }

    public void setUploadCredentialAcknowledgement(boolean uploadCredentialAcknowledgement) {
        this.uploadCredentialAcknowledgement = uploadCredentialAcknowledgement;
    }
}
