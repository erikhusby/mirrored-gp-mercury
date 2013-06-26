package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding("/workflow/CreateFCT.action")
public class CreateFCTActionBean extends CoreActionBean {
    private static String VIEW_PAGE = "/workflow/create_fct.jsp";

    public static final String LOAD_ACTION = "load";

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    @Inject
    private LabBatchDAO labBatchDAO;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Validate(required = true, on = LOAD_ACTION)
    private String lcsetName;

    @Validate(required = true, on = SAVE_ACTION, minvalue = 2)
    private int numberOfLanes;

    @Validate(required = true, on = SAVE_ACTION, expression = "this > 0")
    private float loadingConc;

    private LabBatch labBatch;

    private Map<LabVessel, LabEvent> denatureTubeToEvent = new HashMap<>();

    private List<String> selectedVesselLabels;

    private List<LabBatch> createdBatches;

    public List<LabBatch> getCreatedBatches() {
        return createdBatches;
    }

    public void setCreatedBatches(List<LabBatch> createdBatches) {
        this.createdBatches = createdBatches;
    }

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
            for (LabVessel vessel : labBatch.getStartingBatchLabVessels()) {
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
        labBatch = labBatchDAO.findByBusinessKey(lcsetName);
        createdBatches = new ArrayList<>();
        for (String denatureTubeBarcode : selectedVesselLabels) {
            Set<LabVessel> vesselSet = new HashSet<>(labVesselDao.findByListIdentifiers(selectedVesselLabels));
            //create a new FCT ticket for every two lanes requested.
            for (int i = 0; i < numberOfLanes; i += 2) {
                LabBatch batch =
                        new LabBatch(denatureTubeBarcode + " FCT ticket", vesselSet, LabBatch.LabBatchType.FCT,
                                loadingConc);
                batch.setBatchDescription(batch.getBatchName());
                labBatchEjb.createLabBatch(batch, null, CreateFields.IssueType.FLOWCELL);
                createdBatches.add(batch);
                //link tickets
                labBatchEjb.linkJiraBatches(labBatch, batch);
            }
            StringBuilder createdBatchLinks = new StringBuilder("<ol>");
            for (LabBatch batch : createdBatches) {
                createdBatchLinks.append("<li><a target=\"JIRA\" href=\"");
                createdBatchLinks.append(batch.getJiraTicket().getBrowserUrl());
                createdBatchLinks.append("\" class=\"external\" target=\"JIRA\">");
                createdBatchLinks.append(batch.getBusinessKey());
                createdBatchLinks.append("</a></li>");
            }
            createdBatchLinks.append("</ol>");
            addMessage("Created {0} FCT tickets with a loading concentration of {1} for denature tube {2}. {3}",
                    numberOfLanes / 2, loadingConc, denatureTubeBarcode, createdBatchLinks.toString());
        }
        return new RedirectResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateOneDenatureTubeSelected() {
        if (selectedVesselLabels == null || selectedVesselLabels.size() != 1) {
            addValidationError("tubeList", "You must select a single denature tube.");
        }
    }
}
