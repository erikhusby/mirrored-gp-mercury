package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.GregorianCalendar;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to LabEvent entities
 */
public class LabEventFixupTest extends Arquillian {

    @Inject
    private LabEventDao labEventDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupBsp346() {
        long[] ids = {110968L, 110972L, 110976L, 110980L, 111080L};
        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            labEventDao.remove(labEvent);
        }
    }

    @Test(enabled = false)
    public void deleteBackfillDaughterPlateEvents() {
        List<LabEvent> byDate = labEventDao.findByDate(new GregorianCalendar(2013, 4, 16, 0, 0).getTime(),
                new GregorianCalendar(2013, 4, 16, 0, 30).getTime());
        for (LabEvent labEvent : byDate) {
            labEventDao.remove(labEvent);
        }
        // expecting 1417 events
    }

    @Test(enabled = false)
    public void fixupGplim1622() {
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 112964L);
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupGplim1938() {
        // Deletes duplicate transfer from tube 346131 to plate 508281
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 192263L);
        VesselTransfer vesselTransfer = labEventDao.findById(VesselToSectionTransfer.class, 54160L);
        labEventDao.remove(vesselTransfer);
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupBsp581() {
        /*
        SELECT
            le.lab_event_id
        FROM
            lab_vessel lv
            INNER JOIN vessel_transfer vt
                ON   vt.source_vessel = lv.lab_vessel_id
            INNER JOIN lab_event le
                ON   le.lab_event_id = vt.lab_event
        WHERE
            lv."LABEL" IN ('CO-6584145', 'CO-6940995', 'CO-6940991', 'CO-6661848',
                          'CO-6940992', 'CO-6656416', 'CO-6641388', 'CO-6630669',
                          'CO-6629602', 'CO-6665938', 'CO-6660878', 'CO-6661363',
                          'CO-4405034', 'CO-4301567');
         */
        long[] ids = {
                154585L,
                154586L,
                154587L,
                152104L,
                152105L,
                152106L,
                152597L,
                152598L,
                152599L,
                153755L,
                153756L,
                153757L,
                154044L,
                154045L,
                154046L,
                154582L,
                154583L,
                154584L,
                151957L,
                151958L,
                151959L,
                146192L,
                146193L,
                146194L,
                146204L,
                146205L,
                146206L,
                152109L,
                152110L,
                152111L,
                152112L,
                152113L,
                152114L,
                146195L,
                146196L,
                146197L,
                146198L,
                146199L,
                146200L,
                146201L,
                146202L,
                146203L};
        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            labEvent.getReagents().clear();
            labEventDao.remove(labEvent);
        }
    }
}
