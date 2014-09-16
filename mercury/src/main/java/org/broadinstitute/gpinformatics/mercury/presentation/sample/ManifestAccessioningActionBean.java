package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("unused")
@UrlBinding(ManifestAccessioningActionBean.ACTIONBEAN_URL_BINDING)
public class ManifestAccessioningActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/samples/manifestAccessioning";
    private static Log logger = LogFactory.getLog(ManifestAccessioningActionBean.class);

    public static final String START_SESSION_PAGE = "/sample/start_session.jsp";
    public static final String REVIEW_UPLOAD_PAGE = "/sample/review_manifest_upload.jsp";
    public static final String ACCESSION_SAMPLE_PAGE = "/sample/accession_sample.jsp";

    public static final String START_A_SESSION_ACTION = "startASession";
    public static final String UPLOAD_MANIFEST_ACTION = "uploadManifest";
    public static final String LOAD_SESSION_ACTION = "loadSession";
    public static final String VIEW_UPLOAD_ACTION = "viewUpload";
    public static final String ACCEPT_UPLOAD_ACTION = "acceptUpload";
    public static final String EXIT_SESSION_ACTION = "exitSession";
    public static final String VIEW_ACCESSION_SCAN_ACTION = "viewAccessionScan";
    public static final String SCAN_ACCESSION_SOURCE_ACTION = "scanAccessionSource";
    public static final String PREVIEW_SESSION_ACTION = "previewSession";
    public static final String CLOSE_SESSION_ACTION = "closeSession";
    public static final String BEGIN_ACCESSION_ACTION = "beginAccession";

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @Inject
    private UserBean userBean;

    private ManifestSession selectedSession;

    @Validate(required = true, on = {LOAD_SESSION_ACTION, ACCEPT_UPLOAD_ACTION,
            EXIT_SESSION_ACTION, SCAN_ACCESSION_SOURCE_ACTION, PREVIEW_SESSION_ACTION, CLOSE_SESSION_ACTION})
    private Long selectedSessionId;

    @Validate(required = true, on = UPLOAD_MANIFEST_ACTION)
    private String researchProjectKey;

    @Validate(required = true, on = UPLOAD_MANIFEST_ACTION)
    private FileBean manifestFile;

    private List<ManifestSession> openSessions;
    private List<ManifestSession> closedSessions;

    public ManifestAccessioningActionBean() {
        super();
    }


    @Before(stages = LifecycleStage.BindingAndValidation, on = {LOAD_SESSION_ACTION, ACCEPT_UPLOAD_ACTION,
            EXIT_SESSION_ACTION, SCAN_ACCESSION_SOURCE_ACTION, PREVIEW_SESSION_ACTION, CLOSE_SESSION_ACTION})
    public void init() {
        selectedSession = manifestSessionDao.find(selectedSessionId);
    }

    @HandlesEvent(LOAD_SESSION_ACTION)
    public Resolution loadSession() {

        //TODO determine page based on session and record state. Putting upload review for now

        return new ForwardResolution(REVIEW_UPLOAD_PAGE);

    }

    @HandlesEvent(START_A_SESSION_ACTION)
    public Resolution startASession() {
        openSessions = manifestSessionDao.findOpenSessions();

        return new ForwardResolution(START_SESSION_PAGE);
    }

    @HandlesEvent(UPLOAD_MANIFEST_ACTION)
    public Resolution uploadManifest() {

        //TODO. DO not think we have to pass in bsp user.  Can probably just inject userbean in Manifest Session Ejb
        try {
            manifestSessionEjb.uploadManifest(researchProjectKey, manifestFile.getInputStream(),
                    manifestFile.getFileName(), userBean.getBspUser());

        } catch (IOException | InformaticsServiceException e) {
            addGlobalValidationError("Un able to upload the manifest file:  %s", e.getMessage());
            return new RedirectResolution(getClass(), BEGIN_ACCESSION_ACTION)
                    .addParameter("researchProjectKey", researchProjectKey);
        }

        return new ForwardResolution(getClass(), START_A_SESSION_ACTION);
    }


    public Long getSelectedSessionId() {
        return selectedSessionId;
    }

    public void setSelectedSessionId(Long selectedSessionId) {
        this.selectedSessionId = selectedSessionId;
    }

    public String getResearchProjectKey() {
        return researchProjectKey;
    }

    public void setResearchProjectKey(String researchProjectKey) {
        this.researchProjectKey = researchProjectKey;
    }

    public FileBean getManifestFile() {
        return manifestFile;
    }

    public void setManifestFile(FileBean manifestFile) {
        this.manifestFile = manifestFile;
    }

    public List<ManifestSession> getOpenSessions() {
        return openSessions;
    }

    public void setOpenSessions(List<ManifestSession> openSessions) {
        this.openSessions = openSessions;
    }

    public List<ManifestSession> getClosedSessions() {
        return closedSessions;
    }

    public void setClosedSessions(List<ManifestSession> closedSessions) {
        this.closedSessions = closedSessions;
    }
}
