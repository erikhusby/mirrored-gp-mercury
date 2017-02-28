package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Container test of AuditReaderDao.
 */

@Test(enabled = true, groups = TestGroups.STANDARD)
public class AuditReaderDaoTest extends Arquillian {
    private final static Random RANDOM = new Random(System.currentTimeMillis());
    private final static int NINE_NINES = 999999999;

    @Inject
    private AuditReaderDao auditReaderDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.STANDARD)
    public void testGetModifiedEntityTypes() throws Exception {
        final LabVessel labVessel = new BarcodedTube("A" + RANDOM.nextInt(NINE_NINES));
        List<AuditedRevDto> auditedRevDtos = revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                labVesselDao.persist(labVessel);
                labVesselDao.flush();
            }
        });

        boolean found = false;
        for (AuditedRevDto auditedRevDto : auditedRevDtos) {
            for (String name : auditedRevDto.getEntityTypeNames()) {
                if (name.contains(BarcodedTube.class.getCanonicalName())) {
                    found = true;
                }
            }
        }
        Assert.assertTrue(found);
    }

    @Test(groups = TestGroups.STANDARD)
    public void testPreviousRev() throws Exception {
        final String barcode = "A" + RANDOM.nextInt(NINE_NINES);
        final String otherBarcode = "A" + RANDOM.nextInt(NINE_NINES);

        // Captures all of the audit rev values from start to end of this test.
        // At the end it will have captured this test's transactions and may contain
        // others due to db commits from other sources.
        Set<AuditedRevDto> auditedRevDtos = new HashSet<>();

        // Creates a new barcoded tube.
        auditedRevDtos.addAll(revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                LabVessel labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                labVesselDao.persist(labVessel);
                labVesselDao.flush();
            }
        }));

        // Gets the persisted entity.
        BarcodedTube persistedTube = barcodedTubeDao.findByBarcode(barcode);
        Assert.assertNotNull(persistedTube);
        Assert.assertNotNull(persistedTube.getLabVesselId());

        // Updates the tube.  The previous rev of this should be the create and not the data fixup
        // (whose rev will be set to a lower value than this update).
        auditedRevDtos.addAll(revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(barcode);
                tube.setTubeType(BarcodedTube.BarcodedTubeType.Cryovial2018);
                barcodedTubeDao.persist(tube);
                barcodedTubeDao.flush();
            }
        }));


        // Simulates a data fixup on the tube type.  This rev is treated differently in order
        // to test correctly getting the previous rev when there was an out of sequence data fixup,
        // which will frequently happen when two JVMs running Mercury write to the same database.
        List<AuditedRevDto> fixupRevDtos = new ArrayList<>();
        fixupRevDtos.addAll(revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(barcode);
                tube.setTubeType(BarcodedTube.BarcodedTubeType.AbgeneTube96plugcap065);
                barcodedTubeDao.persist(tube);
                barcodedTubeDao.flush();
            }
        }));
        // Isolates the audit rev of the fixup transaction.  There is likely an overlap with the
        // previous transaction.
        fixupRevDtos.removeAll(auditedRevDtos);
        Assert.assertTrue(fixupRevDtos.size() >= 1);
        for (Iterator<AuditedRevDto> iter = fixupRevDtos.iterator(); iter.hasNext(); ) {
            AuditedRevDto fixupRevDto = iter.next();
            if (fixupRevDto.getEntityTypeNames().size() == 1 &&
                fixupRevDto.getEntityTypeNames().iterator().next().equals(BarcodedTube.class.getCanonicalName())) {
                BarcodedTube tube = auditReaderDao.getEntityAtVersion(persistedTube.getLabVesselId(),
                        persistedTube.getClass(), fixupRevDto.getRevId());
                if (tube == null || tube.getTubeType() != BarcodedTube.BarcodedTubeType.AbgeneTube96plugcap065) {
                    iter.remove();
                }
            } else {
                iter.remove();
            }
        }
        Assert.assertEquals(fixupRevDtos.size(), 1, "Cannot find the one data fixup audit.");
        // Finds a gap in rev_info_id at least a day old (well before first transaction of this test)
        // and picks an unused rev_info_id in that gap.  The fixupRev is changed to be that unused rev.
        BigDecimal unusedRevId = (BigDecimal) auditReaderDao.getEntityManager().createNativeQuery(
                "SELECT r1.rev_info_id + 1 FROM rev_info r1 " +
                " WHERE NOT EXISTS (SELECT 1 FROM rev_info r2 WHERE r2.rev_info_id = r1.rev_info_id + 1) " +
                " AND rev_date < SYSDATE - 1 " +
                " AND rownum = 1").getSingleResult();
        Assert.assertNotNull(unusedRevId);
        // Updates the fixup audit rev id and keeps it.
        AuditedRevDto fixupRevDto = new AuditedRevDto(unusedRevId.longValue(), fixupRevDtos.get(0).getRevDate(),
                fixupRevDtos.get(0).getUsername(), fixupRevDtos.get(0).getEntityTypeNames());
        auditedRevDtos.add(fixupRevDto);
        utx.begin();
        auditReaderDao.getEntityManager().createNativeQuery(
                "INSERT INTO rev_info (rev_info_id, rev_date, username)" +
                " SELECT " + unusedRevId + ", rev_date, username " +
                " FROM rev_info WHERE rev_info_id = " + fixupRevDtos.get(0).getRevId()).executeUpdate();
        auditReaderDao.getEntityManager().createNativeQuery(
                "UPDATE revchanges SET rev = " + unusedRevId +
                " WHERE rev = " + fixupRevDtos.get(0).getRevId()).executeUpdate();
        auditReaderDao.getEntityManager().createNativeQuery(
                "UPDATE lab_vessel_aud SET rev = " + unusedRevId +
                " WHERE lab_vessel_id = " + persistedTube.getLabVesselId() +
                " AND rev = " + fixupRevDtos.get(0).getRevId()).executeUpdate();
        utx.commit();
        // Removes the old fixupRev so audit trail and etl don't get confused by it.
        utx.begin();
        auditReaderDao.getEntityManager().createNativeQuery(
                "DELETE FROM rev_info WHERE rev_info_id = " + fixupRevDtos.get(0).getRevId()).executeUpdate();
        utx.commit();

        // Deletes the tube.  The previous rev should be the fixup, not the first update.
        auditedRevDtos.addAll(revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(barcode);
                barcodedTubeDao.remove(tube);
                barcodedTubeDao.flush();
            }
        }));

        // Creates another new barcoded tube to see it gets picked up too.
        auditedRevDtos.addAll(revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                LabVessel labVessel = new BarcodedTube(otherBarcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                labVesselDao.persist(labVessel);
                labVesselDao.flush();
            }
        }));


        Assert.assertTrue(auditedRevDtos.size() >= 4);
        Long entityId = persistedTube.getLabVesselId();

        Set<Long> revIds = new HashSet<>();
        for (AuditedRevDto auditedRevDto : auditedRevDtos) {
            revIds.add(auditedRevDto.getRevId());
        }

        BarcodedTube[] auditedTubes = new BarcodedTube[4];
        Long[] auditRevIds = new Long[4];
        boolean foundOtherTube = false;
        // Fetches all tubes and puts the ones for persisted tube in the expected order.
        for (EnversAudit<BarcodedTube> enversAudit : auditReaderDao.fetchEnversAudits(revIds, BarcodedTube.class)) {
            if (enversAudit.getEntity().getLabVesselId().equals(entityId)) {
                switch (enversAudit.getRevType()) {
                case ADD:
                    auditedTubes[0] = enversAudit.getEntity();
                    auditRevIds[0] = enversAudit.getRevInfo().getRevInfoId();
                    break;
                case MOD:
                    if (enversAudit.getEntity().getTubeType() == BarcodedTube.BarcodedTubeType.Cryovial2018) {
                        auditedTubes[1] = enversAudit.getEntity();
                        auditRevIds[1] = enversAudit.getRevInfo().getRevInfoId();
                    } else {
                        auditedTubes[2] = enversAudit.getEntity();
                        auditRevIds[2] = enversAudit.getRevInfo().getRevInfoId();
                    }
                    break;
                case DEL:
                    auditedTubes[3] = enversAudit.getEntity();
                    auditRevIds[3] = enversAudit.getRevInfo().getRevInfoId();
                    break;
                default:
                    Assert.fail("Unexpected revType.");
                }
            } else {
                foundOtherTube = true;
            }
        }
        Assert.assertTrue(foundOtherTube);
        for (int i = 0; i < auditedTubes.length; ++i) {
            Assert.assertNotNull(auditedTubes[i], "At " + i);
            Assert.assertNotNull(auditRevIds[i], "At " + i);
        }

        // Verifies that getPreviousVersion gives ids consistent with above.
        Assert.assertEquals(auditRevIds[2],
                auditReaderDao.getPreviousVersionRevId(entityId, BarcodedTube.class, auditRevIds[3]));
        Assert.assertEquals(auditRevIds[1],
                auditReaderDao.getPreviousVersionRevId(entityId, BarcodedTube.class, auditRevIds[2]));
        Assert.assertEquals(auditRevIds[0],
                auditReaderDao.getPreviousVersionRevId(entityId, BarcodedTube.class, auditRevIds[1]));
        Assert.assertNull(auditReaderDao.getPreviousVersionRevId(entityId, BarcodedTube.class, auditRevIds[0]));
    }

    @Test(groups = TestGroups.STANDARD)
    public void testUsernames() {
        List<String> list = auditReaderDao.getAllAuditUsername();
        Assert.assertTrue(CollectionUtils.isNotEmpty(list));
        Assert.assertTrue(list.contains("jane"));
    }

    @Test(groups = TestGroups.STANDARD)
    public void testPackages() {
        Collection<Class> classes = ReflectionUtil.getMercuryAthenaEntityClasses();
        Assert.assertTrue(CollectionUtils.isNotEmpty(classes));
        Assert.assertTrue(classes.contains(LabEvent.class));
        Assert.assertTrue(classes.contains(VesselContainer.class));
        Assert.assertTrue(classes.contains(Product.class));
        // Should not contain the gap metric db entity classes.
        Assert.assertFalse(classes.contains(AggregationReadGroup.class));
    }

    @Test(groups = TestGroups.STANDARD)
    public void testEntityAtDate() throws Exception {
        int delay = 1;
        List<Date> dates = new ArrayList<>();
        String barcode = "test" + System.currentTimeMillis();

        dates.add(new Date());
        Thread.sleep(delay);

        // Creates a new barcoded tube.
        LabVessel labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
        labVesselDao.persist(labVessel);
        labVesselDao.flush();

        Thread.sleep(delay);
        dates.add(new Date());
        Thread.sleep(delay);

        // Updates the tube.
        BarcodedTube persistedTube = barcodedTubeDao.findByBarcode(barcode);
        Assert.assertNotNull(persistedTube);
        long entityId = persistedTube.getLabVesselId();
        persistedTube.setTubeType(BarcodedTube.BarcodedTubeType.Cryovial2018);
        barcodedTubeDao.persist(persistedTube);
        barcodedTubeDao.flush();

        Thread.sleep(delay);
        dates.add(new Date());
        Thread.sleep(delay);

        // Deletes the tube.
        BarcodedTube deletionTube = barcodedTubeDao.findByBarcode(barcode);
        barcodedTubeDao.remove(deletionTube);
        barcodedTubeDao.flush();

        Thread.sleep(delay);
        dates.add(new Date());

        // Checks the versioned data and dates.
        List<Pair<Date, BarcodedTube>> list = auditReaderDao.getSortedVersionsOfEntity(BarcodedTube.class, entityId);
        Assert.assertEquals(list.size(), 3);

        Assert.assertEquals(list.get(0).getRight().getTubeType(), BarcodedTube.BarcodedTubeType.MatrixTube);
        Assert.assertEquals(list.get(1).getRight().getTubeType(), BarcodedTube.BarcodedTubeType.Cryovial2018);
        Assert.assertNull(list.get(2).getRight());

        Assert.assertTrue(list.get(0).getLeft().getTime() > dates.get(0).getTime());
        Assert.assertTrue(list.get(0).getLeft().getTime() < dates.get(1).getTime());
        Assert.assertTrue(list.get(1).getLeft().getTime() > dates.get(1).getTime());
        Assert.assertTrue(list.get(1).getLeft().getTime() < dates.get(2).getTime());
        Assert.assertTrue(list.get(2).getLeft().getTime() > dates.get(2).getTime());
        Assert.assertTrue(list.get(2).getLeft().getTime() < dates.get(3).getTime());

        Assert.assertNull(auditReaderDao.getVersionAsOf(BarcodedTube.class, entityId, dates.get(0), false));
        Assert.assertEquals(((BarcodedTube) auditReaderDao.getVersionAsOf(BarcodedTube.class, entityId, dates.get(0),
                true)).getTubeType(), BarcodedTube.BarcodedTubeType.MatrixTube);
        Assert.assertEquals(((BarcodedTube) auditReaderDao.getVersionAsOf(BarcodedTube.class, entityId, dates.get(1),
                true)).getTubeType(), BarcodedTube.BarcodedTubeType.MatrixTube);
        Assert.assertEquals(((BarcodedTube) auditReaderDao.getVersionAsOf(BarcodedTube.class, entityId, dates.get(2),
                true)).getTubeType(), BarcodedTube.BarcodedTubeType.Cryovial2018);
        Assert.assertNull(auditReaderDao.getVersionAsOf(BarcodedTube.class, entityId, dates.get(3), false));
        Assert.assertNull(auditReaderDao.getVersionAsOf(BarcodedTube.class, entityId, dates.get(3), true));
    }

    @Test(groups = TestGroups.STANDARD)
    public void testEntityAtDateFail() throws Exception {
        Assert.assertEquals(auditReaderDao.getSortedVersionsOfEntity(BarcodedTube.class, 999999999999L).size(), 0);
    }

    @Test(groups = TestGroups.STANDARD)
    public void testEntitiesAtDate() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy HH.mm.ss");

        // Picks a production PDO that was created and deleted a while ago.
        long pdoId = 104106;
        Date justAfterCreation = dateFormat.parse("07-AUG-14 11.53.41");
        Date justAfterDeletion = dateFormat.parse("11-AUG-14 10.50.24");

        Collection<ProductOrder> pdosOnDate1 = auditReaderDao.getVersionsAsOf(ProductOrder.class, justAfterCreation);
        boolean found = false;
        for (ProductOrder pdo : pdosOnDate1) {
            if (pdo.getProductOrderId() == pdoId) {
                found = true;
                Assert.assertEquals(pdo.getSamples().size(), 96);
                Assert.assertTrue(pdo.getSamples().get(0).getBusinessKey().startsWith("SM-"));
            }
        }
        Assert.assertTrue(found);

        Collection<ProductOrder> pdosOnDate2 = auditReaderDao.getVersionsAsOf(ProductOrder.class, justAfterDeletion);
        for (ProductOrder pdo : pdosOnDate2) {
            Assert.assertNotEquals(pdo.getProductOrderId(), pdoId);
        }
    }

    /**
     * Returns all of the revs that were created while running the transaction.
     *
     * May include more revs than just what's in transactionToBracket, depending on what else
     * the Mercury app is doing.
     *
     * RevInfo.revInfoId is non-monotonic so must use RevInfo revDate to determine the
     * start-end range.
     */
    private List<AuditedRevDto> revsBracketingTransaction(Runnable transactionToBracket) {
        long startTimeSec = System.currentTimeMillis() / 1000;
        try {
            utx.begin();
            transactionToBracket.run();
            utx.commit();
            Thread.yield();
        } catch (Exception e) {
            Assert.fail("Caught exception ", e);
        }
        long endTimeSec = System.currentTimeMillis() / 1000 + 1;
        return auditReaderDao.fetchAuditIds(startTimeSec, endTimeSec, AuditReaderDao.IS_ANY_USER, null);
    }
}
