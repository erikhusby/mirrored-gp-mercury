package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
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
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.broadinstitute.gpinformatics.mercury.entity.sample.TubeTransferException;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("unused")
@UrlBinding(ManifestAccessioningActionBean.ACTIONBEAN_URL_BINDING)
public class ManifestAccessioningActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/sample/accessioning.action";
    public static final String SELECTED_SESSION_ID = "selectedSessionId";
    private static Log logger = LogFactory.getLog(ManifestAccessioningActionBean.class);

    public static final String START_SESSION_PAGE = "/sample/start_session.jsp";
    public static final String REVIEW_UPLOAD_PAGE = "/sample/review_manifest_upload.jsp";
    public static final String ACCESSION_SAMPLE_PAGE = "/sample/accession_sample.jsp";
    public static final String SCAN_SAMPLE_RESULTS_PAGE = "/sample/manifest_status_insert.jsp";

    public static final String START_A_SESSION_ACTION = "startASession";
    public static final String UPLOAD_MANIFEST_ACTION = "uploadManifest";
    public static final String LOAD_SESSION_ACTION = "loadSession";
    public static final String VIEW_UPLOAD_ACTION = "viewUpload";
    public static final String ACCEPT_UPLOAD_ACTION = "acceptUpload";
    public static final String EXIT_SESSION_ACTION = "exitSession";
    public static final String VIEW_ACCESSION_SCAN_ACTION = "viewAccessionScan";
    public static final String SCAN_ACCESSION_SOURCE_ACTION = "scanAccessionSource";
    public static final String PREVIEW_SESSION_CLOSE_ACTION = "previewSessionClose";
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
            EXIT_SESSION_ACTION, SCAN_ACCESSION_SOURCE_ACTION, PREVIEW_SESSION_CLOSE_ACTION, CLOSE_SESSION_ACTION})
    private Long selectedSessionId;

    @Validate(required = true, on = UPLOAD_MANIFEST_ACTION)
    private String researchProjectKey;

    @Validate(required = true, on = UPLOAD_MANIFEST_ACTION)
    private FileBean manifestFile;

    @Validate(required = true, on = SCAN_ACCESSION_SOURCE_ACTION, label = "Source sample is required for accessioning")
    private String accessionSource;

    private List<ManifestSession> openSessions;
    private List<ManifestSession> closedSessions;

    private ManifestStatus statusValues;
    private String scanErrors;
    private String scanMessages;
    private List<ManifestEvent> manifestErrors;

    public ManifestAccessioningActionBean() {
        super();
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"!" + START_A_SESSION_ACTION})
    public void init() {
        if (selectedSessionId != null) {
            selectedSession = manifestSessionDao.find(selectedSessionId);
            statusValues = manifestSessionEjb.getSessionStatus(selectedSessionId);
        }
    }

    @HandlesEvent(LOAD_SESSION_ACTION)
    public Resolution loadSession() {
        RedirectResolution direction;
        switch (selectedSession.getStatus()) {
        case OPEN:
            direction = new RedirectResolution(REVIEW_UPLOAD_PAGE);
        break;
        case ACCESSIONING:
            direction = new RedirectResolution(ACCESSION_SAMPLE_PAGE);
        break;
        case COMPLETED:
            // TODO this needs to be changed to the appropriate PAGE
            direction = new RedirectResolution(VIEW_UPLOAD_ACTION);
        break;
        default:
            addGlobalValidationError("Unable to determine the what to do with this session");
            direction = new RedirectResolution(VIEW_UPLOAD_ACTION);
        }
        direction.addParameter(SELECTED_SESSION_ID, selectedSession.getManifestSessionId());

        return direction;
    }

    @HandlesEvent(VIEW_UPLOAD_ACTION)
    public Resolution view() {
        addErrorMessages();

        return new ForwardResolution(REVIEW_UPLOAD_PAGE);
    }

    @HandlesEvent(VIEW_ACCESSION_SCAN_ACTION)
    public Resolution viewAccessionScan() {
        return new ForwardResolution(ACCESSION_SAMPLE_PAGE);
    }

    private void addErrorMessages() {
        for (ManifestEvent event : selectedSession.getManifestEvents()) {
            addGlobalValidationError(event.getMessage());
        }
    }

    @DefaultHandler
    @HandlesEvent(START_A_SESSION_ACTION)
    public Resolution startASession() {
        openSessions = manifestSessionDao.findOpenSessions();

        return new ForwardResolution(START_SESSION_PAGE);
    }

    @HandlesEvent(UPLOAD_MANIFEST_ACTION)
    public Resolution uploadManifest() {

        try {
            selectedSession =
                    manifestSessionEjb.uploadManifest(researchProjectKey, manifestFile.getInputStream(),
                            manifestFile.getFileName(), userBean.getBspUser());

        } catch (IOException | InformaticsServiceException e) {
            addGlobalValidationError("Unable to upload the manifest file: {2}", e.getMessage());
            return getContext().getSourcePageResolution();
        }
        addErrorMessages();
        return new RedirectResolution(getClass(), LOAD_SESSION_ACTION).addParameter(
                SELECTED_SESSION_ID, selectedSession.getManifestSessionId());
    }

    @HandlesEvent(ACCEPT_UPLOAD_ACTION)
    public Resolution acceptUpload() {

        Resolution result =  new ForwardResolution(getClass(), LOAD_SESSION_ACTION);

        try {
            manifestSessionEjb.acceptManifestUpload(selectedSession.getManifestSessionId());
        } catch (TubeTransferException | InformaticsServiceException e) {
            addGlobalValidationError(e.getMessage());
            result = getContext().getSourcePageResolution();
        }

        return result;
    }

    @HandlesEvent(SCAN_ACCESSION_SOURCE_ACTION)
    public Resolution scanAccessionSource() {

        try {
            manifestSessionEjb.accessionScan(selectedSessionId, accessionSource);
            scanMessages = String.format("Sample %s scanned successfully", accessionSource);
        } catch (Exception e) {
            scanErrors = e.getMessage();
        }
        statusValues = manifestSessionEjb.getSessionStatus(selectedSessionId);
        return new ForwardResolution(SCAN_SAMPLE_RESULTS_PAGE).addParameter(SELECTED_SESSION_ID, selectedSessionId);
    }

    /**
     * TODO  Perhaps make a template page for this and have the ajax return the HTML
     *
     * Use ResearchProjectActionBean.queryRegulatoryInfoReturnHtmlSnippet as a guide
     * lines 115-130 on projects/view.jsp is the AJAX call
     *
     */
    @HandlesEvent(PREVIEW_SESSION_CLOSE_ACTION)
    public Resolution previewSessionClose() {
        statusValues = manifestSessionEjb.getSessionStatus(selectedSessionId);

        return null;
    }

    @HandlesEvent(CLOSE_SESSION_ACTION)
    public Resolution closeSession() {

        Resolution result = new ForwardResolution(getClass(), LOAD_SESSION_ACTION);

        try {
            manifestSessionEjb.closeSession(selectedSessionId);
            addMessage("The session {0} has successfully been marked as completed", selectedSession.getSessionName());
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            result = getContext().getSourcePageResolution();
        }
        return result;
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

    public ManifestSession getSelectedSession() {
        return selectedSession;
    }

    public String getAccessionSource() {
        return accessionSource;
    }

    public void setAccessionSource(String accessionSource) {
        this.accessionSource = accessionSource;
    }

    public ManifestStatus getStatusValues() {
        return statusValues;
    }

    public void setStatusValues(ManifestStatus statusValues) {
        this.statusValues = statusValues;
    }

    public String getScanErrors() {
        return scanErrors;
    }

    public String getScanMessages() {
        return scanMessages;
    }

    public List<ManifestEvent> getManifestErrors() {
        return manifestErrors;
    }
}
