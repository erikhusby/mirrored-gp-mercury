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
    private static Log logger = LogFactory.getLog(ManifestTubeTransferActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/sample/manifestTubeTransfer.action";
    public static final String TUBE_TRANSFER_PAGE = "/sample/manifest_tube_transfer.jsp";

    public static final String SCAN_SOURCE_TUBE_ACTION = "scanSource";
    public static final String SCAN_TARGET_SAMPLE_ACTION = "scanTargetSample";
    public static final String SCAN_TARGET_VESSEL_AND_SAMPLE_ACTION = "scanTargetVessel";
    public static final String RECORD_TRANSFER_ACTION = "recordTransfer";

    @Validate(required = true, on = {RECORD_TRANSFER_ACTION})
    private String sourceTube;

    @Validate(required = true, on = {RECORD_TRANSFER_ACTION})
    private String targetSample;

    @Validate(required = true, on = {RECORD_TRANSFER_ACTION})
    private String targetVessel;

    @Validate(required = true, on = {RECORD_TRANSFER_ACTION})
    private Long activeSessionId;
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

    @Before(stages = LifecycleStage.EventHandling, on = {RECORD_TRANSFER_ACTION})
    public void init() {
        findActiveSession();
    }

    private void findActiveSession() {
        activeSession = manifestSessionDao.find(activeSessionId);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION})
    public void initSessionChoices() {
        availableSessions = manifestSessionDao.findClosedSessions();
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(TUBE_TRANSFER_PAGE);
    }

    @HandlesEvent(RECORD_TRANSFER_ACTION)
    public Resolution recordTransfer() {

        Resolution resolution;

        try {
            manifestSessionEjb.transferSample(activeSessionId, sourceTube, targetSample, targetVessel,
                    userBean.getBspUser());
            addMessage("Collaborator sample {0} has been successfully recorded as transferred to vessel " +
                       "{1} with a sample of {2}", sourceTube, targetVessel, targetSample);
            resolution = new RedirectResolution(getClass(), VIEW_ACTION).addParameter("activeSessionId", activeSessionId);
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            resolution = getContext().getSourcePageResolution();
        }
        return resolution;
    }

    @HandlesEvent(SCAN_SOURCE_TUBE_ACTION)
    public Resolution scanSource() {
        String message = "";
        if(activeSessionId == null) {
            return createTextResolution("You must select a session to continue");
        }
        if(StringUtils.isBlank(sourceTube)) {
            return createTextResolution("You must enter a value for the source Tube");
        }

        findActiveSession();

        if(activeSession == null) {
            return createTextResolution("The selected session could not be found");
        }
        try {
            manifestSessionEjb.validateSourceTubeForTransfer(activeSessionId, sourceTube);
        } catch (Exception e) {
            message = e.getMessage();
        }

        return createTextResolution(message);
    }

    @HandlesEvent(SCAN_TARGET_SAMPLE_ACTION)
    public Resolution scanTargetSample() {

        String message = "";
        try {
            manifestSessionEjb.validateTargetSample(targetSample);
        } catch (Exception e) {
            message = e.getMessage();
        }

        return createTextResolution(message);
    }

    @HandlesEvent(SCAN_TARGET_VESSEL_AND_SAMPLE_ACTION)
    public Resolution scanTargetVessel() {
        String message = "";

        try {
            manifestSessionEjb.validateTargetSampleAndVessel(targetSample, targetVessel);
        } catch (Exception e) {
            message = e.getMessage();
        }
        return createTextResolution(message);
    }

    // Getters and Setters
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

    public Long getActiveSessionId() {
        return activeSessionId;
    }

    public void setActiveSessionId(Long activeSessionId) {
        this.activeSessionId = activeSessionId;
    }

    public ManifestSession getActiveSession() {
        return activeSession;
    }

    public List<ManifestSession> getAvailableSessions() {
        return availableSessions;
    }

}