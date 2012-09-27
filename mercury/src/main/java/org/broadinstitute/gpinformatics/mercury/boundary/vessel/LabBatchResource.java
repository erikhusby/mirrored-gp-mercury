package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.project.ProjectPlanDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.project.BasicProject;
import org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.Starter;
import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.broadinstitute.gpinformatics.mercury.entity.sample.BSPStartingSampleDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * For importing data from Squid and BSP, creates a batch of tubes
 */
@Path("/labbatch")
@Stateless
public class LabBatchResource {

    @Inject
    private ProjectPlanDao projectPlanDao;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private BSPStartingSampleDAO bspStartingSampleDAO;

    @Inject
    private LabBatchDAO labBatchDAO;

    @POST
    public String createLabBatch(LabBatchBean labBatchBean) {
        List<String> tubeBarcodes = new ArrayList<String>();
        List<String> sampleBarcodes = new ArrayList<String>();
        for (TubeBean tubeBean : labBatchBean.getTubeBeans()) {
            tubeBarcodes.add(tubeBean.getBarcode());
            sampleBarcodes.add(tubeBean.getSampleBarcode());
        }

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = twoDBarcodedTubeDAO.findByBarcodes(tubeBarcodes);
        Map<String, BSPStartingSample> mapBarcodeToSample = bspStartingSampleDAO.findByNames(sampleBarcodes);
        if(labBatchBean.getWorkflowName() == null) {
            LabBatch labBatch = buildLabBatch(labBatchBean, mapBarcodeToTube, mapBarcodeToSample, null);
            labBatchDAO.persist(labBatch);
        } else {
            BasicProjectPlan projectPlan = buildProjectPlan(labBatchBean, mapBarcodeToTube, mapBarcodeToSample);
            projectPlanDao.persist(projectPlan);
        }

        projectPlanDao.flush();
        return "Batch persisted";
    }

    public BasicProjectPlan buildProjectPlan(
            LabBatchBean labBatchBean,
            Map<String, TwoDBarcodedTube> mapBarcodeToTube,
            Map<String, BSPStartingSample> mapBarcodeToSample) {
        // todo jmt fix workflow
        JiraTicket jiraTicket = new JiraTicket(new JiraServiceStub(), labBatchBean.getBatchId(), labBatchBean.getBatchId());
        BasicProject project = new BasicProject(labBatchBean.getBatchId(), jiraTicket);
        BasicProjectPlan projectPlan = new BasicProjectPlan(
                project,
                labBatchBean.getBatchId(),
                new WorkflowDescription(labBatchBean.getWorkflowName(), null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

        LabBatch labBatch = buildLabBatch(labBatchBean, mapBarcodeToTube, mapBarcodeToSample, projectPlan);
        jiraTicket.setLabBatch(labBatch);
        return projectPlan;
    }

    public LabBatch buildLabBatch(LabBatchBean labBatchBean, Map<String, TwoDBarcodedTube> mapBarcodeToTube,
            Map<String, BSPStartingSample> mapBarcodeToSample, BasicProjectPlan projectPlan) {
        Set<Starter> starters = new HashSet<Starter>();
        for (TubeBean tubeBean : labBatchBean.getTubeBeans()) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTube.get(tubeBean.getBarcode());
            if (twoDBarcodedTube == null) {
                twoDBarcodedTube = new TwoDBarcodedTube(tubeBean.getBarcode());
                mapBarcodeToTube.put(tubeBean.getBarcode(), twoDBarcodedTube);
            }

            if (tubeBean.getSampleBarcode() == null) {
                starters.add(twoDBarcodedTube);
            } else {
                BSPStartingSample bspStartingSample = mapBarcodeToSample.get(tubeBean.getSampleBarcode());
                if(bspStartingSample == null) {
                    bspStartingSample = new BSPStartingSample(tubeBean.getSampleBarcode() + ".aliquot", projectPlan);
                    mapBarcodeToSample.put(tubeBean.getSampleBarcode(), bspStartingSample);
                }

                starters.add(bspStartingSample);
                projectPlan.addStarter(bspStartingSample);
                projectPlan.addAliquotForStarter(bspStartingSample, twoDBarcodedTube);
            }
        }
        LabBatch labBatch;
        if(projectPlan == null) {
            labBatch = new LabBatch(labBatchBean.getBatchId(), starters);
        } else {
            labBatch = new LabBatch(projectPlan, labBatchBean.getBatchId(), starters);
        }
        return labBatch;
    }
}
