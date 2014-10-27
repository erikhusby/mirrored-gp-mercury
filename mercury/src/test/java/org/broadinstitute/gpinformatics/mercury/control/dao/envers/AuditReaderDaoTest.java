package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.hibernate.envers.RevisionType;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container test of AuditReaderDao.
 */

@Test(enabled = true, groups = TestGroups.STANDARD)
public class AuditReaderDaoTest extends ContainerTest {
    @Inject
    private AuditReaderDao auditReaderDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Test(groups = TestGroups.STANDARD)
    public void testGetModifiedEntityTypes() throws Exception {
        final long now = System.currentTimeMillis();
        final LabVessel labVessel = new BarcodedTube("A" + now);
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
        final long now = System.currentTimeMillis();
        final String barcode = "A" + now;
        // List of all values from start to end of this test, and should have at least this test's transactions.
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

        // Modifies the tube type.
        auditedRevDtos.addAll(revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(barcode);
                tube.setTubeType(BarcodedTube.BarcodedTubeType.Cryovial2018);
                barcodedTubeDao.persist(tube);
                barcodedTubeDao.flush();
            }
        }));

        // Gets the entity id before entity is deleted.
        BarcodedTube persistedTube = barcodedTubeDao.findByBarcode(barcode);
        Assert.assertNotNull(persistedTube);
        Assert.assertNotNull(persistedTube.getLabVesselId());

        // Deletes the tube.
        auditedRevDtos.addAll(revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(barcode);
                barcodedTubeDao.remove(tube);
                barcodedTubeDao.flush();
            }
        }));

        // Creates a random different new barcoded tube.
        auditedRevDtos.addAll(revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                LabVessel labVessel = new BarcodedTube(barcode + "0", BarcodedTube.BarcodedTubeType.MatrixTube);
                labVesselDao.persist(labVessel);
                labVesselDao.flush();
            }
        }));


        Assert.assertTrue(auditedRevDtos.size() >= 4);

        // Iterates on all the BarcodedTube types in the list of revIds, and picks the
        // ones that created, modified, and deleted the tube under test.
        Long tubeCreatedRevId = null;
        Long tubeModifiedRevId = null;
        Long tubeDeletedRevId = null;

        Set<Long> revIds = new HashSet<>();
        for (AuditedRevDto auditedRevDto : auditedRevDtos) {
            revIds.add(auditedRevDto.getRevId());
        }

        List<EnversAudit> auditEntities = auditReaderDao.fetchEnversAudits(revIds, BarcodedTube.class);
        for (EnversAudit<BarcodedTube> enversAudit : auditEntities) {
            BarcodedTube auditedTube = enversAudit.getEntity();
            Long revId = enversAudit.getRevInfo().getRevInfoId();
            RevisionType revType = enversAudit.getRevType();

            if (persistedTube.getLabVesselId().equals(auditedTube.getLabVesselId())) {
                switch(revType) {
                case ADD:
                    Assert.assertNull(tubeCreatedRevId);
                    tubeCreatedRevId = revId;
                    break;
                case MOD:
                    Assert.assertNull(tubeModifiedRevId);
                    tubeModifiedRevId = revId;
                    break;
                case DEL:
                    Assert.assertNull(tubeDeletedRevId);
                    tubeDeletedRevId = revId;
                }
            }
        }
        Assert.assertNotNull(tubeCreatedRevId);
        Assert.assertNotNull(tubeModifiedRevId);
        Assert.assertNotNull(tubeDeletedRevId);

        Long entityId = persistedTube.getLabVesselId();

        // Now checks that the entity diffs from version to version show the change in tube type.
        Long prevId = auditReaderDao.getPreviousVersionRevId(entityId, BarcodedTube.class, tubeCreatedRevId);
        Assert.assertNull(prevId);

        prevId = auditReaderDao.getPreviousVersionRevId(entityId, BarcodedTube.class, tubeModifiedRevId);
        Assert.assertEquals(prevId, tubeCreatedRevId);
        BarcodedTube createdTube = auditReaderDao.getEntityAtVersion(entityId, BarcodedTube.class, prevId);
        Assert.assertEquals(createdTube.getTubeType(), BarcodedTube.BarcodedTubeType.MatrixTube);

        prevId = auditReaderDao.getPreviousVersionRevId(entityId, BarcodedTube.class, tubeDeletedRevId);
        Assert.assertEquals(prevId, tubeModifiedRevId);
        BarcodedTube modifiedTube = auditReaderDao.getEntityAtVersion(entityId, BarcodedTube.class, prevId);
        Assert.assertEquals(modifiedTube.getTubeType(), BarcodedTube.BarcodedTubeType.Cryovial2018);


        // Test getting at the revs another way.
        List<AuditedRevDto> auditedRevs = auditReaderDao.fetchAuditedRevs(revIds);
        Map<Long, List<AuditedRevDto>> map = AuditedRevDto.mappedByRevId(auditedRevs);
        Assert.assertTrue(map.keySet().contains(tubeCreatedRevId));
        Assert.assertTrue(map.keySet().contains(tubeModifiedRevId));
        Assert.assertTrue(map.keySet().contains(tubeDeletedRevId));
        // Verify only the one entity was modified at the tubeModifiedRevId transaction.
        Assert.assertTrue(map.get(tubeModifiedRevId).size() == 1);
        Assert.assertTrue(map.get(tubeModifiedRevId).get(0).getEntityTypeNames().contains(
                BarcodedTube.class.getCanonicalName()));
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
