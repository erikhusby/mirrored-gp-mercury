package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
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

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private GenericReagentDao reagentDao;

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

    @Test(enabled = false)
    public void fixupGplim2250() {
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 245583L);
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupBsp1181v1() {
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 304946L);
        LabBatch labBatch = labBatchDao.findById(LabBatch.class, 51107L);
        // nulling out LabBatch with ID 51107 (BP-46374) to correct swapped dilution and black plate issue.
        labEvent.setLabBatch(null);
        labBatch.getLabEvents().remove(labEvent);

        labEventDao.persist(labEvent);
        labBatchDao.persist(labBatch);
    }

    @Test(enabled = false)
    public void fixupBsp1181v2() {
        // Fixup v1 did not do the trick, Mercury would still overflow the stack chasing these transfers.  v2 deletes
        // the event altogether.
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 304946L);
        VesselTransfer vesselTransfer = labEventDao.findById(VesselTransfer.class, 79056L);
        labEventDao.remove(vesselTransfer);
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupBsp1181v3() {
        // Delete the Pico buffer addition event on the dilution plate.
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 304947L);
        GenericReagent reagent = reagentDao.findByReagentNameAndLot("Pico", "RG-5652");
        labEvent.getReagents().remove(reagent);
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupGplim2377() {
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 319206L);
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupGplim2367() {
        // Delete cherry pick.  LabVessel.FixupTest.fixupGplim2367Part2 changes the tube formation to achieve the same
        // affect, without confusing getSampleInstances.
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 317839L);
        labEvent.getCherryPickTransfers().clear();
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupGplim2393() {
        // After undoing UPDATE statement in the ticket...
        // Override routing for shearing transfer
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 324061L);
        LabBatch labBatch = labBatchDao.findByName("LCSET-4771");
        labEvent.setManualOverrideLcSet(labBatch);
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim2336() {
        // After undoing the UPDATE statement in the ticket...

        // Fix 2 lab batches exception for 130912_SL-HCB_0354_BFCH72CAADXX
        // Override routing for shearing transfer
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 217038L);
        LabBatch labBatch = labBatchDao.findByName("LCSET-4069");
        labEvent.setManualOverrideLcSet(labBatch);

        // Fix 3 lab batches exception for 131119_SL-MAG_0203_FC000000000-A6DL6
        // Override routing for shearing transfer
        labEvent = labEventDao.findById(LabEvent.class, 284615L);
        labBatch = labBatchDao.findByName("LCSET-4486");
        labEvent.setManualOverrideLcSet(labBatch);

        labEventDao.flush();
    }

    // Undoes a BSP export to mercury
    @Test(enabled = false)
    public void fixupGplim2535() {
        for (long labEventId = 384504; labEventId <= 384598; ++labEventId) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, labEventId);
            if (labEvent != null) {
                labEventDao.remove(labEvent);
            }
        }
    }
}