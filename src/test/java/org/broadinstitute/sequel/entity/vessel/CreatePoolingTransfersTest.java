package org.broadinstitute.sequel.entity.vessel;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.sequel.test.BettaLimsMessageFactory;
import org.broadinstitute.sequel.boundary.vessel.RackBean;
import org.broadinstitute.sequel.boundary.vessel.TubeBean;
import org.broadinstitute.sequel.control.dao.labevent.LabEventDao;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.sequel.entity.labevent.CherryPickTransfer;
import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventType;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.MediaType;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Imports from Squid the pooling transfers that were done in the user interface, before this transfer was messaged.
 */
public class CreatePoolingTransfersTest extends ContainerTest {
    @Inject
    private PersonDAO personDAO;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Inject
    private LabEventDao labEventDao;

    @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION)
    public void testImport() throws ParseException {
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "    r.barcode as source_tube_barcode, " +
                "    r2.barcode as target_tube_barcode, " +
                "    wd.NAME as well_name, " +
                "    p.barcode as rack_barcode, " +
                "    se.start_time, " +
                "    lm.machine_name, " +
                "    s.user_id " +
                "FROM " +
                "    receptacle_transfer_event rte " +
                "    INNER JOIN receptacle r " +
                "        ON   r.receptacle_id = rte.src_receptacle_id " +
                "    LEFT OUTER JOIN well_map_entry wme " +
                "        ON   wme.receptacle_id = r.receptacle_id " +
                "    LEFT OUTER JOIN well_description wd " +
                "        ON   wd.well_description_id = wme.well_description_id " +
                "    LEFT OUTER JOIN well_map wm " +
                "        ON   wm.well_map_id = wme.well_map_id " +
                "    LEFT OUTER JOIN plate p " +
                "        ON   p.plate_id = wm.rack_plate_id " +
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
                "                             receptacle_transfer_event rte " +
                "                             INNER JOIN receptacle_event re " +
                "                                 ON   re.station_event_id = rte.station_event_id " +
                "                             INNER JOIN station_event se " +
                "                                 ON   se.station_event_id = re.station_event_id " +
                "                             INNER JOIN program p " +
                "                                 ON   p.program_id = se.program_id " +
                "                         WHERE " +
                "                             p.program_name IN ('SquidUI', 'LimRec') " +
                "                         GROUP BY " +
                "                             re.receptacle_id " +
                "                         HAVING " +
                "                             COUNT(*) > 5) " +
                "    AND se.start_time >= TO_DATE('2011-01-01', 'YYYY-MM-DD') " +
                "    AND lm.machine_name = 'UnknownReceptacleEventLocation' " +
                "ORDER BY " +
                "    target_tube_barcode, " +
                "    well_name ");
        nativeQuery.setHint("org.hibernate.timeout", new Integer(600));
        List resultList = nativeQuery.getResultList();
        String previousTargetTubeBarcode = "";
        RackBean sourceRackOfTubes = null;
        List<TubeBean> tubeBeans = null;
        int wellIndex = 0;
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        for (Object o : resultList) {
            wellIndex++;
            Object[] columns = (Object[]) o;
            String sourceTubeBarcode = (String) columns[0];
            String targetTubeBarcode = (String) columns[1];
            String wellName = (String) columns[2];
            if(wellName == null) {
                wellName = bettaLimsMessageFactory.buildWellName(wellIndex);
            }
            String rackBarcode = (String) columns[3];
            if(rackBarcode == null) {
                rackBarcode = Long.toString(System.currentTimeMillis());
            }
            Timestamp eventDate = (Timestamp) columns[4];
            String eventLocation = (String) columns[5];
            String operator = (String) columns[6];

            if(!targetTubeBarcode.equals(previousTargetTubeBarcode)) {
                previousTargetTubeBarcode = targetTubeBarcode;
                if(sourceRackOfTubes != null) {
                    try {
                        System.out.println("About to persist rack " + sourceRackOfTubes.barcode + ", target tube " + targetTubeBarcode);
                        String sourceRackLabel = persistRack(sourceRackOfTubes);
                        RackBean targetRackOfTubes = new RackBean(Long.toString(System.currentTimeMillis() + 1), null,
                                Arrays.asList(new TubeBean(targetTubeBarcode, "A01", null)));
                        String targetRackLabel = persistRack(targetRackOfTubes);
                        GenericLabEvent genericLabEvent = new GenericLabEvent(LabEventType.POOLING_TRANSFER,
                                eventDate, eventLocation, 1L, personDAO.findByName(operator));
                        RackOfTubes dbSourceRackOfTubes = rackOfTubesDao.getByLabel(sourceRackLabel);
                        RackOfTubes dbTargetRackOfTubes = rackOfTubesDao.getByLabel(targetRackLabel);
                        for (TubeBean tubeBean : tubeBeans) {
                            genericLabEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                                    dbSourceRackOfTubes.getVesselContainer(),
                                    VesselPosition.getByName(tubeBean.position),
                                    dbTargetRackOfTubes.getVesselContainer(),
                                    VesselPosition.getByName("A01"),
                                    genericLabEvent));
                        }
                        labEventDao.persist(genericLabEvent);
                        labEventDao.flush();
                        labEventDao.clear();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                tubeBeans = new ArrayList<TubeBean>();
                wellIndex = 0;
                sourceRackOfTubes = new RackBean(rackBarcode, null, tubeBeans);
            }
            tubeBeans.add(new TubeBean(sourceTubeBarcode, wellName, null));
        }
    }

    private String persistRack(RackBean sourceRackOfTubes) {
        // Use a web service, rather than just calling persist on a DAO, because a constraint
        // violation invalidates the EntityManager.  The web service gets a fresh EntityManager for
        // each request.
        String label = Client.create().resource("http://localhost:8181/SequeL/rest/rackoftubes")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(sourceRackOfTubes)
                .post(String.class);
        System.out.println(label);
        return label;
    }
}
