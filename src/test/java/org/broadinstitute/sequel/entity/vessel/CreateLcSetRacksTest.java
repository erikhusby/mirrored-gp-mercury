package org.broadinstitute.sequel.entity.vessel;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.sequel.boundary.vessel.LabBatchBean;
import org.broadinstitute.sequel.boundary.vessel.TubeBean;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

/**
 * To prepare for sending past production BettaLIMS messages into SequeL, this class creates LC Sets and associated
 * batches of tubes that are stored in Squid.
 */
public class CreateLcSetRacksTest extends ContainerTest {

    @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION)
    public void testCreateLcSets() {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     l.KEY, " +
                "     lwd.NAME, " +
                "     r.barcode AS tube_barcode, " +
                "     ls.barcode AS sample_barcode " +
                "FROM " +
                "     lcset l " +
                "     INNER JOIN lab_workflow lw " +
                "          ON   lw.lcset_id = l.lcset_id " +
                "     INNER JOIN lab_workflow_def lwd " +
                "          ON   lwd.lab_workflow_def_id = lw.lab_workflow_def_id " +
                "     INNER JOIN lab_workflow_receptacle lwr " +
                "          ON   lwr.lab_workflow_id = lw.lab_workflow_id " +
                "     INNER JOIN receptacle r " +
                "          ON   r.receptacle_id = lwr.receptacle_id " +
                "     INNER JOIN seq_content sc " +
                "          ON   sc.receptacle_id = r.receptacle_id " +
                "     INNER JOIN seq_content_descr_set scds " +
                "          ON   scds.seq_content_id = sc.seq_content_id " +
                "     INNER JOIN seq_content_descr scd " +
                "          ON   scd.seq_content_descr_id = scds.seq_content_descr_id " +
                "     INNER JOIN gssr_pool_descr gpd " +
                "          ON   gpd.seq_content_descr_id = scd.seq_content_descr_id " +
                "     INNER JOIN lc_sample ls " +
                "          ON   ls.lc_sample_id = gpd.lc_sample_id " +
                "     INNER JOIN well_map_entry wme " +
                "          ON   wme.receptacle_id = r.receptacle_id " +
                "     INNER JOIN well_description wd " +
                "          ON   wd.well_description_id = wme.well_description_id " +
                "     INNER JOIN well_map wm " +
                "          ON   wm.well_map_id = wme.well_map_id " +
                "     INNER JOIN plate p " +
                "          ON   p.plate_id = wm.rack_plate_id " +
                "     INNER JOIN plate_transfer_event pte " +
                "          ON   pte.source_well_map_id = wm.well_map_id " +
                "     INNER JOIN plate_event pe " +
                "          ON   pe.station_event_id = pte.station_event_id " +
                "     INNER JOIN station_event se " +
                "          ON   se.station_event_id = pe.station_event_id " +
                "     INNER JOIN station_event_type set1 " +
                "          ON   set1.station_event_type_id = se.station_event_type_id " +
                "ORDER BY " +
                "     l.KEY, " +
                "     wd.NAME ");
        List resultList = nativeQuery.getResultList();
        String previousLcSet = "";
        LabBatchBean labBatch = null;
        List<TubeBean> tubeBeans = null;
        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String lcSet = (String) columns[0];
            String workflowName = (String) columns[1];
            String tubeBarcode = (String) columns[2];
            String sampleBarcode = (String) columns[3];
            if(!lcSet.equals(previousLcSet)) {
                previousLcSet = lcSet;
                if(labBatch != null) {
                    System.out.println("About to persist batch " + labBatch.getBatchId());
                    String response = null;
                    try {
                        // Use a web service, rather than just calling persist on a DAO, because a constraint
                        // violation invalidates the EntityManager.  The web service gets a fresh EntityManager for
                        // each request.
                        response = Client.create().resource("http://localhost:8181/SequeL/rest/labbatch")
                                .type(MediaType.APPLICATION_XML_TYPE)
                                .accept(MediaType.APPLICATION_XML)
                                .entity(labBatch)
                                .post(String.class);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    System.out.println(response);
                }
                tubeBeans = new ArrayList<TubeBean>();
                labBatch = new LabBatchBean(lcSet, workflowName, tubeBeans);
            }
            tubeBeans.add(new TubeBean(tubeBarcode, sampleBarcode));
        }
    }
}
