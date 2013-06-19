package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UrlBinding("/workflow/CreateFCT.action")
public class CreateFCTActionBean extends CoreActionBean {
    private static String VIEW_PAGE = "/workflow/create_fct.jsp";
    public static final String LOAD_ACTION = "load";

    @Inject
    private LabBatchDAO labBatchDAO;
    @Validate(required = true, on = LOAD_ACTION)
    private String lcsetName;
    private LabBatch labBatch;
    @Validate(required = true, on = SAVE_ACTION, minvalue = 2)
    private int numberOfLanes;
    @Validate(required = true, on = SAVE_ACTION, expression = "this > 0")
    private float loadingConc;
    private Map<LabVessel, LabEvent> denatureTubeToEvent = new HashMap<>();
    private List<String> selectedVesselLabels;

    public String getLcsetName() {
        return lcsetName;
    }

    public void setLcsetName(String lcsetName) {
        this.lcsetName = lcsetName;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public void setLabBatch(LabBatch labBatch) {
        this.labBatch = labBatch;
    }

    public Map<LabVessel, LabEvent> getDenatureTubeToEvent() {
        return denatureTubeToEvent;
    }

    public void setDenatureTubeToEvent(Map<LabVessel, LabEvent> denatureTubeToEvent) {
        this.denatureTubeToEvent = denatureTubeToEvent;
    }

    public List<String> getSelectedVesselLabels() {
        return selectedVesselLabels;
    }

    public void setSelectedVesselLabels(List<String> selectedVesselLabels) {
        this.selectedVesselLabels = selectedVesselLabels;
    }

    public int getNumberOfLanes() {
        return numberOfLanes;
    }

    public void setNumberOfLanes(int numberOfLanes) {
        this.numberOfLanes = numberOfLanes;
    }

    public float getLoadingConc() {
        return loadingConc;
    }

    public void setLoadingConc(float loadingConc) {
        this.loadingConc = loadingConc;
    }

    /**
     * This method reloads the LCSET after validation on the save action so that we have some fields to hang our
     * validation errors on.
     */
    @After(stages = LifecycleStage.BindingAndValidation, on = SAVE_ACTION)
    public void init() {
        if (lcsetName != null) {
            loadLCSet();
        }
    }

    @HandlesEvent(VIEW_ACTION)
    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * Loads the LCSet from the UI and iterates over all the starting vessels looking for any descendants that took
     * part in the Denature Transfer step.
     *
     * @return A forward resolution to the current page.
     */
    @HandlesEvent(LOAD_ACTION)
    public Resolution loadLCSet() {
        labBatch = labBatchDAO.findByBusinessKey(lcsetName);
        if (labBatch != null) {
            for (LabVessel vessel : labBatch.getStartingLabVessels()) {
                denatureTubeToEvent.putAll(vessel.findVesselsForLabEventType(LabEventType.DENATURE_TRANSFER));
            }
        } else {
            addValidationError("lcsetText", "Could not find " + lcsetName);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * This method creates FCT tickets in JIRA and persists the relevant lab batches.
     *
     * @return A forward resolution back to the current page.
     */
    @HandlesEvent(SAVE_ACTION)
    public Resolution createFCTTicket() {
        //create FCT tickets and lab batches here
        for (String denatureTubeBarcode : selectedVesselLabels) {
            addMessage("Created {0} FCT tickets with a loading concentration of {1} for denature tube {2}.",
                    numberOfLanes / 2, loadingConc, denatureTubeBarcode);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateOneDenatureTubeSelected() {
        if (selectedVesselLabels == null || selectedVesselLabels.size() != 1) {
            addValidationError("tubeList", "You must select a single denature tube.");
        }
    }
}
