package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunFinder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchFixUpTest;
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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
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
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVesselFixupTest.WHITESPACE_PATTERN;

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

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

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
    public void fixupPo743() {
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
        for (long id : new Long[]{687513L, 687557L}) {
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
        for (long id : new Long[]{710100L, 710156L}) {
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
        for (long id : new Long[]{724636L, 724050L, 724047L, 723648L}) {
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
        for (Map.Entry<VesselPosition, ? extends LabVessel> mapEntry : vesselContainer.getMapPositionToVessel()
                .entrySet()) {
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
        Long transferToTubeId = (Long) queryCherryPick.getSingleResult();
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
        for (Object[] result : (List<Object[]>) queryVesselTransfer.getResultList()) {
            Assert.assertNull(transferFromTubeId, "Multiple transfers from tube.");
            transferFromTubeId = (Long) result[0];
            flowcellVesselId = (Long) result[1];
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
        for (Object[] result : (List<Object[]>) queryLbsv.getResultList()) {
            Assert.assertNull(labBatchStartingVesselId, "Multiple labBatchStartingVessel.");
            labBatchStartingVesselId = (Long) result[0];
            denatureTubeId = (Long) result[1];
            fctBatchId = (Long) result[2];
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
        LabBatchStartingVessel lbsvDelete =
                labEventDao.findById(LabBatchStartingVessel.class, labBatchStartingVesselId);
        Assert.assertNotNull(lbsvDelete);
        System.out.println(
                "Removing labBatchStartingVessel " + labBatchStartingVesselId + " from FCT batch " + fctBatchId);
        labEventDao.remove(lbsvDelete);

        // The flowcell is left in place, but a vessel search on it indicates it has no contents.
        // It may be necessary to delete it from the database in order to allow the lab to reuse it.
        System.out.println("Orphan flowcell is " + flowcellVesselId);

        labEventDao.persist(new FixupCommentary(
                "GPLIM-3258 remove two events to unlink tube and flowcell due to error during template creation."));

        utx.commit();
    }

    /**
     * Failed to solve the problem; later discovered that the sample had been through shearing 3 times.
     */
    @Test(enabled = false)
    public void fixupGplim3279Try1() {
        lcsetOverride(717989L, LabEventType.SHEARING_TRANSFER, "LCSET-6507", "GPLIM-3279");
    }

    /**
     * Failed to solve the problem; later discovered that the sample had been through shearing 3 times.
     */
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
            for (long id : ids) {
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

    /**
     * Delete Activity Begin and End event sent by a Bravo simulator.
     */
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
            for (long id : ids) {
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
            for (long id : ids) {
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
                    indexPlate =
                            (StaticPlate) labEvent.getSectionTransfers().iterator().next().getSourceVesselContainer().
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
            Assert.assertEquals(
                    pondTube.getSampleInstancesV2().iterator().next().getMolecularIndexingScheme().getName(),
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
            for (long id : ids) {
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
            for (CherryPickTransfer cherryPickTransfer : shearCleanPlate.getContainerRole()
                    .getCherryPickTransfersTo()) {
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
                        while (matcher.find()) {
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
                NotSupportedException | RollbackException e) {
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
    public void fixupGplim4019() throws Exception {
        // Deletes wrong striptube to flowcell event id=1201592.
        userBean.loginOSUser();
        utx.begin();
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1201592L);
        System.out.println("Deleting " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
        labEventDao.remove(labEvent);
        labEventDao.persist(new FixupCommentary("GPLIM-4019 delete wrong flowcell association event"));
        labEventDao.flush();
        utx.commit();
    }

    /**
     * Change source in PicoMicrofluorTransfers, a white plate was used twice.
     */
    @Test(enabled = false)
    public void fixupSupport1531() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        // Currently 000009496969 -> 000002543920
        // Change to 000009497369 -> 000002543920
        long[] eventIds = {1202826L, 1202827L, 1202828L};
        StaticPlate staticPlate = staticPlateDao.findByBarcode("000009497369");
        for (long eventId : eventIds) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, eventId);
            Assert.assertEquals(labEvent.getLabEventType(), LabEventType.PICO_MICROFLUOR_TRANSFER);

            SectionTransfer sectionTransfer = labEvent.getSectionTransfers().iterator().next();
            String sourceLabel = sectionTransfer.getSourceVesselContainer().getEmbedder().getLabel();
            Assert.assertEquals(sourceLabel, "000009496969");
            Assert.assertEquals(sectionTransfer.getTargetVesselContainer().getEmbedder().getLabel(), "000002543920");

            sectionTransfer.setSourceVesselContainer(staticPlate.getContainerRole());
            System.out.println("In " + labEvent.getLabEventId() + ", changing " + sourceLabel + " to " +
                               staticPlate.getLabel());
        }
        labEventDao.persist(new FixupCommentary("SUPPORT-1531 change source of PicoMicrofluorTransfer"));
        labEventDao.flush();
        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/DeleteLabEvents.txt, so it can
     * be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-4104
     * InfiniumHybridization
     * 1278705
     * 1278706
     * 1278707
     */
    @Test(enabled = false)
    public void fixupGplim4104() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("DeleteLabEvents.txt"));
        String jiraTicket = lines.get(0);
        String eventType = lines.get(1);

        for (String id : lines.subList(2, lines.size())) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, Long.parseLong(id));
            Assert.assertEquals(labEvent.getLabEventType().getName(), eventType);
            System.out.println("Deleting lab event " + labEvent.getLabEventId());
            labEventDao.remove(labEvent);
        }

        labEventDao.persist(new FixupCommentary(jiraTicket + " delete " + eventType));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4046() throws Exception {
        // Deletes shearing transfer event in order to resend a corrected bettalims message with an added rework tube.
        userBean.loginOSUser();
        utx.begin();
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1222203L);
        for (LabEventReagent labEventReagent : labEvent.getLabEventReagents()) {
            System.out.println("Removing labEventReagent " + labEventReagent.getLabEvent().getLabEventId() +
                               ", " + labEventReagent.getReagent().getName());
            labEventDao.remove(labEventReagent);
        }
        labEvent.getLabEventReagents().clear();
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getCherryPickTransfers()));
        for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
            System.out.println("Removing sectionTransfer " + sectionTransfer.getVesselTransferId());
            labEventDao.remove(sectionTransfer);
        }
        labEvent.getSectionTransfers().clear();
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getVesselToSectionTransfers()));
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getVesselToVesselTransfers()));
        System.out.println("Removing " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
        labEventDao.remove(labEvent);
        labEventDao.persist(new FixupCommentary("GPLIM-4046 delete shearing transfer event."));
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

    /**
     * Delete malformed DenatureTransfer
     */
    @Test(enabled = false)
    public void fixupSupport1661() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        long[] ids = {1268180L};
        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            System.out.println("Deleting lab event " + labEvent.getLabEventId());
            labEventDao.remove(labEvent);
        }

        labEventDao.persist(new FixupCommentary("SUPPORT-1661 delete Denature transfer"));
        labEventDao.flush();
        BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode("0193780863");
        Assert.assertEquals(barcodedTube.getSampleInstancesV2().size(), 5);
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4196() {
        userBean.loginOSUser();

        fixupVesselToVessel(1380583L, "SM-AZRN2", "1124988659");
        fixupVesselToVessel(1380582L, "SM-AZRN3", "1124988660");
        fixupVesselToVessel(1385966L, "SM-AZRNE", "1124988672");
        fixupVesselToVessel(1385968L, "SM-AZRNQ", "1124988683");

        labEventDao.persist(new FixupCommentary("GPLIM-4196 fixup extraction transfers"));
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupGplim4203() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        long[] ids = {1397778L, 1397779L};
        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            Assert.assertEquals(labEvent.getLabEventType(), LabEventType.POND_PICO);
            System.out.println("LabEvent " + id + " type " + labEvent.getLabEventType());
            labEvent.setLabEventType(LabEventType.CATCH_PICO);
            System.out.println("   updated to " + labEvent.getLabEventType());
        }

        labEventDao.persist(new FixupCommentary("GPLIM-4203 changing lab event type to catch pico"));
        labEventDao.flush();
        utx.commit();
    }

    /**
     * Fix InfiniumHybridization transfers, columns 6 and 7.
     */
    @Test(enabled = false)
    public void fixupSupport1908() throws Exception {
        /*
        Used this GAP query to map exported plate names to amp plate barcodes:
        SELECT
            m.plate_name,
            sl.sample_list_name
        FROM
            esp.masterplate m
            INNER JOIN esp.sample_list sl
                ON   sl.plate_id = m.plate_id
        WHERE
            m.plate_name IN ('CVB_PTSD_BIZU_Mega_01', 'CVB_PTSD_BIZU_Mega_02',
                            'CVB_PTSD_BIZU_Mega_06', 'CVB_PTSD_BIZU_Mega_07',
                            'CVB_PTSD_BIZU_Mega_08', 'CVB_PTSD_BIZU_Mega_09',
                            'CVB_PTSD_BIZU_Mega_10', 'CVB_PTSD_BIZU_Mega_11',
                            'CVB_PTSD_BIZU_Mega_12', 'CVB_PTSD_BIZU_Mega_13',
                            'CVB_PTSD_BIZU_Mega_14', 'CVB_PTSD_BIZU_Mega_15',
                            'CVB_PTSD_BIZU_Mega_16', 'CVB_PTSD_BIZU_Mega_17',
                            'CVB_PTSD_BIZU_Mega_18', 'CVB_PTSD_BIZU_Mega_19',
                            'CVB_PTSD_BIZU_Mega_20', 'CVB_PTSD_BIZU_Mega_21',
                            'CVB_PTSD_BIZU_Mega_22', 'CVB_PTSD_BIZU_Mega_23',
                            'CVB_PTSD_BIZU_Mega_24', 'CVB_PTSD_BIZU_Mega_25',
                            'CVB_PTSD_BIZU_Mega_26', 'CVB_PTSD_BIZU_Mega_27',
                            'CVB_PTSD_BIZU_Mega_28', 'CVB_PTSD_BIZU_Mega_29',
                            'CVB_PTSD_BIZU_Mega_30', 'CVB_PTSD_BIZU_Mega_31',
                            'CVB_PTSD_BIZU_Mega_32', 'CVB_PTSD_BIZU_Mega_33',
                            'CVB_PTSD_BIZU_Mega_34', 'CVB_PTSD_BIZU_Mega_35',
                            'CVB_PTSD_BIZU_Mega_36')
        ORDER BY
            m.plate_name;
         */
        userBean.loginOSUser();
        utx.begin();

        String[] plateBarcodes = {
                "000016838609",
                "000016844109",
                "000016841609",
                "000016841509",
                "000016837709",
                "000016851109",
                "000016838109",
                "000016850309",
                "000016840109",
                "000016851009",
                "000016852209",
                "000016851309",
                "000016844209",
                "000016851609",
                "000016849309",
                "000016844309",
                "000016839809",
                "000016849009",
                "000016849509",
                "000016844709",
                "000016841709",
                "000016842209",
                "000016839709",
                "000016849109",
                "000016845209",
                "000016860809",
                "000016863809",
                "000016861809",
                "000016849409",
                "000016849809",
                "000016839609",
                "000016842609",
                "000016839409"
        };
        Map<VesselPosition, VesselPosition> mapSourceToDest = new HashMap<VesselPosition, VesselPosition>() {{
            put(VesselPosition.B06, VesselPosition.R02C01);
            put(VesselPosition.C06, VesselPosition.R03C01);
            put(VesselPosition.D06, VesselPosition.R04C01);
            put(VesselPosition.E06, VesselPosition.R05C01);
            put(VesselPosition.F06, VesselPosition.R06C01);
            put(VesselPosition.G06, VesselPosition.R07C01);
            put(VesselPosition.B07, VesselPosition.R02C01);
            put(VesselPosition.C07, VesselPosition.R03C01);
            put(VesselPosition.D07, VesselPosition.R04C01);
            put(VesselPosition.E07, VesselPosition.R05C01);
            put(VesselPosition.F07, VesselPosition.R06C01);
            put(VesselPosition.G07, VesselPosition.R07C01);
        }};
        for (String plateBarcode : plateBarcodes) {
            StaticPlate staticPlate = staticPlateDao.findByBarcode(plateBarcode);
            for (CherryPickTransfer cherryPickTransfer : staticPlate.getContainerRole().getCherryPickTransfersFrom()) {
                VesselPosition vesselPosition = mapSourceToDest.get(cherryPickTransfer.getSourcePosition());
                if (vesselPosition != null) {
                    System.out.println("For " + staticPlate.getLabel() + " changing transfer to " +
                                       cherryPickTransfer.getTargetVesselContainer().getEmbedder().getLabel() + " " +
                                       cherryPickTransfer.getTargetPosition() + " to " + vesselPosition);
                    cherryPickTransfer.setTargetPosition(vesselPosition);
                }
            }
        }

        labEventDao.persist(new FixupCommentary("SUPPORT-1908 fix Infinium transfers"));
        labEventDao.flush();
        utx.commit();
    }

    /**
     * Fixup Pico transfers with source and destination sections of different sizes.
     */
    @Test(enabled = false)
    public void fixupGplim4250() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<SectionTransfer> sectionTransfers = labEventDao.findAll(SectionTransfer.class,
                new GenericDao.GenericDaoCallback<SectionTransfer>() {
                    @Override
                    public void callback(CriteriaQuery<SectionTransfer> criteriaQuery, Root<SectionTransfer> root) {
                        CriteriaBuilder criteriaBuilder = labEventDao.getEntityManager().getCriteriaBuilder();
                        criteriaQuery.where(
                                criteriaBuilder.equal(root.get(SectionTransfer_.sourceSection), SBSSection.ALL384),
                                criteriaBuilder.notEqual(root.get(SectionTransfer_.targetSection), SBSSection.ALL384));
                    }
                });
        for (SectionTransfer sectionTransfer : sectionTransfers) {
            Assert.assertEquals(sectionTransfer.getLabEvent().getLabEventType(), LabEventType.PICO_MICROFLUOR_TRANSFER);
            System.out.println("In " + sectionTransfer.getLabEvent().getLabEventId() + " changing source " +
                               sectionTransfer.getSourceSection() + " to " + sectionTransfer.getTargetSection());
            sectionTransfer.setSourceSection(sectionTransfer.getTargetSection());
        }

        labEventDao.persist(new FixupCommentary("GPLIM-4250 fix pico sections"));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim4295fixupEventType() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        long wrongEventId = 1478962L;
        LabEvent labEvent = labEventDao.findById(LabEvent.class, wrongEventId);
        if (labEvent == null || labEvent.getLabEventType() != LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP) {
            throw new RuntimeException("cannot find " + wrongEventId + " or is not ICE_CATCH_ENRICHMENT_CLEANUP");
        }
        System.out.println("LabEvent " + wrongEventId + " type " + labEvent.getLabEventType());
        labEvent.setLabEventType(LabEventType.POND_REGISTRATION);
        System.out.println("   updated to " + labEvent.getLabEventType());
        labEventDao.persist(new FixupCommentary(
                "GPLIM-4295 incorrect protocol chosen caused wrong type of lab event."));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim4430() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        long wrongEventId = 1663671L;
        LabEvent labEvent = labEventDao.findById(LabEvent.class, wrongEventId);
        if (labEvent == null || labEvent.getLabEventType() != LabEventType.ICE_CATCH_ENRICHMENT_CLEANUP) {
            throw new RuntimeException("cannot find " + wrongEventId + " or is not ICE_CATCH_ENRICHMENT_CLEANUP");
        }
        System.out.println("LabEvent " + wrongEventId + " type " + labEvent.getLabEventType());
        labEvent.setLabEventType(LabEventType.POND_REGISTRATION);
        System.out.println("   updated to " + labEvent.getLabEventType());
        labEventDao.persist(new FixupCommentary(
                "GPLIM-4430 incorrect protocol chosen caused wrong type of lab event."));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4302() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        List<LabVessel> infiniumChips = labVesselDao.findAllWithEventButMissingAnother(LabEventType.INFINIUM_XSTAIN,
                LabEventType.INFINIUM_AUTOCALL_ALL_STARTED);
        BspUser bspUser = userBean.getBspUser();
        long disambiguator = 1L;
        for (LabVessel labVessel : infiniumChips) {
            Date start = new Date();
            long operator = bspUser.getUserId();
            LabEvent labEvent = new LabEvent(LabEventType.INFINIUM_AUTOCALL_ALL_STARTED, start,
                    LabEvent.UI_PROGRAM_NAME, disambiguator, operator, LabEvent.UI_PROGRAM_NAME);
            labVessel.addInPlaceEvent(labEvent);
            disambiguator++;
            System.out.println(
                    "Adding InfiniumAutoCallAllStarted event as an in place lab event to chip " + labVessel.getLabel());
        }

        FixupCommentary fixupCommentary = new FixupCommentary(
                "GPLIM-4302 - complete old infinium chips by adding InfiniumAutoCallAllStarted event");
        labEventDao.persist(fixupCommentary);
        labEventDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport2319() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        long[] ids =
                {1732578L, 1732576L, 1732580L, 1732599L, 1732608L, 1732611L, 1732616L, 1732622L, 1732627L, 1732684L,
                        1732686L, 1732688L, 1732715L, 1732759L};

        Reagent undesired = reagentDao.findByReagentNameLotExpiration("HS buffer", "RG-10095", null);
        Reagent desired = reagentDao.findByReagentNameLotExpiration("HS buffer", "RG-12126", null);
        Assert.assertNotNull(undesired);
        Assert.assertNotNull(desired);

        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent == null || labEvent.getLabEventType() != LabEventType.PICO_DILUTION_TRANSFER) {
                throw new RuntimeException("cannot find " + id + " or is not PICO_DILUTION_TRANSFER");
            }
            for (LabEventReagent labEventReagent : labEvent.getLabEventReagents()) {
                if (labEventReagent.getReagent().equals(undesired)) {
                    System.out.println("Removing " + undesired.getName() + " on event " + labEvent.getLabEventId());
                    labEvent.getLabEventReagents().remove(labEventReagent);
                    genericReagentDao.remove(labEventReagent);
                }
            }
            System.out.println("Adding " + desired.getName() + " on event " + labEvent.getLabEventId());
            labEvent.addReagent(desired);
        }

        FixupCommentary fixupCommentary = new FixupCommentary(
                "SUPPORT-2319 - Removing expired reagent for new one");
        labEventDao.persist(fixupCommentary);
        labEventDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport2330() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        long[] ids = {1740607L};

        Reagent undesired = reagentDao.findByReagentNameLotExpiration("HS buffer", "RG-8262", null);
        Reagent desired = reagentDao.findByReagentNameLotExpiration("HS buffer", "RG-3111", null);
        Assert.assertNotNull(undesired);

        if (desired == null) {
            desired = new GenericReagent("HS buffer", "RG-3111", null);
        }

        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent == null || labEvent.getLabEventType() != LabEventType.RIBO_DILUTION_TRANSFER) {
                throw new RuntimeException("cannot find " + id + " or is not RiboDilutionTransfer");
            }
            for (LabEventReagent labEventReagent : labEvent.getLabEventReagents()) {
                if (labEventReagent.getReagent().equals(undesired)) {
                    System.out.println("Removing " + undesired.getName() + " on event " + labEvent.getLabEventId());
                    labEvent.getLabEventReagents().remove(labEventReagent);
                    genericReagentDao.remove(labEventReagent);
                }
            }
            System.out.println("Adding " + desired.getName() + " on event " + labEvent.getLabEventId());
            labEvent.addReagent(desired);
        }

        FixupCommentary fixupCommentary = new FixupCommentary(
                "SUPPORT-2330 - Removing expired reagent for new one");
        labEventDao.persist(fixupCommentary);
        labEventDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4508() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        long[] ids = {1727870L, 1727867L, 1727881L};
        for (long wrongEventId : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, wrongEventId);
            if (labEvent == null || labEvent.getLabEventType() != LabEventType.EXTRACT_BLOOD_MICRO_TO_SPIN) {
                throw new RuntimeException("cannot find " + wrongEventId + " or is not EXTRACT_BLOOD_MICRO_TO_SPIN");
            }
            System.out.println("LabEvent " + wrongEventId + " type " + labEvent.getLabEventType());
            labEvent.setLabEventType(LabEventType.EXTRACT_FRESH_TISSUE_MICRO_TO_SPIN);
            System.out.println("   updated to " + labEvent.getLabEventType());
        }

        labEventDao.persist(new FixupCommentary("GPLIM-4508 change event type to ExtractFreshTissueMicroToSpin"));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim4422() {
        userBean.loginOSUser();

        fixupVesselToVessel(1644232L, "SM-CEMB5", "1125710886");

        labEventDao.persist(new FixupCommentary("GPLIM-4422 fixup extraction transfer"));
        labEventDao.flush();
    }

    @Test(enabled = false)
    public void fixupSupport2485() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        long[] ids = {1831499L, 1831501L};
        for (long labEventId : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, labEventId);
            if (labEvent == null || labEvent.getLabEventType() != LabEventType.DILUTION_TO_FLOWCELL_TRANSFER) {
                throw new RuntimeException("cannot find " + labEventId + " or is not DILUTION_TO_FLOWCELL_TRANSFER");
            } else if (!labEvent.getEventLocation().equals("SL-HDE")) {
                throw new RuntimeException("Did not find expected station of SL-HDE");
            } else {
                System.out.println("LabEvent " + labEventId + " station " + labEvent.getEventLocation());
                labEvent.setEventLocation("SL-HDD");
                System.out.println("   updated to " + labEvent.getEventLocation());
            }
        }
        labEventDao.persist(new FixupCommentary("SUPPORT-2485 change lab event location to SL-HDD"));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim4796BackfillOnPremPdosToHaveAllStartedEvent() throws Exception {
        List<LabVessel> infiniumChips = labVesselDao.findAllWithEventButMissingAnother(LabEventType.INFINIUM_XSTAIN,
                LabEventType.INFINIUM_AUTOCALL_ALL_STARTED);
        InfiniumRunFinder runFinder = new InfiniumRunFinder();
        BspUser bspUser = bspUserList.getByUsername("seqsystem");
        List<LabVessel> invalidChips = new ArrayList<>();
        for (LabVessel labVessel : infiniumChips) {
            StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
            boolean invalidPipelineLocation = runFinder.checkForInvalidPipelineLocation(staticPlate);
            if (invalidPipelineLocation) {
                invalidChips.add(labVessel);
            }
        }
        userBean.loginOSUser();
        utx.begin();
        long disambiguator = 1L;
        for (LabVessel labVessel : invalidChips) {
            Date start = new Date();
            long operator = bspUser.getUserId();
            LabEvent labEvent =
                    new LabEvent(LabEventType.INFINIUM_AUTOCALL_ALL_STARTED, start, LabEvent.UI_PROGRAM_NAME,
                            disambiguator, operator, LabEvent.UI_PROGRAM_NAME);
            labVessel.addInPlaceEvent(labEvent);
            System.out.println(
                    "Adding InfiniumAutoCallAllStarted event as an in place lab event to chip " + labVessel.getLabel());
            disambiguator++;
        }
        labEventDao.persist(new FixupCommentary("GPLIM-4796 add started event to all chips marked on prem if missing"));
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport2691() throws Exception {
        // Deletes shearing transfer in order to resend a corrected bettalims message with a tube in a new position.
        userBean.loginOSUser();
        utx.begin();
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1934498L);
        Assert.assertEquals(labEvent.getLabEventType(), LabEventType.SHEARING_TRANSFER);
        for (LabEventReagent labEventReagent : labEvent.getLabEventReagents()) {
            System.out.println("Removing labEventReagent " + labEventReagent.getLabEvent().getLabEventId() +
                               ", " + labEventReagent.getReagent().getName());
            labEventDao.remove(labEventReagent);
        }
        labEvent.getLabEventReagents().clear();
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getCherryPickTransfers()));
        for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
            System.out.println("Removing sectionTransfer " + sectionTransfer.getVesselTransferId());
            labEventDao.remove(sectionTransfer);
        }
        labEvent.getSectionTransfers().clear();
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getVesselToSectionTransfers()));
        Assert.assertTrue(CollectionUtils.isEmpty(labEvent.getVesselToVesselTransfers()));
        System.out.println("Removing " + labEvent.getLabEventType() + " " + labEvent.getLabEventId());
        labEventDao.remove(labEvent);
        labEventDao.persist(new FixupCommentary("SUPPORT-2691 delete shearing transfer event."));
        labEventDao.flush();
        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/ManualOverrideLabEvents.txt, so it can
     * be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-4104
     * LCSET-10868
     * 1980836
     */
    @Test(enabled = false)
    public void fixupGplim4798() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ManualOverrideLabEvents.txt"));
        String jiraTicket = lines.get(0);
        String batchId = lines.get(1);
        LabBatch labBatch = labBatchDao.findByName(batchId);
        if (labBatch == null) {
            throw new RuntimeException("Batch not found: " + batchId);
        }
        for (String id : lines.subList(2, lines.size())) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, Long.parseLong(id));
            labEvent.setManualOverrideLcSet(labBatch);
            System.out.println("Lab event " + labEvent.getLabEventId() + " manual override to " + batchId);
            TransferTraverserCriteria transferTraverserCriteria = new LabBatchFixUpTest.ComputeLabBatchTtc(true);
            for (LabVessel targetLabVessel : labEvent.getTargetLabVessels()) {
                VesselContainer<?> containerRole = targetLabVessel.getContainerRole();
                if (containerRole == null) {
                    targetLabVessel.evaluateCriteria(transferTraverserCriteria,
                            TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    for (VesselPosition vesselPosition : targetLabVessel.getVesselGeometry().getVesselPositions()) {
                        containerRole.evaluateCriteria(vesselPosition, transferTraverserCriteria,
                                TransferTraverserCriteria.TraversalDirection.Descendants, 0);
                    }
                }
            }
        }

        labEventDao.persist(new FixupCommentary(jiraTicket + " manual override to " + batchId));
        labEventDao.flush();
        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/ClearManualOverrideLabEvents.txt,
     * so it can be used for other similar fixups, without writing a new test.  It is used to clear previous LabBatch
     * manual overrides of LabEvents.  Example contents of the file are:
     * GPLIM-5906
     * 3117214
     */
    @Test(enabled = false)
    public void fixupGplim5906() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ClearManualOverrideLabEvents.txt"));
        String jiraTicket = lines.get(0);
        for (String id : lines.subList(1, lines.size())) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, Long.parseLong(id));
            labEvent.setManualOverrideLcSet(null);
            System.out.println("Lab event " + labEvent.getLabEventId() + " clear manual override");
        }

        labEventDao.persist(new FixupCommentary(jiraTicket + " clear manual override"));
        labEventDao.flush();
        utx.commit();
    }

    /**
     * A Pond Registration in PCR Plus workflow must be PCR Plus Pond Registration in order to ETL the library name <br/>
     * ETL refresh events 1982110 and 1983690 after ticket deployed and this test is run
     */
    @Test(enabled = false)
    public void fixupGplim4851() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        LabEvent labEvent = labEventDao.findById(LabEvent.class, 1982110L);
        Assert.assertTrue(labEvent.getLabEventType() == LabEventType.POND_REGISTRATION);
        labEvent.setLabEventType(LabEventType.PCR_PLUS_POND_REGISTRATION);

        labEventDao.persist(
                new FixupCommentary("GPLIM-4851 Change event 1982110 type to PCR_PLUS_POND_REGISTRATION for workflow"));
        labEventDao.flush();
        utx.commit();
    }

    /**
     * This test reads its parameters from a file, testdata/FixupStations.txt, so it can be used for other similar fixups,
     * without writing a new test.  Example contents of the file are:
     * GPLIM-5294
     * 2458267 SL-HCD SL-HDE
     * <p>
     * Where the format for the second line is labEventId, old station, new station
     */
    @Test(enabled = false)
    public void fixupGplim5294() throws Exception {
        try {
            userBean.loginOSUser();
            utx.begin();

            List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("FixupStations.txt"));
            String jiraTicket = lines.get(0);
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = WHITESPACE_PATTERN.split(lines.get(i));
                if (fields.length != 3) {
                    throw new RuntimeException("Expected three white-space separated fields in " + lines.get(i));
                }
                long labEventId = Long.parseLong(fields[0]);
                String oldStation = fields[1];
                String newStation = fields[2];
                LabEvent labEvent = labEventDao.findById(LabEvent.class, labEventId);
                if (labEvent == null) {
                    throw new RuntimeException("Failed to find lab event " + labEventId);
                } else if (!labEvent.getEventLocation().equals(oldStation)) {
                    throw new RuntimeException("Did not find expected station of " + oldStation);
                }
                System.out.println("Updating station of " + labEventId + " from " +
                                   oldStation + " to " + newStation);
                labEvent.setEventLocation(newStation);
            }

            labEventDao.persist(new FixupCommentary(jiraTicket + " update stations"));
            labEventDao.flush();
            utx.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupSupport3870() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        List<Triple<Long, String, String>> triples = new ArrayList<Triple<Long, String, String>>() {{
            add(Triple.of(2582026L, "1162992550", "B04"));
            add(Triple.of(2581978L, "1162992600", "A12"));
            add(Triple.of(2581983L, "1162992581", "B02"));
        }};

        for (Triple<Long, String, String> triple : triples) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, triple.getLeft());
            if (labEvent == null) {
                throw new RuntimeException("Failed to find lab event: " + triple.getLeft());
            } else if (labEvent.getLabEventType() != LabEventType.AUTO_DAUGHTER_PLATE_CREATION) {
                throw new RuntimeException("Wrong lab event found");
            }
            BarcodedTube missingTube = barcodedTubeDao.findByBarcode(triple.getMiddle());
            if (missingTube == null) {
                throw new RuntimeException("Failed to find tube: " + triple.getMiddle());
            }
            SectionTransfer sectionTransfer = labEvent.getSectionTransfers().iterator().next();
            TubeFormation tubeFormation = (TubeFormation) sectionTransfer.getSourceVesselContainer().getEmbedder();

            VesselPosition missingVesselPosition = VesselPosition.getByName(triple.getRight());
            BarcodedTube vesselAtPosition =
                    tubeFormation.getContainerRole().getVesselAtPosition(missingVesselPosition);
            Assert.assertNull(vesselAtPosition);

            Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
            VesselPosition[] vesselPositions = tubeFormation.getVesselGeometry().getVesselPositions();
            for (int i = 0; i < vesselPositions.length; i++) {
                mapPositionToTube.put(vesselPositions[i],
                        tubeFormation.getContainerRole().getVesselAtPosition(vesselPositions[i]));
            }
            mapPositionToTube.put(missingVesselPosition, missingTube);
            TubeFormation newTubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
            sectionTransfer.setSourceVesselContainer(newTubeFormation.getContainerRole());
            System.out.println(
                    "Add tube " + missingTube.getLabel() + " at " + missingVesselPosition.name() + " to  event "
                    + triple.getLeft());
        }

        // Add Missing H12 cherry pick to Hybridization events
        List<Long> labEvents = Arrays.asList(2591464L, 2591473L, 2591764L);
        for (Long labEventId : labEvents) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, labEventId);
            if (labEvent == null) {
                throw new RuntimeException("Failed to find lab event: " + labEventId);
            } else if (labEvent.getLabEventType() != LabEventType.INFINIUM_HYBRIDIZATION) {
                throw new RuntimeException("Wrong lab event found");
            }

            StaticPlate sourcePlate =
                    (StaticPlate) labEvent.getCherryPickTransfers().iterator().next().getSourceVesselContainer().
                            getEmbedder();
            StaticPlate destChip =
                    (StaticPlate) labEvent.getCherryPickTransfers().iterator().next().getTargetVesselContainer().
                            getEmbedder();
            labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                    sourcePlate.getContainerRole(), VesselPosition.H12, null,
                    destChip.getContainerRole(), VesselPosition.R12C02, null, labEvent));
            System.out.println("Adding Cherry pick from H12 of " + sourcePlate.getLabel() + " to R12C02 of " + destChip.getLabel() +
                " for lab event " + labEvent.getLabEventId());
        }

        //Change Lab event types to arrays
        for (long id : new Long[]{2582123L, 2582124L, 2582125L, 2581983L, 2582026L, 2581978L, 2584507L}) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent == null || labEvent.getLabEventType() != LabEventType.AUTO_DAUGHTER_PLATE_CREATION) {
                throw new RuntimeException("cannot find " + id + " or is not AUTO_DAUGHTER_PLATE_CREATION");
            }
            System.out.print("LabEvent " + id + " type " + labEvent.getLabEventType());
            labEvent.setLabEventType(LabEventType.ARRAY_PLATING_DILUTION);
            System.out.println("   updated to " + labEvent.getLabEventType());

        }

        labEventDao.persist(new FixupCommentary("SUPPORT-3870 add missing wells and change event tyo to array plating dilution"));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGPLIM5438() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        // Change Lab event types to arrays
        for (long id : new Long[]{2584508L, 2584509L, 2584616L, 2584617L, 2584618L}) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent == null || labEvent.getLabEventType() != LabEventType.AUTO_DAUGHTER_PLATE_CREATION) {
                throw new RuntimeException("Cannot find event " + id + " or is not AUTO_DAUGHTER_PLATE_CREATION");
            }
            System.out.print("LabEvent " + id + " type " + labEvent.getLabEventType());
            labEvent.setLabEventType(LabEventType.ARRAY_PLATING_DILUTION);
            System.out.println("   updated to " + labEvent.getLabEventType());

        }

        labEventDao.persist(new FixupCommentary("GPLIM-5438 change event type from AUTO_DAUGHTER_PLATE_CREATION to ARRAY_PLATING_DILUTION"));
        labEventDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport4140() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        long[] ids =
                { 2785453L, 2785461L, 2785475L, 2785488L, 2785494L, 2785498L, 2785512L, 2785520L, 2785536L, 2785539L,
                        2785542L,2785551L,2785556L,2785566L};

        Reagent undesired = reagentDao.findByReagentNameLotExpiration("HS buffer", "RG-14921", null);
        Reagent desired = reagentDao.findByReagentNameLotExpiration("HS buffer", "RG-15500", null);

        // It is unclear if the lab will have done another round of pico with the new reagent, so check for existence or
        // create if need be.
        if (desired == null) {
            desired = new GenericReagent("HS buffer", "RG-15500", null);
        }
        Assert.assertNotNull(undesired);
        Assert.assertNotNull(desired);

        for (long id : ids) {
            LabEvent labEvent = labEventDao.findById(LabEvent.class, id);
            if (labEvent == null || labEvent.getLabEventType() != LabEventType.PICO_DILUTION_TRANSFER) {
                throw new RuntimeException("cannot find " + id + " or is not PICO_DILUTION_TRANSFER");
            }
            for (LabEventReagent labEventReagent : labEvent.getLabEventReagents()) {
                if (labEventReagent.getReagent().equals(undesired)) {
                    System.out.println("Removing " + undesired.getName() + " on event " + labEvent.getLabEventId());
                    labEvent.getLabEventReagents().remove(labEventReagent);
                    genericReagentDao.remove(labEventReagent);
                }
            }
            System.out.println("Adding " + desired.getName() + " on event " + labEvent.getLabEventId());
            labEvent.addReagent(desired);
        }

        FixupCommentary fixupCommentary = new FixupCommentary(
                "SUPPORT-4140 - Removing expired reagent for new one");
        labEventDao.persist(fixupCommentary);
        labEventDao.flush();

        utx.commit();
    }

}
