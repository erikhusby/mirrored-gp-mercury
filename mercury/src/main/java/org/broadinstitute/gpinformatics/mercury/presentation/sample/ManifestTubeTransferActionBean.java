package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import java.util.List;

@SuppressWarnings("unused")
@UrlBinding(ManifestTubeTransferActionBean.ACTIONBEAN_URL_BINDING)
public class ManifestTubeTransferActionBean extends CoreActionBean {

    public static final String ACTIONBEAN_URL_BINDING = "/sample/manifestTubeTransfer.action";
    private static Log logger = LogFactory.getLog(ManifestTubeTransferActionBean.class);

    // Helping constants
    private static final String STEP_1_SOURCE_SAMPLE_IS_REQUIRED = "Step 1 (Source Sample) is required";
    private static final String STEP_2_SAMPLE_KEY_IS_REQUIRED = "Step 2 (Sample Key) is required";
    private static final String STEP_3_LAB_VESSEL_IS_REQUIRED = "Step 3 (Lab vessel) is required";
    private static final String SELECT_A_SESSION = "You must select a session to continue";

    // Page Definition(s)
    public static final String TUBE_TRANSFER_PAGE = "/sample/manifest_tube_transfer.jsp";

    // Actions
    public static final String SCAN_SOURCE_TUBE_ACTION = "scanSource";
    public static final String SCAN_TARGET_SAMPLE_ACTION = "scanTargetSample";
    public static final String SCAN_TARGET_VESSEL_AND_SAMPLE_ACTION = "scanTargetVessel";
    public static final String RECORD_TRANSFER_ACTION = "recordTransfer";

    private String sourceTube;
    private String targetSample;
    private String targetVessel;

    @Validate(required = true, on = {RECORD_TRANSFER_ACTION}, label = "Session")
    private String activeSessionId;
    private ManifestSession activeSession;
    private List<ManifestSession> availableSessions;

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @Inject
    private UserBean userBean;

    public ManifestTubeTransferActionBean() {
        super();
    }

    /**
     * Define validations to be used for page submissions.  Grouped them all here to allow user to see mixture of
     * requirement validations (for empty fields) and validation errors (for invalid input).
     */
    @ValidationMethod(on = RECORD_TRANSFER_ACTION)
    public void validateInput() {

        if (StringUtils.isBlank(sourceTube)) {
            addGlobalValidationError(STEP_1_SOURCE_SAMPLE_IS_REQUIRED);
        } else {
            String message = validateSource();
            if (StringUtils.isNotBlank(message)) {
                addGlobalValidationError(message);
            }
        }

        if (StringUtils.isBlank(targetSample)) {
            addGlobalValidationError(STEP_2_SAMPLE_KEY_IS_REQUIRED);
        } else {
            String message = validateTargetSample();
            if (StringUtils.isNotBlank(message)) {
                addGlobalValidationError(message);
            }
        }
        if (StringUtils.isBlank(targetVessel)) {
            addGlobalValidationError(STEP_3_LAB_VESSEL_IS_REQUIRED);
        } else {
            if (StringUtils.isNotBlank(targetSample)) {
                String message = validateTargetVessel();
                if (StringUtils.isNotBlank(message)) {
                    addGlobalValidationError(message);
                }
            }
        }
    }

    /**
     * initialize required values to process submission
     */
    @Before(stages = LifecycleStage.EventHandling, on = {RECORD_TRANSFER_ACTION})
    public void init() {
        findActiveSession();
    }

    /**
     * Find the session based on the selected session
     */
    private void findActiveSession() {
        activeSession = manifestSessionDao.find(Long.valueOf(activeSessionId));
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, RECORD_TRANSFER_ACTION})
    public void initSessionChoices() {
        availableSessions = manifestSessionDao.findSessionsEligibleForTubeTransfer();
    }

    /**
     * handles viewing the page
     */
    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(TUBE_TRANSFER_PAGE);
    }

    /**
     * Handles recording the actual transfer from the collaborator source tube to mercury vessel
     */
    @HandlesEvent(RECORD_TRANSFER_ACTION)
    public Resolution recordTransfer() {

        Resolution resolution;

        try {
            manifestSessionEjb.transferSample(Long.valueOf(activeSessionId), sourceTube, targetSample, targetVessel);
            addMessage("Sample ID {0} has been successfully recorded as transferred to vessel " +
                       "{1} with a sample of {2}", sourceTube, targetVessel, targetSample);
            resolution =
                    new RedirectResolution(getClass(), VIEW_ACTION).addParameter("activeSessionId", activeSessionId);
        } catch (Exception e) {
            addGlobalValidationError(String.format(
                    "Could not record a transfer of sample %s to vessel %s with sample %s",
                    sourceTube, targetVessel, targetSample), e);
            logger.error(String.format(
                    "Could not record a transfer of sample %s to vessel %s with sample %s",
                    sourceTube, targetVessel, targetSample), e);
            resolution = getContext().getSourcePageResolution();
        }
        return resolution;
    }

    /**
     * Handles the AJAX call to validate the collaborator source tube.
     *
     * @return
     */
    @HandlesEvent(SCAN_SOURCE_TUBE_ACTION)
    public Resolution scanSource() {
        if (activeSessionId == null) {
            return createTextResolution(SELECT_A_SESSION);
        }
        if (StringUtils.isBlank(sourceTube)) {
            return createTextResolution(STEP_1_SOURCE_SAMPLE_IS_REQUIRED);
        }

        findActiveSession();

        if (activeSession == null) {
            return createTextResolution("The selected session could not be found");
        }
        String message = validateSource();

        return createTextResolution(message);
    }

    /**
     * Handles the AJAX call to validate the value input for Mercury sample
     *
     * @return
     */
    @HandlesEvent(SCAN_TARGET_SAMPLE_ACTION)
    public Resolution scanTargetSample() {

        if (StringUtils.isBlank(targetSample)) {
            return createTextResolution(STEP_2_SAMPLE_KEY_IS_REQUIRED);
        }

        String message = validateTargetSample();

        return createTextResolution(message);
    }

    /**
     * Handles the AJAX call to validate the Lab Vessel selected as well as the Mercury Sample
     *
     * @return
     */
    @HandlesEvent(SCAN_TARGET_VESSEL_AND_SAMPLE_ACTION)
    public Resolution scanTargetVessel() {

        if (StringUtils.isBlank(targetSample)) {
            return createTextResolution(STEP_2_SAMPLE_KEY_IS_REQUIRED);
        }

        if (StringUtils.isBlank(targetVessel)) {
            return createTextResolution(STEP_3_LAB_VESSEL_IS_REQUIRED);
        }

        String message = validateTargetVessel();
        return createTextResolution(message);
    }

    /**
     * Validates source tube
     *
     * @return any error messages if an exception occurs
     */
    private String validateSource() {
        String message = "";
        try {
            manifestSessionEjb.validateSourceTubeForTransfer(Long.valueOf(activeSessionId), sourceTube);
        } catch (Exception e) {
            message = e.getMessage();
            logger.error("Could not validate source " + sourceTube, e);
        }
        return message;
    }

    /**
     * Validates target sample
     * @return any error messages if an exception occurs
     */
    private String validateTargetSample() {

        String message = "";
        try {
            findActiveSession();
            if(!activeSession.isCovidSession()) {
                manifestSessionEjb.findAndValidateTargetSample(targetSample);
            }
        } catch (Exception e) {
            message = e.getMessage();
            logger.error("Could not validate target sample " + targetSample, e);
        }
        return message;
    }

    /**
     * Validates target vessel and sample
     * validation
     * @return any error messages if an exception occurs
     */
    private String validateTargetVessel() {
        String message = "";

        try {
            findActiveSession();
            if(activeSession.isCovidSession()) {
                //CREATE vessel and sample if they do not exist
                manifestSessionEjb.createVesselAndSample(targetSample, targetVessel);
            }
            manifestSessionEjb.findAndValidateTargetSampleAndVessel(targetSample, targetVessel);
        } catch (Exception e) {
            message = e.getMessage();
            logger.error(String.format("Could not validate target vessel %s and sample %s", targetVessel, targetSample), e);
        }
        return message;
    }

    //  *********************************************************
    //  Getters and Setters
    //  *********************************************************s
    public String getSourceTube() {
        return sourceTube;
    }

    public void setSourceTube(String sourceTube) {
        this.sourceTube = sourceTube;
    }

    public String getTargetSample() {
        return targetSample;
    }

    public void setTargetSample(String targetSample) {
        this.targetSample = targetSample;
    }

    public String getTargetVessel() {
        return targetVessel;
    }

    public void setTargetVessel(String targetVessel) {
        this.targetVessel = targetVessel;
    }

    public String getActiveSessionId() {
        return activeSessionId;
    }

    public void setActiveSessionId(String activeSessionId) {
        this.activeSessionId = activeSessionId;
    }

    public ManifestSession getActiveSession() {
        return activeSession;
    }

    public List<ManifestSession> getClosedSessions() {
        return availableSessions;
    }
}
