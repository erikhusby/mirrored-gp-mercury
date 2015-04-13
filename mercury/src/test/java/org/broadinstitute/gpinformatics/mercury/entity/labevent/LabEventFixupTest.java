package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to LabEvent entities
 */
@Test(groups = TestGroups.FIXUP)
public class LabEventFixupTest extends Arquillian {

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private GenericReagentDao reagentDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private UserBean userBean;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

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

    @Test(enabled = false)
    public void fixupGplim2611() {
        // Override routing for shearing transfer
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 401897L);
        LabBatch labBatch = labBatchDao.findByName("LCSET-5177");
        labEvent.setManualOverrideLcSet(labBatch);
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim2706() {
        // Override routing for shearing transfer
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 445419L);
        LabBatch labBatch = labBatchDao.findByName("LCSET-5390");
        labEvent.setManualOverrideLcSet(labBatch);
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupIpi61190() {
        // Override routing for shearing transfer
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 477915L);
        LabBatch labBatch = labBatchDao.findByName("LCSET-5496");
        labEvent.setManualOverrideLcSet(labBatch);
        labEventDao.flush();
    }

    /**
     * Delete a transfer that was resubmitted with a changed disambiguator.
     */
    @Test(enabled = false)
    public void fixupGplim2535Part2() {
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 385541L);
        labEventDao.remove(labEvent);
    }

    // Removes auto daughter plate creation causing cyclic transfer
    @Test(enabled = false)
    public void fixupGplim2568() {
        long[] ids = {399358L};
        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent != null) {
                labEventDao.remove(labEvent);
            }
        }
    }

    @Test(enabled = false)
    public void fixupFct18386() {
        // Delete BSP UI daughter plate transfer, which duplicates earlier deck automation transfer.
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 524316L);
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupFct18237() {
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 516658L);
        labEvent.getReagents().clear();
        labEventDao.remove(labEvent);
        labEvent = labEventDao.findById(LabEvent.class, 516659L);
        labEvent.getReagents().clear();
        labEventDao.remove(labEvent);
    }

    @Test(enabled = false)
    public void fixupPo743 () {
        BarcodedTube tube = barcodedTubeDao.findByBarcode("0168281750");
        Assert.assertEquals(tube.getTransfersTo().size(), 1);
        LabEvent labEvent = tube.getTransfersTo().iterator().next();
        for (CherryPickTransfer cherryPickTransfer : labEvent.getCherryPickTransfers()) {
            switch (cherryPickTransfer.getTargetPosition()) {
                case A02:
                    cherryPickTransfer.setSourcePosition(VesselPosition.C01);
                    break;
                case C01:
                    cherryPickTransfer.setSourcePosition(VesselPosition.A01);
                    break;
                case B02:
                    cherryPickTransfer.setSourcePosition(VesselPosition.C01);
                    break;
                case G01:
                    cherryPickTransfer.setSourcePosition(VesselPosition.B01);
                    break;
                case H01:
                    cherryPickTransfer.setSourcePosition(VesselPosition.B01);
                    break;
            }
        }
        barcodedTubeDao.flush();
    }

    /**
     * This is done after importing index plates from Squid.
     */
    @Test(enabled = false)
    public void fixupGplim3164() {
        userBean.loginOSUser();
        StaticPlate staticPlateEmpty = staticPlateDao.findByBarcode("000001814423-GPLIM-3164");
        StaticPlate staticPlateIndexed = staticPlateDao.findByBarcode("000001814423");
        for (LabEvent labEvent : staticPlateEmpty.getTransfersFrom()) {
            for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                System.out.println("Changing source for labEvent " + labEvent.getLabEventId());
                sectionTransfer.setSourceVesselContainer(staticPlateIndexed.getContainerRole());
            }
        }
        staticPlateDao.flush();
    }

    @Test(enabled = false)
    public void gplim3126fixupMachineName() {
        userBean.loginOSUser();
        for (long id : new Long[] {687513L, 687557L}) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent == null) {
                throw new RuntimeException("cannot find " + id);
            }
            System.out.println("LabEvent " + id + " location " + labEvent.getEventLocation());
            labEvent.setEventLocation("JON_HAMM");
            System.out.println("   updated to " + labEvent.getEventLocation());
            labEventDao.persist(new FixupCommentary(
                    "GPLIM-3126 incorrect machine configuration caused messages to be sent with the wrong machine name."));
            labEventDao.flush();
        }
    }

    @Test(enabled = false)
    public void gplim3208fixupEventType() {
        userBean.loginOSUser();
        for (long id : new Long[] {710100L, 710156L}) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent == null || labEvent.getLabEventType() != LabEventType.FINGERPRINTING_ALIQUOT) {
                throw new RuntimeException("cannot find " + id + " or is not FINGERPRINTING_ALIQUOT");
            }
            System.out.println("LabEvent " + id + " type " + labEvent.getLabEventType());
            labEvent.setLabEventType(LabEventType.SHEARING_ALIQUOT);
            System.out.println("   updated to " + labEvent.getLabEventType());
            labEventDao.persist(new FixupCommentary(
                    "GPLIM-3208 incorrect selection at the janus app caused wrong type of aliquot event."));
            // Next time move this flush out of the loop so that both fixups are on one rev and
            // share one FixupCommentary.
            labEventDao.flush();
        }
    }

    // Change lab events' eventLocation changed from BUNSEN to BEAKER.
    @Test(enabled = false)
    public void gplim3248fixupEventType() {
        userBean.loginOSUser();
        for (long id : new Long[] {724636L, 724050L, 724047L, 723648L}) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent == null || !labEvent.getEventLocation().equals("BUNSEN")) {
                throw new RuntimeException("cannot find " + id + " or location not BUNSEN");
            }
            System.out.println("LabEvent " + id + " location " + labEvent.getEventLocation());
            labEvent.setEventLocation("BEAKER");
            System.out.println("   updated to " + labEvent.getEventLocation());
        }
        labEventDao.persist(new FixupCommentary(
                "GPLIM-3248 machine script set location to BUNSEN but it was actually BEAKER."));
        labEventDao.flush();
    }


    /**
     * Unlinks FCT from dilution tube and flowcell, by deleting the denature to dilution event,
     * deleting the dilution to flowcell event, removing the tube from the FCT batch, and deleting
     * the flowcell.
     */
    @Test(enabled = false)
    public void gplim3258fixupDenatureAndFlowcellEvents() throws Exception {
        userBean.loginOSUser();

        // Finds the dilution tube and its position.
        String dilutionTubeBarcode = "0113964182";

        BarcodedTube dilutionTube = barcodedTubeDao.findByBarcode(dilutionTubeBarcode);
        Assert.assertNotNull(dilutionTube);

        Collection<VesselContainer<?>> containers = dilutionTube.getContainers();
        Assert.assertEquals(containers.size(), 1);
        VesselContainer<?> vesselContainer = containers.iterator().next();
        String vesselPositionName = null;
        for (Map.Entry<VesselPosition,? extends LabVessel> mapEntry : vesselContainer.getMapPositionToVessel().entrySet()) {
            if (mapEntry.getValue().getLabel().equals(dilutionTubeBarcode)) {
                Assert.assertNull(vesselPositionName, "Multiple occurrences in tube formation.");
                vesselPositionName = mapEntry.getKey().name();
            }
        }
        Assert.assertNotNull(vesselPositionName);
        Long dilutionTubeFormationId = vesselContainer.getEmbedder().getLabVesselId();
        System.out.println("Using dilution tube " + dilutionTube.getLabVesselId() + " at position " +
                           vesselPositionName + " in tubeFormation " + dilutionTubeFormationId);

        // Finds the cherry pick transfer into the dilution tube.  The DenatureToDilution lab event
        // has multiple cherry picks and all but one must remain.
        // Cannot use hibernate for this because the lab event somehow remains managed by entity manager
        // and consequently cannot delete its cherry pick transfer without Hibernate resurrecting it.
        Query queryCherryPick = labEventDao.getEntityManager().createNativeQuery(
                "select vessel_transfer_id from vessel_transfer " +
                "where target_vessel = :targetVessel and target_position = :targetPosition");
        queryCherryPick.setParameter("targetVessel", dilutionTubeFormationId);
        queryCherryPick.setParameter("targetPosition", vesselPositionName);
        // Fixes the return types.
        queryCherryPick.unwrap(SQLQuery.class).addScalar("vessel_transfer_id", LongType.INSTANCE);
        Long transferToTubeId = (Long)queryCherryPick.getSingleResult();
        Assert.assertNotNull(transferToTubeId);

        // Finds the dilution to flowcell transfer.
        // Cannot use hibernate for this because the lab event somehow remains managed by entity manager
        // and consequently cannot delete its vessel transfer without Hibernate resurrecting it.  That in
        // turn causes the attempted event deletion due to its foreign key constraint in vessel transfer.
        Query queryVesselTransfer = labEventDao.getEntityManager().createNativeQuery(
                "select vessel_transfer_id, target_vessel from vessel_transfer where source_vessel = :dilutionTubeId");
        queryVesselTransfer.setParameter("dilutionTubeId", dilutionTube.getLabVesselId());
        // Fixes the return types.
        queryVesselTransfer.unwrap(SQLQuery.class)
                .addScalar("vessel_transfer_id", LongType.INSTANCE)
                .addScalar("target_vessel", LongType.INSTANCE);
        Long transferFromTubeId = null;
        Long flowcellVesselId = null;
        for (Object[] result : (List<Object[]>)queryVesselTransfer.getResultList()) {
            Assert.assertNull(transferFromTubeId, "Multiple transfers from tube.");
            transferFromTubeId = (Long)result[0];
            flowcellVesselId = (Long)result[1];
        }
        Assert.assertNotNull(transferFromTubeId);

        // Finds the FCT batch starting vessel association and the batch id.
        // Cannot use hibernate for this because the lab batch staring vessel is claimed to be a detached entity
        // regardless of its being immediately gotten from the dao by its id.
        Query queryLbsv = labEventDao.getEntityManager().createNativeQuery(
                "select batch_starting_vessel_id, lab_vessel, lab_batch from batch_starting_vessels " +
                " where dilution_vessel = :dilutionTubeId");
        queryLbsv.setParameter("dilutionTubeId", dilutionTube.getLabVesselId());
        // Fixes the return types.
        queryLbsv.unwrap(SQLQuery.class)
                .addScalar("batch_starting_vessel_id", LongType.INSTANCE)
                .addScalar("lab_vessel", LongType.INSTANCE)
                .addScalar("lab_batch", LongType.INSTANCE);
        Long labBatchStartingVesselId = null;
        Long denatureTubeId = null;
        Long fctBatchId = null;
        for (Object[] result : (List<Object[]>)queryLbsv.getResultList()) {
            Assert.assertNull(labBatchStartingVesselId, "Multiple labBatchStartingVessel.");
            labBatchStartingVesselId = (Long)result[0];
            denatureTubeId = (Long)result[1];
            fctBatchId = (Long)result[2];
        }
        Assert.assertNotNull(labBatchStartingVesselId);

        // Must use a user transaction in order to have all of the fixups in one transaction.
        utx.begin();

        // Unlinks the DenatureToDilution event for the one dilution tube by deleting the transfer to the tube.
        CherryPickTransfer cpt = labEventDao.findById(CherryPickTransfer.class, transferToTubeId);
        Assert.assertNotNull(cpt);
        System.out.println("Deleting DenatureToDilution cherry pick transfer " + cpt.getVesselTransferId());
        //cpt.getLabEvent().getCherryPickTransfers().remove(cpt);
        labEventDao.remove(cpt);

        // Unlinks the DilutionToFlowcell event by deleting transfers from the tube.
        VesselToSectionTransfer vt = labEventDao.findById(VesselToSectionTransfer.class, transferFromTubeId);
        Assert.assertNotNull(vt);
        System.out.println("Deleting DilutionToFlowcell transfer " + vt.getVesselTransferId());
        vt.getLabEvent().getVesselToSectionTransfers().remove(vt);
        Long transferFromEventId = vt.getLabEvent().getLabEventId();
        labEventDao.remove(vt);

        LabEvent transferFromTubeEvent = labEventDao.findById(LabEvent.class, transferFromEventId);
        Assert.assertNotNull(transferFromTubeEvent);
        System.out.println("Deleting DilutionToFlowcell event " + transferFromEventId);
        labEventDao.remove(transferFromTubeEvent);

        // Removes the dilution tube from the FCT batch.  Hibernate removes the denature tube's
        // reference to the FCT batch and the FCT batch reference to the dilution tube.
        LabBatchStartingVessel lbsvDelete = labEventDao.findById(LabBatchStartingVessel.class, labBatchStartingVesselId);
        Assert.assertNotNull(lbsvDelete);
        System.out.println("Removing labBatchStartingVessel " + labBatchStartingVesselId + " from FCT batch " + fctBatchId);
        labEventDao.remove(lbsvDelete);

        // The flowcell is left in place, but a vessel search on it indicates it has no contents.
        // It may be necessary to delete it from the database in order to allow the lab to reuse it.
        System.out.println("Orphan flowcell is " + flowcellVesselId);

        labEventDao.persist(new FixupCommentary(
                "GPLIM-3258 remove two events to unlink tube and flowcell due to error during template creation."));

        utx.commit();
    }

    /** Failed to solve the problem; later discovered that the sample had been through shearing 3 times. */
    @Test(enabled = false)
    public void fixupGplim3279Try1() {
        lcsetOverride(717989L, LabEventType.SHEARING_TRANSFER, "LCSET-6507", "GPLIM-3279");
    }

    /** Failed to solve the problem; later discovered that the sample had been through shearing 3 times. */
    @Test(enabled = false)
    public void fixupGplim3279Try2() {
        lcsetOverride(717910L, LabEventType.SHEARING_ALIQUOT, "LCSET-6507", "GPLIM-3279");
    }

    @Test(enabled = false)
    public void fixupGplim3279Try3() {
        // Zims was returning LCSET-6611, but I think it should have been 6507.
        lcsetOverride(722534L, LabEventType.SHEARING_ALIQUOT, "LCSET-6507", "GPLIM-3279");
    }

    private void lcsetOverride(long labEventId, LabEventType labEventType, String lcsetName, String jiraTicket) {
        userBean.loginOSUser();
        // Override routing for shearing transfer
        LabEvent labEvent = labEventDao.findById(LabEvent.class, labEventId);
        Assert.assertEquals(labEvent.getLabEventType(), labEventType);
        LabBatch labBatch = labBatchDao.findByName(lcsetName);
        labEvent.setManualOverrideLcSet(labBatch);
        labEventDao.persist(new FixupCommentary(
                jiraTicket + " manual override to " + lcsetName + " for " + labEventType));
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupQual676() {
        try {
            userBean.loginOSUser();
            utx.begin();
            long[] ids = {856424L, 856423L};
            for (long id: ids) {
                LabEvent dilutionToFlowcell = labEventDao.findById(LabEvent.class, id);
                Assert.assertEquals(dilutionToFlowcell.getLabEventType(), LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);
                System.out.println("Deleting " + dilutionToFlowcell.getLabEventType() + " " +
                        dilutionToFlowcell.getLabEventId());
                labEventDao.remove(dilutionToFlowcell);
            }
            labEventDao.persist(new FixupCommentary("QUAL-676 delete duplicate events"));
            labEventDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }
}
