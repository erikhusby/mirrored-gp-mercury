package org.broadinstitute.sequel.boundary.vessel;

import org.broadinstitute.sequel.control.dao.vessel.BSPSampleAuthorityTwoDTubeDao;
import org.broadinstitute.sequel.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.sequel.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.broadinstitute.sequel.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * For importing data from Squid, creates a rack of tubes
 */
@Path("/rackoftubes")
@Stateless
public class RackOfTubesResource {

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private BSPSampleAuthorityTwoDTubeDao bspSampleAuthorityTwoDTubeDao;

    @POST
    public String createRack(RackBean rackBean) {
        String label = rackBean.barcode + "_" + Long.toString(System.currentTimeMillis());
        RackOfTubes rackOfTubes = new RackOfTubes(label);
        BasicProjectPlan projectPlan = null;
        JiraTicket jiraTicket = null;
        if (rackBean.lcSet != null) {
            // todo jmt fix workflow
            jiraTicket = new JiraTicket(new JiraServiceStub(), rackBean.lcSet, rackBean.lcSet);
            projectPlan = new BasicProjectPlan(new BasicProject(rackBean.lcSet,
                    jiraTicket), rackBean.lcSet,
                    new WorkflowDescription("", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));
        }

        List<String> tubeBarcodes = new ArrayList<String>();
        List<String> sampleBarcodes = new ArrayList<String>();
        for (TubeBean tubeBean : rackBean.tubeBeans) {
            tubeBarcodes.add(tubeBean.barcode);
            sampleBarcodes.add(tubeBean.sampleBarcode);
        }
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = twoDBarcodedTubeDAO.findByBarcodes(tubeBarcodes);
        Map<String, BSPSampleAuthorityTwoDTube> mapBarcodeToSample = bspSampleAuthorityTwoDTubeDao.findByLabels(sampleBarcodes);

        for (TubeBean tubeBean : rackBean.tubeBeans) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTube.get(tubeBean.barcode);
            if (twoDBarcodedTube == null) {
                BSPSampleAuthorityTwoDTube bspSampleAuthorityTwoDTube = mapBarcodeToSample.get(tubeBean.sampleBarcode);
                if(bspSampleAuthorityTwoDTube == null) {
                    BSPStartingSample bspStartingSample = new BSPStartingSample(tubeBean.sampleBarcode + ".aliquot", projectPlan);
                    bspSampleAuthorityTwoDTube = new BSPSampleAuthorityTwoDTube(bspStartingSample);
                    mapBarcodeToSample.put(tubeBean.sampleBarcode, bspSampleAuthorityTwoDTube);
                }
                if(tubeBean.sampleBarcode == null) {
                    twoDBarcodedTube = new TwoDBarcodedTube(tubeBean.barcode);
                } else {
                    if (projectPlan != null) {
                        projectPlan.addStarter(bspSampleAuthorityTwoDTube);
                        // todo jmt not sure if this is necessary, but TransferTraverserTest expects it
                        projectPlan.addAliquotForStarter(bspSampleAuthorityTwoDTube, bspSampleAuthorityTwoDTube);
                    }
                    twoDBarcodedTube = bspSampleAuthorityTwoDTube;
                }
            }
            rackOfTubes.getVesselContainer().addContainedVessel(twoDBarcodedTube,
                    VesselPosition.getByName(tubeBean.position));
        }
        if (jiraTicket != null) {
            jiraTicket.setLabBatch(new LabBatch(projectPlan, rackBean.lcSet, projectPlan.getStarters()));
        }
        rackOfTubes.makeDigest();
        List<RackOfTubes> byDigest = rackOfTubesDao.findByDigest(rackOfTubes.getDigest());
        if(byDigest.isEmpty()) {
            rackOfTubesDao.persist(rackOfTubes);
            rackOfTubesDao.flush();
            return label;
        }
        return byDigest.get(0).getLabel();
    }
}
