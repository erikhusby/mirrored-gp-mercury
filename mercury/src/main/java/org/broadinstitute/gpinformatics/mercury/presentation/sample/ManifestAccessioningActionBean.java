package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.OnwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
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
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@UrlBinding(ManifestAccessioningActionBean.ACTIONBEAN_URL_BINDING)
public class ManifestAccessioningActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/sample/accessioning.action";
    public static final String SELECTED_SESSION_ID = "selectedSessionId";
    private static Log logger = LogFactory.getLog(ManifestAccessioningActionBean.class);
    public static final String START_SESSION_PAGE = "/sample/start_session.jsp";

    public static final String REVIEW_UPLOAD_PAGE = "/sample/review_manifest_upload.jsp";
    public static final String REVIEW_COVID_UPLOAD_PAGE = "/sample/review_covid_manifest_upload.jsp";
    public static final String ACCESSION_SAMPLE_PAGE = "/sample/accession_sample.jsp";
    public static final String SCAN_SAMPLE_RESULTS_PAGE = "/sample/manifest_status_insert.jsp";
    public static final String PREVIEW_CLOSE_SESSION_PAGE = "/sample/preview_close_session.jsp";
    private static final String ASSOCIATE_RECEIPT_PAGE = "/sample/associate_receipt.jsp";

    public static final String START_A_SESSION_ACTION = "startASession";
    public static final String START_A_COVID_SESSION_ACTION = "startACovidSession";
    public static final String UPLOAD_MANIFEST_ACTION = "uploadManifest";
    public static final String UPLOAD_COVID_MANIFEST_ACTION = "uploadCovidManifest";
    public static final String LOAD_SESSION_ACTION = "loadSession";
    public static final String VIEW_UPLOAD_ACTION = "viewUpload";
    public static final String ACCEPT_UPLOAD_ACTION = "acceptUpload";
    public static final String EXIT_SESSION_ACTION = "exitSession";
    public static final String VIEW_ACCESSION_SCAN_ACTION = "viewAccessionScan";
    public static final String SCAN_ACCESSION_SOURCE_ACTION = "scanAccessionSource";
    public static final String PREVIEW_SESSION_CLOSE_ACTION = "previewSessionClose";
    public static final String CLOSE_SESSION_ACTION = "closeSession";
    public static final String BEGIN_ACCESSION_ACTION = "beginAccession";
    public static final String FIND_RECEIPT_ACTION = "findReceipt";
    public static final String ASSOCIATE_RECEIPT_ACTION = "associateReceipt";

    public static final String UNABLE_TO_ASSOCIATE_RECEIPT_ERROR = "Unable to associate receipt with manifest Session";


    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @Inject
    private UserBean userBean;

    @Inject
    private JiraService jiraService;

    @Inject
    private ProjectTokenInput projectTokenInput;

    private ManifestSession selectedSession;

    @Validate(required = true, on = {LOAD_SESSION_ACTION, ACCEPT_UPLOAD_ACTION,
            EXIT_SESSION_ACTION, SCAN_ACCESSION_SOURCE_ACTION, PREVIEW_SESSION_CLOSE_ACTION,
            CLOSE_SESSION_ACTION})
    private Long selectedSessionId;

    @Validate(required = true, on = UPLOAD_MANIFEST_ACTION)
    private FileBean manifestFile;

    @Validate(required = true, on = SCAN_ACCESSION_SOURCE_ACTION, label = "Source sample is required for accessioning")
    private String accessionSource;

    private String accessionTube;

    private List<ManifestSession> openSessions;
    private List<ManifestSession> closedSessions;

    private ManifestStatus statusValues;
    private String scanErrors;
    private String scanMessages;

    private String receiptKey;

    private String receiptSummary;
    private String receiptDescription;
    private ManifestSessionEjb.AccessioningProcessType accessioningProcessType=
            ManifestSessionEjb.AccessioningProcessType.CRSP;
    private String usesSampleKit = "false";

    @After(stages = LifecycleStage.BindingAndValidation, on = {"!" + START_A_SESSION_ACTION})
    public void init() {
        if (selectedSessionId != null) {
            selectedSession = manifestSessionDao.find(selectedSessionId);
            statusValues = selectedSession.generateSessionStatusForClose();

            String businessKey = "";
            if(!selectedSession.isCovidSession()) {
                businessKey = selectedSession.getResearchProject().getBusinessKey();
            }
            projectTokenInput.setup(businessKey);
            accessioningProcessType = selectedSession.getAccessioningProcessType();
        }
    }

    @HandlesEvent(LOAD_SESSION_ACTION)
    public Resolution loadSession() {
        OnwardResolution direction;
        switch (selectedSession.getStatus()) {
        case OPEN:
            direction = new ForwardResolution(getClass(), VIEW_UPLOAD_ACTION);
            break;
        case ACCESSIONING:
            direction = new ForwardResolution(getClass(), VIEW_ACCESSION_SCAN_ACTION);
            break;
        case COMPLETED:
            direction = new ForwardResolution(getClass(), START_A_SESSION_ACTION);
            break;
        default:
            addGlobalValidationError("Unable to determine what to do with this session");
            direction = new RedirectResolution(REVIEW_UPLOAD_PAGE);
        }
        direction.addParameter(SELECTED_SESSION_ID, selectedSession.getManifestSessionId());

        return direction;
    }

    @HandlesEvent(VIEW_UPLOAD_ACTION)
    public Resolution view() {
        addErrorMessages();

        ForwardResolution forwardResolution = new ForwardResolution(REVIEW_UPLOAD_PAGE);
        if(selectedSession.isCovidSession()) {
            forwardResolution = new ForwardResolution(REVIEW_COVID_UPLOAD_PAGE);
        }
        return forwardResolution;
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

    @HandlesEvent(START_A_COVID_SESSION_ACTION)
    public Resolution startACovidSession() {

        accessioningProcessType = ManifestSessionEjb.AccessioningProcessType.COVID;

        return startASession();
    }

    @DefaultHandler
    @HandlesEvent(START_A_SESSION_ACTION)
    public Resolution startASession() {
        final List<ManifestSession> tempOpenSessions = manifestSessionDao.findOpenSessions();
        this.openSessions = tempOpenSessions.stream().filter(manifestSession -> {
            if(accessioningProcessType == ManifestSessionEjb.AccessioningProcessType.COVID) {
                return manifestSession.getAccessioningProcessType() == ManifestSessionEjb.AccessioningProcessType.COVID;
            } else {
                return manifestSession.getAccessioningProcessType() != ManifestSessionEjb.AccessioningProcessType.COVID;
            }
        }).collect(Collectors.toList());

        return new ForwardResolution(START_SESSION_PAGE);
    }

    @HandlesEvent(UPLOAD_COVID_MANIFEST_ACTION)
    public Resolution uploadCovidManifest() {
        accessioningProcessType = ManifestSessionEjb.AccessioningProcessType.COVID;
        return uploadManifest();
    }

    @HandlesEvent(UPLOAD_MANIFEST_ACTION)
    public Resolution uploadManifest() {

        try {
            ResearchProject researchProject = null;
            String researchProjectBusinessKey = null;
            if(accessioningProcessType != ManifestSessionEjb.AccessioningProcessType.COVID) {
                researchProject = projectTokenInput.getTokenObject();

                if (researchProject == null) {
                    addGlobalValidationError("a Research Project is required to upload manifest");
                    return getContext().getSourcePageResolution();
                }
                researchProjectBusinessKey = researchProject.getBusinessKey();
            }

            selectedSession = manifestSessionEjb.uploadManifest(researchProjectBusinessKey,
                    manifestFile.getInputStream(), manifestFile.getFileName(),
                    accessioningProcessType, Boolean.parseBoolean(usesSampleKit));

        } catch (IOException | InformaticsServiceException e) {
            logger.error(e);
            addGlobalValidationError("Unable to upload the manifest file: {2}", e.getMessage());
            return getContext().getSourcePageResolution();
        }
        return new RedirectResolution(getClass(), VIEW_UPLOAD_ACTION).addParameter(
                SELECTED_SESSION_ID, selectedSession.getManifestSessionId());
    }

    @HandlesEvent(ACCEPT_UPLOAD_ACTION)
    public Resolution acceptUpload() {

        Resolution result = new ForwardResolution(getClass(), LOAD_SESSION_ACTION);

        try {
            manifestSessionEjb.acceptManifestUpload(selectedSession.getManifestSessionId());
        } catch (TubeTransferException | InformaticsServiceException e) {
            addGlobalValidationError(e.getMessage());
            logger.error(e);
            result = getContext().getSourcePageResolution();
        }

        return result;
    }

    @HandlesEvent(SCAN_ACCESSION_SOURCE_ACTION)
    public Resolution scanAccessionSource() {

        try {
            if(!selectedSession.isCovidSession()) {
                if (selectedSession.isFromSampleKit()) {
                    if (StringUtils.isBlank(accessionTube)) {
                        addGlobalValidationError("Source tube barcode is required for accessioning sample kits");
                        return getContext().getSourcePageResolution();
                    }
                    manifestSessionEjb.findAndValidateTargetSampleAndVessel(accessionSource, accessionTube);
                }
            }
            manifestSessionEjb.accessionScan(selectedSessionId, accessionSource, accessionTube);
            scanMessages = String.format("Sample %s scanned successfully", accessionSource);
        } catch (Exception e) {
            scanErrors = e.getMessage();
            logger.error(scanErrors);
        }
        statusValues = manifestSessionEjb.getSessionStatus(selectedSessionId);
        return new ForwardResolution(SCAN_SAMPLE_RESULTS_PAGE).addParameter(SELECTED_SESSION_ID, selectedSessionId);
    }

    @HandlesEvent(PREVIEW_SESSION_CLOSE_ACTION)
    public Resolution previewSessionClose() {
        return new ForwardResolution(PREVIEW_CLOSE_SESSION_PAGE);
    }

    @HandlesEvent(CLOSE_SESSION_ACTION)
    public Resolution closeSession() {

        if(!selectedSession.canSessionExcludeReceiptTicket()) {
            addGlobalValidationError("Unable to submit this session without a record of receipt");
            return new ForwardResolution(ACCESSION_SAMPLE_PAGE);
        }

        try {
            manifestSessionEjb.closeSession(selectedSessionId);
            addMessage("The session {0} has successfully been marked as completed", selectedSession.getSessionName());
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            logger.error(e);
            return getContext().getSourcePageResolution();
        }
        return new ForwardResolution(getClass(), LOAD_SESSION_ACTION);
    }

    @HandlesEvent(FIND_RECEIPT_ACTION)
    public Resolution findReceipt() {
        ForwardResolution resolution = new ForwardResolution(ASSOCIATE_RECEIPT_PAGE)
                            .addParameter(SELECTED_SESSION_ID, selectedSession.getManifestSessionId());

        try {
            String projectKeyFieldName = "project";
            JiraIssue receiptInfo = jiraService.getIssueInfo(receiptKey, projectKeyFieldName);
            String projectKey = ((Map<String, String>) receiptInfo.getFieldValue(projectKeyFieldName)).get("key");
            if (!projectKey.equals(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())) {
                scanErrors = String.format("Receipt Identifier must be a %s project.",
                        CreateFields.ProjectType.RECEIPT_PROJECT.getProjectName());
            } else {
                receiptSummary = receiptInfo.getSummary();
                resolution = new ForwardResolution(ASSOCIATE_RECEIPT_PAGE)
                        .addParameter(SELECTED_SESSION_ID, selectedSession.getManifestSessionId())
                        .addParameter("receiptSummary", receiptInfo.getSummary())
                        .addParameter("receiptDescription", receiptInfo.getDescription())
                        .addParameter("receiptKey", receiptKey);
            }
        } catch (WebApplicationException e) {
            scanErrors = String.format("Unable to access the specified record of receipt: %s", receiptKey);
            logger.error(scanErrors);
        } catch (Exception e) {
            scanErrors = e.getMessage();
            logger.error(scanErrors);
        }

        return resolution;
    }

    @HandlesEvent(ASSOCIATE_RECEIPT_ACTION)
    public Resolution associateReceipt() {
        try {
            manifestSessionEjb.updateReceiptInfo(selectedSession.getManifestSessionId(), receiptKey);

            addMessage("The accessioning session was successfully updated with the receipt record: " + receiptKey);
        } catch (Exception e) {
            logger.error(UNABLE_TO_ASSOCIATE_RECEIPT_ERROR, e);
            addGlobalValidationError(UNABLE_TO_ASSOCIATE_RECEIPT_ERROR);
        }

        return new ForwardResolution(ACCESSION_SAMPLE_PAGE);
    }

    public Long getSelectedSessionId() {
        return selectedSessionId;
    }

    public void setSelectedSessionId(Long selectedSessionId) {
        this.selectedSessionId = selectedSessionId;
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

    public ProjectTokenInput getProjectTokenInput() {
        return projectTokenInput;
    }

    public String getAccessionTube() {
        return accessionTube;
    }

    public void setAccessionTube(String accessionTube) {
        this.accessionTube = accessionTube;
    }

    public String getReceiptKey() {
        return receiptKey;
    }

    public void setReceiptKey(String receiptKey) {
        this.receiptKey = receiptKey;
    }

    public String getReceiptDescription() {
        return receiptDescription;
    }

    public String getReceiptSummary() {
        return receiptSummary;
    }

    public boolean isCovidProcess() {
        return accessioningProcessType == ManifestSessionEjb.AccessioningProcessType.COVID;
    }

    public ManifestSessionEjb.AccessioningProcessType getAccessioningProcessType() {
        return accessioningProcessType;
    }

    public String getUsesSampleKit() {
        return usesSampleKit;
    }

    public void setUsesSampleKit(String usesSampleKit) {
        this.usesSampleKit = usesSampleKit;
    }
}
