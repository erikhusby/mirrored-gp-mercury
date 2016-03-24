package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to LabEvent entities
 */
@Test(groups = TestGroups.FIXUP)
public class LabEventFixupTest extends Arquillian {

    private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("eventType=\"([^\"]*)\"");
    private static final Pattern STATION_PATTERN = Pattern.compile("station=\"([^\"]*)\"");
    private static final Pattern START_PATTERN = Pattern.compile("start=\"([^\"]*)\"");
    private static final Pattern REAGENT_PATTERN = Pattern.compile(
            "reagent barcode=\"([^\"]*)\" kitType=\"([^\"]*)\" expiration=\"([^\"]*)\"");

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
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private GenericReagentDao genericReagentDao;

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

        Collection<VesselContainer<?>> containers = dilutionTube.getVesselContainers();
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
    public void fixupGplim3513() {
        try {
            userBean.loginOSUser();
            utx.begin();
            LabEvent dilutionToFlowcell = labEventDao.findById(LabEvent.class, 850779L);
            Assert.assertEquals(dilutionToFlowcell.getLabEventType(), LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);
            System.out.println("Deleting " + dilutionToFlowcell.getLabEventType() + " " +
                    dilutionToFlowcell.getLabEventId());
            labEventDao.remove(dilutionToFlowcell);
            labEventDao.persist(new FixupCommentary("GPLIM-3513 delete duplicate event"));
            labEventDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupGplim3508() {
        userBean.loginOSUser();
        LabEvent catchCleanup = labEventDao.findById(LabEvent.class, 849904L);
        Assert.assertEquals(catchCleanup.getLabEventType(), LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP);
        LabEvent catchPico1 = labEventDao.findById(LabEvent.class, 849943L);
        Assert.assertEquals(catchPico1.getLabEventType(), LabEventType.CATCH_PICO);
        LabEvent catchPico2 = labEventDao.findById(LabEvent.class, 849942L);
        Assert.assertEquals(catchPico2.getLabEventType(), LabEventType.CATCH_PICO);

        System.out.print("Changing " + catchCleanup.getLabEventType());
        catchCleanup.setLabEventType(LabEventType.POND_REGISTRATION);
        System.out.println(" to " + catchCleanup.getLabEventType());

        System.out.print("Changing " + catchPico1.getLabEventType());
        catchPico1.setLabEventType(LabEventType.POND_PICO);
        System.out.println(" to " + catchPico1.getLabEventType());

        System.out.print("Changing " + catchPico2.getLabEventType());
        catchPico2.setLabEventType(LabEventType.POND_PICO);
        System.out.println(" to " + catchPico2.getLabEventType());

        labEventDao.persist(new FixupCommentary("GPLIM-3508 change event types"));
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
                dilutionToFlowcell.getReagents().clear();
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

    @Test(enabled = false)
    public void fixupSupport815() {
        userBean.loginOSUser();
        // Flip source rack in ShearingTransfer (traversal code doesn't currently honor PlateTransferEventType.isFlipped)
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 896861L);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.SHEARING_TRANSFER);
        SectionTransfer sectionTransfer = labEvent.getSectionTransfers().iterator().next();
        TubeFormation tubeFormation = (TubeFormation) sectionTransfer.getSourceVesselContainer().getEmbedder();
        VesselPosition[] vesselPositions = tubeFormation.getVesselGeometry().getVesselPositions();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (int i = 0; i < vesselPositions.length; i++) {
            mapPositionToTube.put(vesselPositions[95 - i],
                    tubeFormation.getContainerRole().getVesselAtPosition(vesselPositions[i]));
        }
        TubeFormation newTubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        sectionTransfer.setSourceVesselContainer(newTubeFormation.getContainerRole());
        labEventDao.persist(new FixupCommentary("SUPPORT-815 plate flip"));
        labEventDao.flush();
        // Verify 150605_SL-HAA_0467_AC6DNDANXX
    }

    @Test(enabled = false)
    public void fixupGplim3612() {
        userBean.loginOSUser();
        List<Long> labEventIds = Arrays.asList(926421L);
        manualOverride(labEventIds, "LCSET-7421", "GPLIM-3612 fixup ShearingTransfer due to ambiguous LCSET.");
    }

    private void manualOverride(List<Long> labEventIds, String batchName, String reason) {
        List<LabEvent> labEvents = labEventDao.findListByList(LabEvent.class, LabEvent_.labEventId, labEventIds);
        Assert.assertEquals(labEvents.size(), labEventIds.size());
        LabBatch labBatch = labBatchDao.findByName(batchName);
        Assert.assertNotNull(labBatch);
        for (LabEvent labEvent : labEvents) {
            labEvent.setManualOverrideLcSet(labBatch);
            System.out.println("Setting " + labEvent.getLabEventId() + " to " +
                    labEvent.getManualOverrideLcSet().getBatchName());
        }
        labEventDao.persist(new FixupCommentary(reason));
        labEventDao.flush();
    }

    /** Delete Activity Begin and End event sent by a Bravo simulator. */
    @Test(enabled = false)
    public void fixupGplim3568() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        Collection<LabEvent> labEvents = labEventDao.findListByList(LabEvent.class, LabEvent_.eventLocation,
                Collections.singletonList("EPOLK-VM"));
        Assert.assertTrue(CollectionUtils.isNotEmpty(labEvents));
        for (LabEvent labEvent : labEvents) {
            Assert.assertTrue(labEvent.getLabEventType() == LabEventType.ACTIVITY_BEGIN ||
                              labEvent.getLabEventType() == LabEventType.ACTIVITY_END);
            System.out.println("Deleting " + labEvent.getLabEventId());
            labEventDao.remove(labEvent);
        }
        labEventDao.persist(new FixupCommentary("GPLIM-3568 delete activity events sent by a simulator."));
        labEventDao.flush();
        utx.commit();
    }



    @Test(enabled = false)
    public void fixupSwap150() {
        userBean.loginOSUser();
        LabEvent dilutionToFlowcell = labEventDao.findById(LabEvent.class, 887920L);
        Assert.assertEquals(dilutionToFlowcell.getLabEventType(), LabEventType.DILUTION_TO_FLOWCELL_TRANSFER);
        VesselToSectionTransfer vesselToSectionTransfer =
                dilutionToFlowcell.getVesselToSectionTransfers().iterator().next();
        Assert.assertEquals(vesselToSectionTransfer.getSourceVessel().getLabel(), "0177366427");
        System.out.print("Changing " + dilutionToFlowcell.getLabEventId() + " from " +
                         vesselToSectionTransfer.getSourceVessel().getLabel() + " to ");
        vesselToSectionTransfer.setSourceVessel(barcodedTubeDao.findByBarcode("0177366410"));
        System.out.println(vesselToSectionTransfer.getSourceVessel().getLabel());
        labEventDao.persist(new FixupCommentary("SWAP-150 change source of dilution to flowcell transfer"));
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3586() {
        userBean.loginOSUser();

        fixupVesselToVessel(884183L, "SM-74PCZ", "0175568017");
        fixupVesselToVessel(885072L, "SM-74NF3", "0175567592");
        fixupVesselToVessel(884176L, "SM-74PDC", "0175568014");

        labEventDao.persist(new FixupCommentary("GPLIM-3586 fixup extraction transfers"));
        labEventDao.flush();
    }

    private void fixupVesselToVessel(long labEventId, String oldTargetBarcode, String newTargetBarcode) {
        LabEvent labEvent = labEventDao.findById(LabEvent.class, labEventId);
        VesselToVesselTransfer vesselToVesselTransfer = labEvent.getVesselToVesselTransfers().iterator().next();
        Assert.assertEquals(vesselToVesselTransfer.getTargetVessel().getLabel(), oldTargetBarcode);
        System.out.print("In " + labEvent.getLabEventId() + " changing " +
                         vesselToVesselTransfer.getTargetVessel().getLabel() + " to ");
        vesselToVesselTransfer.setTargetVessel(barcodedTubeDao.findByBarcode(newTargetBarcode));
        System.out.println(vesselToVesselTransfer.getTargetVessel().getLabel());
    }

    @Test(enabled = false)
    public void fixupGplim3591() {
        userBean.loginOSUser();

        fixupVesselToVessel(891067L, "SM-74P3F", "0175568242");
        fixupVesselToVessel(891109L, "SM-74NEQ", "0175567599");
        fixupVesselToVessel(891072L, "SM-74P55", "0175568173");
        fixupVesselToVessel(899707L, "SM-74NE2", "0175567623");

        labEventDao.persist(new FixupCommentary("GPLIM-3591 fixup extraction transfers"));
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3591Part2() {
        userBean.loginOSUser();

        fixupVesselToVessel(891071L, "SM-74P42", "0175568216");

        labEventDao.persist(new FixupCommentary("GPLIM-3591 fixup another extraction transfer"));
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupIpi61573() {
        try {
            userBean.loginOSUser();
            utx.begin();
            LabEvent labEvent = labEventDao.findById(LabEvent.class, 909443L);
            Assert.assertEquals(labEvent.getLabEventType(), LabEventType.FLOWCELL_TRANSFER);
            System.out.println("Deleting " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
            labEvent.getReagents().clear();
            labEventDao.remove(labEvent);
            labEventDao.persist(new FixupCommentary("IPI-61573 delete duplicate event"));
            labEventDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupGplim3601() {
        userBean.loginOSUser();
        List<Long> labEventIds = new ArrayList<Long>() {{
            add(916835L);
            add(916836L);
            add(916837L);
        }};
        List<LabEvent> labEvents = labEventDao.findListByList(LabEvent.class, LabEvent_.labEventId, labEventIds);
        Assert.assertEquals(labEvents.size(), 3);
        LabBatch labBatch = labBatchDao.findByName("LCSET-7385");
        Assert.assertNotNull(labBatch);
        for (LabEvent labEvent : labEvents) {
            labEvent.setManualOverrideLcSet(labBatch);
        }
        labEventDao.persist(new FixupCommentary("GPLIM-3601 fixup PicoMicrofluorTransfers due to ambiguous LCSET."));
        labEventDao.flush();
    }


    @Test(enabled = false)
    public void fixupSupport876() {
        try {
            userBean.loginOSUser();
            utx.begin();
            long[] ids = {951437L, 949353L};
            for (long id: ids) {
                LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
                Assert.assertEquals(labEvent.getLabEventType(), LabEventType.PICO_MICROFLUOR_TRANSFER);
                System.out.println("Deleting " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
                labEvent.getReagents().clear();
                labEventDao.remove(labEvent);
            }
            labEventDao.persist(new FixupCommentary("SUPPORT-876 delete incorrect events"));
            labEventDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupSupport1011() {
        try {
            userBean.loginOSUser();
            utx.begin();
            long[] ids = {988769L, 988770L, 988771L, 989205L, 989206L};
            for (long id: ids) {
                LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
                System.out.println("Deleting " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
                labEvent.getReagents().clear();
                labEvent.getSectionTransfers().clear();
                labEvent.getVesselToSectionTransfers().clear();
                labEventDao.remove(labEvent);
            }
            labEventDao.persist(new FixupCommentary("SUPPORT-1011 delete incorrect events due to label swap"));
            labEventDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }

    }

    @Test(enabled = false)
    public void fixupGplim3834() {
        try {
            userBean.loginOSUser();
            utx.begin();

            // Need to find and flip a P7 transfer.
            StaticPlate shearCleanPlate = staticPlateDao.findByBarcode("000006874373");
            StaticPlate indexPlate = null;
            LabEvent adapterLigation = null;
            for (LabEvent labEvent : shearCleanPlate.getTransfersTo()) {
                if (labEvent.getLabEventType() == LabEventType.INDEXED_ADAPTER_LIGATION) {
                    indexPlate = (StaticPlate) labEvent.getSectionTransfers().iterator().next().getSourceVesselContainer().
                            getEmbedder();
                    adapterLigation = labEvent;
                }
            }
            Assert.assertNotNull(indexPlate);
            Assert.assertEquals(indexPlate.getLabel(), "000002933523");

            // Traversal code doesn't currently honor PlateTransferEventType.isFlipped, so replace section transfer with
            // cherry picks.
            List<VesselPosition> wells = SBSSection.ALL96.getWells();
            for (int i = 0; i < 96; i++) {
                adapterLigation.getCherryPickTransfers().add(new CherryPickTransfer(
                        indexPlate.getContainerRole(), wells.get(95 - i), null,
                        shearCleanPlate.getContainerRole(), wells.get(i), null, adapterLigation));
            }

            adapterLigation.getSectionTransfers().clear();
            labEventDao.flush();
            labEventDao.clear();

            LabEvent pondReg = labEventDao.findById(LabEvent.class, 1052911L);
            BarcodedTube pondTube = (BarcodedTube) pondReg.getSectionTransfers().iterator().next().
                    getTargetVesselContainer().getVesselAtPosition(VesselPosition.D04);
            // Currently 0187458266_Illumina_P5-Lorez_P7-Hinij in pipeline API.
            // Changing tagged_357 to tagged_288 = P7-Fofeb.
            Assert.assertEquals(pondTube.getSampleInstancesV2().iterator().next().getMolecularIndexingScheme().getName(),
                    "Illumina_P5-Lorez_P7-Fofeb");
            System.out.println("Flipping " + adapterLigation.getLabEventType() + " " + adapterLigation.getLabEventId());
            labEventDao.persist(new FixupCommentary("GPLIM-3834 delete incorrect events due to label swap"));
            labEventDao.flush();
            utx.commit();
            // Verify 151022_SL-HDJ_0670_AH2MN7ADXX
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupSupport1085() {
        userBean.loginOSUser();
        // Add cherry picks from bait to second row of samples in columns 1 and 3.
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 992394L);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.ICE_1S_TBAIT_PICK);
        CherryPickTransfer cherryPickTransfer = labEvent.getCherryPickTransfers().iterator().next();
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(cherryPickTransfer.getSourceVesselContainer(),
                VesselPosition.A01, cherryPickTransfer.getAncillarySourceVessel(),
                cherryPickTransfer.getTargetVesselContainer(), VesselPosition.B01,
                cherryPickTransfer.getAncillaryTargetVessel(), labEvent));
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(cherryPickTransfer.getSourceVesselContainer(),
                VesselPosition.A03, cherryPickTransfer.getAncillarySourceVessel(),
                cherryPickTransfer.getTargetVesselContainer(), VesselPosition.B03,
                cherryPickTransfer.getAncillaryTargetVessel(), labEvent));
        System.out.println("Added cherry picks to " + labEvent.getLabEventId());

        labEvent = labEventDao.findById(LabEvent.class, 992684L);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.ICE_2ND_BAIT_PICK);
        cherryPickTransfer = labEvent.getCherryPickTransfers().iterator().next();
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(cherryPickTransfer.getSourceVesselContainer(),
                VesselPosition.A01, cherryPickTransfer.getAncillarySourceVessel(),
                cherryPickTransfer.getTargetVesselContainer(), VesselPosition.B01,
                cherryPickTransfer.getAncillaryTargetVessel(), labEvent));
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(cherryPickTransfer.getSourceVesselContainer(),
                VesselPosition.A03, cherryPickTransfer.getAncillarySourceVessel(),
                cherryPickTransfer.getTargetVesselContainer(), VesselPosition.B03,
                cherryPickTransfer.getAncillaryTargetVessel(), labEvent));
        System.out.println("Added cherry picks to " + labEvent.getLabEventId());

        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3788() {
        try {
            userBean.loginOSUser();
            utx.begin();
            long[] ids = {1049478L};
            for (long id: ids) {
                LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
                Assert.assertEquals(labEvent.getLabEventType(), LabEventType.EXTRACT_FFPE_MICRO1_TO_MICRO2);
                System.out.println("Deleting " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
                labEvent.getReagents().clear();
                labEvent.getVesselToVesselTransfers().clear();
                labEventDao.remove(labEvent);
            }
            labEventDao.persist(new FixupCommentary("GPLIM-3788 delete looping event"));
            labEventDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This fixup is not a good example.  The necessary orphanRemoval = true was on a different branch, so the
     * fixup didn't work the first time.  It had to be run several times, with modifications, to have the desired effect.
     */
    @Test(enabled = false)
    public void fixupSupport1296() {
        try {
            userBean.loginOSUser();
            utx.begin();

            // Need to find and flip a P7 transfer.
            StaticPlate shearCleanPlate = staticPlateDao.findByBarcode("000007126773");
            StaticPlate indexPlate = null;
            LabEvent adapterLigation = null;
            for (LabEvent labEvent : shearCleanPlate.getTransfersTo()) {
                if (labEvent == null) {
                    continue;
                }
                if (labEvent.getLabEventType() == LabEventType.INDEXED_ADAPTER_LIGATION) {
                    if (labEvent.getSectionTransfers().isEmpty()) {
                        indexPlate = (StaticPlate) labEvent.getCherryPickTransfers().iterator().next().
                                getSourceVesselContainer().getEmbedder();
                    } else {
                        indexPlate = (StaticPlate) labEvent.getSectionTransfers().iterator().next().
                                getSourceVesselContainer().getEmbedder();
                    }
                    adapterLigation = labEvent;
                    break;
                }
            }
            Assert.assertNotNull(indexPlate);
            Assert.assertEquals(indexPlate.getLabel(), "000001976523");

            // Traversal code doesn't currently honor PlateTransferEventType.isFlipped, so replace section transfer with
            // cherry picks.
            for (CherryPickTransfer cherryPickTransfer : shearCleanPlate.getContainerRole().getCherryPickTransfersTo()) {
                cherryPickTransfer.getSourceVesselContainer().getCherryPickTransfersFrom().clear();
            }
            shearCleanPlate.getContainerRole().getCherryPickTransfersTo().clear();

            for (CherryPickTransfer cherryPickTransfer : adapterLigation.getCherryPickTransfers()) {
                cherryPickTransfer.clearLabEvent();
            }
            adapterLigation.getCherryPickTransfers().clear();
            List<VesselPosition> wells = SBSSection.ALL96.getWells();
            for (int i = 0; i < 96; i++) {
                adapterLigation.getCherryPickTransfers().add(new CherryPickTransfer(
                        indexPlate.getContainerRole(), wells.get(95 - i), null,
                        shearCleanPlate.getContainerRole(), wells.get(i), null, adapterLigation));
            }

            adapterLigation.getSectionTransfers().clear();
            System.out.println("Flipping " + adapterLigation.getLabEventType() + " " + adapterLigation.getLabEventId());
            labEventDao.persist(new FixupCommentary("SUPPORT-1296 flip adapter plate"));
            labEventDao.flush();
            utx.commit();
        } catch (NotSupportedException | SystemException | HeuristicMixedException | HeuristicRollbackException |
                RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupGplim3513Backfill() {
        backfillReagents("DilutionToFlowcellTransfer", "GPLIM-3151 backfill reagents", "20141|201504");
    }

    private void backfillReagents(String eventTypeParam, String reason, String directoryRegex) {
        userBean.loginOSUser();
        try {
            utx.begin();
            SimpleDateFormat xmlDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            Charset charset = Charset.forName("US-ASCII");
            CharsetDecoder decoder = charset.newDecoder();

            // Visit the inbox date directories that match the regex
            String inbox = "\\\\neon\\seq_lims\\mercury\\prod\\bettalims\\inbox";
            Pattern dirPattern = Pattern.compile(directoryRegex);
            DirectoryStream<Path> dateDirs = Files.newDirectoryStream(Paths.get(inbox));
            for (Path path : dateDirs) {
                Matcher dirMatcher = dirPattern.matcher(path.toString());
                if (!dirMatcher.find()) {
                    continue;
                }
                for (File file : path.toFile().listFiles()) {
                    // Open the file and then get a channel from the stream
                    FileInputStream fis = new FileInputStream(file);
                    FileChannel fc = fis.getChannel();

                    // Get the file's size and then map it into memory
                    MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0L, fc.size());

                    // Decode the file into a char buffer
                    CharBuffer charBuffer = decoder.decode(byteBuffer);

                    try {
                        Matcher eventMatcher = EVENT_TYPE_PATTERN.matcher(charBuffer);
                        if (!eventMatcher.find()) {
                            continue;
                        }
                        String eventType = eventMatcher.group(1);
                        if (!eventType.equals(eventTypeParam)) {
                            continue;
                        }

                        // Parse the unique key for the event
                        Matcher matcher = STATION_PATTERN.matcher(charBuffer);
                        if (!matcher.find()) {
                            System.out.println("Failed to find station in " + file.getName());
                            continue;
                        }
                        String station = matcher.group(1);
                        matcher = START_PATTERN.matcher(charBuffer);
                        if (!matcher.find()) {
                            System.out.println("Failed to find start date in " + file.getName());
                            continue;
                        }
                        String startDateString = matcher.group(1);
                        Date startDate = xmlDateFormat.parse(startDateString);

                        // Parse the reagents
                        List<GenericReagent> genericReagents = new ArrayList<>();
                        matcher = REAGENT_PATTERN.matcher(charBuffer);
                        int i = 1;
                        while(matcher.find()) {
                            String lot = matcher.group(1);
                            String reagentName = matcher.group(2);
                            if (reagentName.equals("Universal Sequencing Buffer")) {
                                reagentName += " " + i;
                                i++;
                            }
                            String expirationString = matcher.group(3);
                            if (expirationString.startsWith("12015") || expirationString.startsWith("42015")) {
                                expirationString = expirationString.substring(1);
                            } else if (expirationString.startsWith("302015")) {
                                expirationString = expirationString.substring(2);
                            }
                            Date expiration = xmlDateFormat.parse(expirationString);
                            genericReagents.add(new GenericReagent(reagentName, lot, expiration));
                        }

                        // Fetch the event and add the reagents
                        if (!genericReagents.isEmpty()) {
                            LabEvent labEvent = labEventDao.findByLocationDateDisambiguator(station, startDate, 1L);
                            if (labEvent == null) {
                                System.out.println("Failed to find database event in " + file.getName());
                                continue;
                            }
                            if (!labEvent.getReagents().isEmpty()) {
                                System.out.println("Reagents already persisted in " + file.getName());
                                continue;
                            }
                            System.out.println("Adding reagents to " + station + " " + startDate);
                            for (GenericReagent genericReagent : genericReagents) {
                                // Fetching each reagent individually like this is not ideal for performance,
                                // but the slowness of reading the files is more of a bottleneck.
                                GenericReagent dbGenericReagent = genericReagentDao.findByReagentNameLotExpiration(
                                        genericReagent.getName(), genericReagent.getLot(),
                                        genericReagent.getExpiration());
                                if (dbGenericReagent != null) {
                                    genericReagent = dbGenericReagent;
                                }
                                labEvent.addReagent(genericReagent);
                            }
                            labEventDao.flush();
                            labEventDao.clear();
                        }
                    } finally {
                        fc.close();
                    }
                }
            }
            labEventDao.persist(new FixupCommentary(reason));
            labEventDao.flush();
            utx.commit();
        } catch (IOException | ParseException | HeuristicRollbackException | HeuristicMixedException | SystemException |
                NotSupportedException |RollbackException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void gplim3796FixEventDisambiguator() {
        userBean.loginOSUser();
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1052890L);
        labEvent.setDisambiguator(1L);
        labEventDao.persist(new FixupCommentary(
                "GPLIM-3796 changed disambiguator of an event to avoid a unique constraint violation with another receipt event"));
    }

    @Test(enabled = false)
    public void fixupGplim3805() {
        userBean.loginOSUser();
        LabEvent daughterPlateTransfer = labEventDao.findById(LabEvent.class, 1056208L);
        Assert.assertNotNull(daughterPlateTransfer);
        Assert.assertTrue(daughterPlateTransfer.getCherryPickTransfers().size() > 0);
        System.out.print("Removing lab event " + daughterPlateTransfer.getLabEventId() + " and its cherry picks");
        for (CherryPickTransfer transfer : daughterPlateTransfer.getCherryPickTransfers()) {
            transfer.clearLabEvent();
            labEventDao.remove(transfer);
        }
        daughterPlateTransfer.getCherryPickTransfers().clear();
        labEventDao.remove(daughterPlateTransfer);

        labEventDao.persist(new FixupCommentary("GPLIM-3796 undo an earlier fixup that created a daughter plate."));
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim3805a() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        TubeFormation source = labEventDao.findById(TubeFormation.class, 2342838L);
        List<CherryPickTransfer> transfers = new ArrayList<>(source.getContainerRole().getCherryPickTransfersFrom());
        Assert.assertEquals(transfers.size(), 74);

        System.out.println("Removing " + transfers.size() + " cherry picks from tube formation " +
                           source.getLabVesselId());

        for (CherryPickTransfer transfer : transfers) {
            transfer.getAncillaryTargetVessel().getVesselToVesselTransfersThisAsTarget().remove(transfer);
            transfer.getAncillarySourceVessel().getVesselToVesselTransfersThisAsSource().remove(transfer);
            transfer.getTargetVesselContainer().getCherryPickTransfersTo().remove(transfer);
            transfer.getSourceVesselContainer().getCherryPickTransfersFrom().remove(transfer);
            labEventDao.remove(transfer);
        }

        labEventDao.flush();
        labEventDao.persist(
                new FixupCommentary("GPLIM-3796 more undo of an earlier fixup that created a daughter plate."));
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim3845() throws Exception {
        // Delete cherry pick.
        userBean.loginOSUser();
        utx.begin();
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1076569L);
        CherryPickTransfer transfer = labEventDao.findById(CherryPickTransfer.class, 164987L);

        labEvent.getCherryPickTransfers().clear();

        transfer.getAncillaryTargetVessel().getVesselToVesselTransfersThisAsTarget().remove(transfer);
        transfer.getAncillarySourceVessel().getVesselToVesselTransfersThisAsSource().remove(transfer);
        transfer.getTargetVesselContainer().getCherryPickTransfersTo().remove(transfer);
        transfer.getSourceVesselContainer().getCherryPickTransfersFrom().remove(transfer);
        labEventDao.remove(transfer);

        labEventDao.remove(labEvent);
        labEventDao.persist(new FixupCommentary("GPLIM-3845 delete cherry pick event"));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport1369() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        // Finds vessel transfers having source section ALL384 when the source is a Eppendorf96 plate.
        Query queryVesselTransfer = labEventDao.getEntityManager().createNativeQuery(
                "select vessel_transfer_id from vessel_transfer vt, lab_vessel src " +
                "where vt.source_vessel = src.lab_vessel_id " +
                "and src.plate_type = 'Eppendorf96' and source_section = 'ALL384'");
        queryVesselTransfer.unwrap(SQLQuery.class).addScalar("vessel_transfer_id", LongType.INSTANCE);
        List<Long> vesselTransferIds = queryVesselTransfer.getResultList();
        Assert.assertTrue(CollectionUtils.isNotEmpty(vesselTransferIds));

        // Updates the source section to ALL96.
        List<SectionTransfer> transfers = labEventDao.findListByList(SectionTransfer.class,
                SectionTransfer_.vesselTransferId, vesselTransferIds);
        Assert.assertEquals(transfers.size(), vesselTransferIds.size());
        for (SectionTransfer transfer : transfers) {
            Assert.assertEquals(transfer.getSourceSection(), SBSSection.ALL384);
            transfer.setSourceSection(SBSSection.ALL96);
        }
        System.out.println("Changed ALL384 to ALL96 for vessel transfers " + StringUtils.join(vesselTransferIds, ", "));

        labEventDao.persist(new FixupCommentary("SUPPORT-1369 fixup Pico Microfluor transfers from Eppendorf96"));
        labEventDao.flush();
        utx.commit();
    }
    /**
     * ANXX flowcells are 2500s with 8 lanes, not 2 lanes.
     */
    @Test(enabled = false)
    public void fixupGplim3932() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        List<IlluminaFlowcell> flowcells = illuminaFlowcellDao.findLikeBarcode("%ANXX");
        for (IlluminaFlowcell flowcell : flowcells) {
            for (LabEvent labEvent : flowcell.getTransfersTo()) {
                if (labEvent.getLabEventType() == LabEventType.FLOWCELL_TRANSFER) {
                    for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                        if (sectionTransfer.getTargetSection() == SBSSection.ALL2) {
                            sectionTransfer.setTargetSection(SBSSection.FLOWCELL8);
                            flowcell.setFlowcellType(IlluminaFlowcell.FlowcellType.HiSeqFlowcell);
                            System.out.println("Changing section in transfer to " + flowcell.getLabel());
                        }
                    }
                }
            }
        }
        illuminaFlowcellDao.persist(new FixupCommentary("GPLIM-3932 change 2500 ANXX flowcells to 8 lanes"));
        illuminaFlowcellDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim3967() throws Exception {
        // Deletes duplicate thermocycler event id=1170398.
        userBean.loginOSUser();
        utx.begin();
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1170398L);
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getReagents()));
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getLabEventReagents()));
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getCherryPickTransfers()));
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getSectionTransfers()));
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getVesselToSectionTransfers()));
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getVesselToVesselTransfers()));
        System.out.println("Deleting " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
        labEventDao.remove(labEvent);
        labEventDao.persist(new FixupCommentary("GPLIM-3967 delete thermocycler event"));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport1602() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        long[] ids = {1242543L, 1242544L, 1242545L, 1242546L, 1242547L};
        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            System.out.println("Deleting lab event " + labEvent.getLabEventId());
            labEventDao.remove(labEvent);
        }

        labEventDao.persist(new FixupCommentary("SUPPORT-1602 delete Infinium events"));
        labEventDao.flush();
        utx.commit();
    }
}
