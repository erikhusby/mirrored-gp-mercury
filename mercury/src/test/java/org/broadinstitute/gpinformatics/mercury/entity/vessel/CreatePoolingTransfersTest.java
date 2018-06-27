package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STUBBY;

/**
 * Imports from Squid the pooling transfers that were done in the user interface, before this transfer was messaged.
 * Wildfly rejects deploying with non-existing persistence unit - uncomment attribute if running
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class CreatePoolingTransfersTest extends StubbyContainerTest {

    public CreatePoolingTransfersTest(){}

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private LabEventDao labEventDao;

    //@PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Inject
    BSPUserList bspUserList;

    @Test(enabled = false, groups = STUBBY)
    public void testImport() throws ParseException {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                                                            "    r.barcode as source_tube_barcode, " +
                                                            "    r2.barcode as target_tube_barcode, " +
//                "    wd.NAME as well_name, " +
//                "    p.barcode as rack_barcode, " +
                                                            "    se.start_time, " +
                                                            "    lm.machine_name, " +
                                                            "    s.user_id " +
                                                            "FROM " +
                                                            "    receptacle_transfer_event rte " +
                                                            "    INNER JOIN receptacle r " +
                                                            "        ON   r.receptacle_id = rte.src_receptacle_id " +
//                "    LEFT OUTER JOIN well_map_entry wme " +
//                "        ON   wme.receptacle_id = r.receptacle_id " +
//                "    LEFT OUTER JOIN well_description wd " +
//                "        ON   wd.well_description_id = wme.well_description_id " +
//                "    LEFT OUTER JOIN well_map wm " +
//                "        ON   wm.well_map_id = wme.well_map_id " +
//                "    LEFT OUTER JOIN plate p " +
//                "        ON   p.plate_id = wm.rack_plate_id " +
                                                            "    INNER JOIN receptacle_event re " +
                                                            "        ON   re.station_event_id = rte.station_event_id " +
                                                            "    INNER JOIN receptacle r2 " +
                                                            "        ON   r2.receptacle_id = re.receptacle_id " +
                                                            "    INNER JOIN station_event se " +
                                                            "        ON   se.station_event_id = re.station_event_id " +
                                                            "    INNER JOIN staff s " +
                                                            "        ON   s.staff_id = se.staff_id " +
                                                            "    INNER JOIN lab_machine lm " +
                                                            "        ON   lm.lab_machine_id = se.station_id " +
                                                            "WHERE " +
                                                            "    re.receptacle_id IN (SELECT " +
                                                            "                             re.receptacle_id " +
                                                            "                         FROM " +
                                                            "                             receptacle_transfer_event rte "
                                                            +
                                                            "                             INNER JOIN receptacle_event re "
                                                            +
                                                            "                                 ON   re.station_event_id = rte.station_event_id "
                                                            +
                                                            "                             INNER JOIN station_event se "
                                                            +
                                                            "                                 ON   se.station_event_id = re.station_event_id "
                                                            +
                                                            "                             INNER JOIN program p " +
                                                            "                                 ON   p.program_id = se.program_id "
                                                            +
                                                            "                         WHERE " +
                                                            "                             p.program_name IN ('SquidUI', 'LimRec') "
                                                            +
                                                            "                         GROUP BY " +
                                                            "                             re.receptacle_id " +
                                                            "                         HAVING " +
                                                            "                             COUNT(*) > 5) " +
                                                            "    AND se.start_time >= TO_DATE('2011-01-01', 'YYYY-MM-DD') "
                                                            +
                                                            "    AND lm.machine_name = 'UnknownReceptacleEventLocation' "
                                                            +
                                                            "ORDER BY " +
                                                            "    target_tube_barcode, " +
                                                            "    well_name ");
        nativeQuery.setHint("org.hibernate.timeout", 600);
        List resultList = nativeQuery.getResultList();
        String previousTargetTubeBarcode = "";

        List<String> sourceTubeBarcodes = null;

//        BSPUserList bspUserList = ServiceAccessUtility.getBean ( BSPUserList.class );


        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String sourceTubeBarcode = (String) columns[0];
            String targetTubeBarcode = (String) columns[1];
            Timestamp eventDate = (Timestamp) columns[2];
            String eventLocation = (String) columns[3];
            String operator = (String) columns[4];

            if (!targetTubeBarcode.equals(previousTargetTubeBarcode)) {
                previousTargetTubeBarcode = targetTubeBarcode;
                if (sourceTubeBarcodes != null) {
                    LabEvent genericLabEvent = new LabEvent(LabEventType.POOLING_TRANSFER,
                            eventDate, eventLocation, 1L, bspUserList.getByUsername(operator).getUserId(),
                            "createPoolingTransfersTest");

                    BarcodedTube targetTube = barcodedTubeDao.findByBarcode(targetTubeBarcode);
                    if (targetTube == null) {
                        targetTube = new BarcodedTube(targetTubeBarcode);
                    }

                    Map<String, BarcodedTube> mapBarcodeToSourceTube =
                            barcodedTubeDao.findByBarcodes(sourceTubeBarcodes);
                    for (String tubeBarcode : sourceTubeBarcodes) {
                        BarcodedTube sourceTube = mapBarcodeToSourceTube.get(tubeBarcode);
                        if (sourceTube == null) {
                            sourceTube = new BarcodedTube(tubeBarcode);
                        }
                        genericLabEvent.getVesselToVesselTransfers().add(new VesselToVesselTransfer(
                                sourceTube,
                                targetTube,
                                genericLabEvent));
                    }
                    labEventDao.persist(genericLabEvent);
                    labEventDao.flush();
                    labEventDao.clear();
                }
                sourceTubeBarcodes = new ArrayList<>();
            }
            sourceTubeBarcodes.add(sourceTubeBarcode);
        }
    }

/*
    private String persistRack(RackBean sourceRackOfTubes) {
        // Use a web service, rather than just calling persist on a DAO, because a constraint
        // violation invalidates the EntityManager.  The web service gets a fresh EntityManager for
        // each request.
        String label = Client.create().resource("http://localhost:8181/Mercury/rest/rackoftubes")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(sourceRackOfTubes)
                .post(String.class);
        System.out.println(label);
        return label;
    }
*/
}
