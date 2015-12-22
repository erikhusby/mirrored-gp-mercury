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
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.math.BigDecimal;
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
    public static final List<IlluminaFlowcell.FlowcellType> FLOWCELL_TYPES;
    static {
        List<IlluminaFlowcell.FlowcellType> createFct = new ArrayList<>();
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.YES) {
                createFct.add(flowcellType);
            }
        }
        FLOWCELL_TYPES = createFct;
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Validate(required = true, on = LOAD_ACTION)
    private String lcsetName;

    private int numberOfLanes;

    private BigDecimal loadingConc;

    private LabBatch labBatch;

    private Map<LabEvent, Set<LabVessel>> denatureTubeToEvent = new HashMap<>();

    private List<String> selectedVesselLabels;

    private List<LabBatch> createdBatches;

    private IlluminaFlowcell.FlowcellType selectedType;

    public IlluminaFlowcell.FlowcellType getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(IlluminaFlowcell.FlowcellType selectedType) {
        this.selectedType = selectedType;
    }

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

    public Map<LabEvent, Set<LabVessel>> getDenatureTubeToEvent() {
        return denatureTubeToEvent;
    }

    public void setDenatureTubeToEvent(Map<LabEvent, Set<LabVessel>> denatureTubeToEvent) {
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

    public BigDecimal getLoadingConc() {
        return loadingConc;
    }

    public void setLoadingConc(BigDecimal loadingConc) {
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
        labBatch = labBatchDao.findByBusinessKey(lcsetName);
        if (labBatch != null) {
            for (LabVessel vessel : labBatch.getStartingBatchLabVessels()) {
                // Can't use denatureTubeToEvent.putAll, because multiple denatures may be created by the same event.
                Map<LabEvent, Set<LabVessel>> mapEventToVessels = vessel.findVesselsForLabEventType(
                        LabEventType.DENATURE_TRANSFER, true);
                for (Map.Entry<LabEvent, Set<LabVessel>> labEventSetEntry : mapEventToVessels.entrySet()) {
                    Set<LabVessel> labVessels = denatureTubeToEvent.get(labEventSetEntry.getKey());
                    if (labVessels == null) {
                        labVessels = new HashSet<>();
                        denatureTubeToEvent.put(labEventSetEntry.getKey(), labVessels);
                    }
                    labVessels.addAll(labEventSetEntry.getValue());
                }
            }
        } else {
            addValidationError("lcsetText", "Could not find " + lcsetName);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * This method creates FCT tickets in JIRA and persists the relevant lab batches.
     *
     * @return A redirect resolution back to the current page.
     */
    @HandlesEvent(SAVE_ACTION)
    public Resolution createFCTTicket() {
        labBatch = labBatchDao.findByBusinessKey(lcsetName);
        createdBatches = new ArrayList<>();
        for (String denatureTubeBarcode : selectedVesselLabels) {
            Set<LabVessel> vesselSet = new HashSet<>(labVesselDao.findByListIdentifiers(selectedVesselLabels));

            LabBatch.LabBatchType batchType = selectedType.getBatchType();
            int lanesPerFlowcell = selectedType.getVesselGeometry().getVesselPositions().length;
            CreateFields.IssueType issueType = selectedType.getIssueType();
            for (int i = 0; i < numberOfLanes; i += lanesPerFlowcell) {
                LabBatch batch =
                        new LabBatch(denatureTubeBarcode + " FCT ticket", vesselSet, batchType, loadingConc, selectedType);
                batch.setBatchDescription(batch.getBatchName());
                labBatchEjb.createLabBatch(batch, userBean.getLoginUserName(), issueType, this);
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
                    numberOfLanes / lanesPerFlowcell, loadingConc, denatureTubeBarcode, createdBatchLinks.toString());
        }
        return new RedirectResolution(CreateFCTActionBean.class, VIEW_ACTION);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateOneDenatureTubeSelected() {
        if (selectedVesselLabels == null || selectedVesselLabels.size() != 1) {
            addValidationError("tubeList", "You must select a single denature tube.");
        }
    }

    public List<IlluminaFlowcell.FlowcellType> getFlowcellTypes() {
        return FLOWCELL_TYPES;
    }
}
