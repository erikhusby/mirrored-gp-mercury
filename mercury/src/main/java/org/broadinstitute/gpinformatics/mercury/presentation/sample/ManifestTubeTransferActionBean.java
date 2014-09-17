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

    @Validate(required = true, on = {RECORD_TRANSFER_ACTION, SCAN_SOURCE_TUBE_ACTION})
    private String sourceTube;

    @Validate(required = true, on = {SCAN_TARGET_VESSEL_AND_SAMPLE_ACTION, SCAN_TARGET_SAMPLE_ACTION,
            RECORD_TRANSFER_ACTION})
    private String targetSample;

    @Validate(required = true, on = {SCAN_TARGET_VESSEL_AND_SAMPLE_ACTION, RECORD_TRANSFER_ACTION})
    private String targetVessel;

    @Validate(required = true, on = {"!"+VIEW_ACTION})
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

    @Before(stages = LifecycleStage.EventHandling, on = {"!"+VIEW_ACTION})
    public void init() {
        activeSession = manifestSessionDao.find(activeSessionId);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION})
    public void initSessionChoices() {
        availableSessions = manifestSessionDao.findClosedSessions();
    }

    @DefaultHandler
    public Resolution view() {
        return new RedirectResolution(TUBE_TRANSFER_PAGE);
    }

    @HandlesEvent(RECORD_TRANSFER_ACTION)
    public Resolution recordTransfer() {
        try {
            manifestSessionEjb.transferSample(activeSessionId, sourceTube, targetSample, targetVessel,
                    userBean.getBspUser());
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
        }
        return new ForwardResolution(getClass(), VIEW_ACTION);
    }

    @HandlesEvent(SCAN_SOURCE_TUBE_ACTION)
    public Resolution scanSource() {
        String message = "";
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