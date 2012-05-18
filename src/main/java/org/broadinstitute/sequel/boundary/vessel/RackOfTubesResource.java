package org.broadinstitute.sequel.boundary.vessel;

import org.broadinstitute.sequel.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * For importing data from Squid, creates a rack of tubes
 */
@Path("/rackoftubes")
@Stateless
public class RackOfTubesResource {
    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @POST
    public void createRack(RackBean rackBean) {
        RackOfTubes rackOfTubes = new RackOfTubes(rackBean.barcode);
        // todo jmt fix workflow
        ProjectPlan projectPlan = new ProjectPlan(new BasicProject(rackBean.lcSet,
                new JiraTicket(new DummyJiraService(), rackBean.lcSet, rackBean.lcSet)), rackBean.lcSet,
                new WorkflowDescription("", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

        for (TubeBean tubeBean : rackBean.tubeBeans) {
            SampleSheet sampleSheet = new SampleSheet();
            sampleSheet.addStartingSample(new BSPSample(tubeBean.sampleBarcode, projectPlan));
            rackOfTubes.getVesselContainer().addContainedVessel(new TwoDBarcodedTube(tubeBean.barcode, sampleSheet),
                    VesselPosition.getByName(tubeBean.position));
        }
        rackOfTubes.makeDigest();
        rackOfTubesDao.persist(rackOfTubes);
    }
}
