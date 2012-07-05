package org.broadinstitute.sequel.boundary.vessel;

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

    @POST
    public String createRack(RackBean rackBean) {
        String label = rackBean.barcode + "_" + Long.toString(System.currentTimeMillis());
        RackOfTubes rackOfTubes = new RackOfTubes(label);
        BasicProjectPlan projectPlan = null;
        if (rackBean.lcSet != null) {
            // todo jmt fix workflow
            projectPlan = new BasicProjectPlan(new BasicProject(rackBean.lcSet,
                    new JiraTicket(new JiraServiceStub(), rackBean.lcSet, rackBean.lcSet)), rackBean.lcSet,
                    new WorkflowDescription("", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));
        }

        List<String> barcodes = new ArrayList<String>();
        for (TubeBean tubeBean : rackBean.tubeBeans) {
            barcodes.add(tubeBean.barcode);
        }
        Map<String,TwoDBarcodedTube> mapBarcodeToTube = twoDBarcodedTubeDAO.findByBarcodes(barcodes);
        for (TubeBean tubeBean : rackBean.tubeBeans) {
            TwoDBarcodedTube twoDBarcodedTube = mapBarcodeToTube.get(tubeBean.barcode);
            if (twoDBarcodedTube == null) {
                if(tubeBean.sampleBarcode == null) {
                    twoDBarcodedTube = new TwoDBarcodedTube(tubeBean.barcode);
                } else {
                    BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(new BSPStartingSample(tubeBean.sampleBarcode + ".aliquot", projectPlan, null));

                    twoDBarcodedTube = bspAliquot;
                }
            }
            rackOfTubes.getVesselContainer().addContainedVessel(twoDBarcodedTube,
                    VesselPosition.getByName(tubeBean.position));
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
