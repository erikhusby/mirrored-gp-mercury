package org.broadinstitute.sequel.entity.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * To prepare for sending past production BettaLIMS messages into SequeL, this class creates LC Sets and associated
 * racks of tubes.
 */
public class CreateLcSetRacksTest extends ContainerTest {
    private static final Log log = LogFactory.getLog(CreateLcSetRacksTest.class);

    @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Test(enabled = false)
    public void testCreateLcSets() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "    l.\"KEY\", " +
                "    p.barcode as rack_barcode, " +
                "    wd.\"NAME\", " +
                "    r.barcode as tube_barcode, " +
                "    ls.barcode as sample_barcode " +
                "FROM " +
                "    lcset l " +
                "    INNER JOIN lab_workflow lw " +
                "        ON   lw.lcset_id = l.lcset_id " +
                "    INNER JOIN lab_workflow_receptacle lwr " +
                "        ON   lwr.lab_workflow_id = lw.lab_workflow_id " +
                "    INNER JOIN receptacle r " +
                "        ON   r.receptacle_id = lwr.receptacle_id " +
                "    INNER JOIN seq_content sc " +
                "        ON   sc.receptacle_id = r.receptacle_id " +
                "    INNER JOIN seq_content_descr_set scds " +
                "        ON   scds.seq_content_id = sc.seq_content_id " +
                "    INNER JOIN seq_content_descr scd " +
                "        ON   scd.seq_content_descr_id = scds.seq_content_descr_id " +
                "    INNER JOIN gssr_pool_descr gpd " +
                "        ON   gpd.seq_content_descr_id = scd.seq_content_descr_id " +
                "    INNER JOIN lc_sample ls " +
                "        ON   ls.lc_sample_id = gpd.lc_sample_id " +
                "    INNER JOIN well_map_entry wme " +
                "        ON   wme.receptacle_id = r.receptacle_id " +
                "    INNER JOIN well_description wd " +
                "        ON   wd.well_description_id = wme.well_description_id " +
                "    INNER JOIN well_map wm " +
                "        ON   wm.well_map_id = wme.well_map_id " +
                "    INNER JOIN plate p " +
                "        ON   p.plate_id = wm.rack_plate_id " +
                "    INNER JOIN plate_transfer_event pte " +
                "        ON   pte.source_well_map_id = wm.well_map_id " +
                "    INNER JOIN plate_event pe " +
                "        ON   pe.station_event_id = pte.station_event_id " +
                "    INNER JOIN station_event se " +
                "        ON   se.station_event_id = pe.station_event_id " +
                "    INNER JOIN station_event_type set1 " +
                "        ON   set1.station_event_type_id = se.station_event_type_id " +
                "ORDER BY " +
                "    l.\"KEY\", " +
                "    wd.\"NAME\" ");
        List resultList = nativeQuery.getResultList();
        String previousLcSet = "";
        RackOfTubes rackOfTubes = null;
        ProjectPlan projectPlan = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String lcSet = (String) columns[0];
            String rackBarcode = (String) columns[1];
            String wellName = (String) columns[2];
            String tubeBarcode = (String) columns[3];
            String sampleBarcode = (String) columns[4];
            if(!lcSet.equals(previousLcSet)) {
                previousLcSet = lcSet;
                if(rackOfTubes != null) {
                    System.out.println("About to persist rack " + rackOfTubes.getLabel() + ", LCSet " + lcSet);
                    try {
                        rackOfTubesDao.persist(rackOfTubes);
                        rackOfTubesDao.flush();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    rackOfTubesDao.clear();
                }
                rackOfTubes = new RackOfTubes(rackBarcode);
                // todo jmt fix workflow
                projectPlan = new ProjectPlan(new BasicProject(lcSet, new JiraTicket(new DummyJiraService(), lcSet, lcSet)), lcSet,
                        new WorkflowDescription("", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));
            }
            SampleSheet sampleSheet = new SampleSheet();
            sampleSheet.addStartingSample(new BSPSample(sampleBarcode, projectPlan));
            rackOfTubes.getVesselContainer().addContainedVessel(new TwoDBarcodedTube(tubeBarcode, sampleSheet), VesselPosition.getByName(wellName));
        }
    }
}
