package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.SQLQuery;
import org.hibernate.envers.RevisionType;
import org.hibernate.type.LongType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

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
        Collection<Long> revIds = revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                labVesselDao.persist(labVessel);
                labVesselDao.flush();
            }
        });

        boolean found = false;
        for (Long revId : revIds) {
            Collection<String> names = auditReaderDao.getClassnamesModifiedAtRevision(revId);
            for (String name : names) {
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

        // Create, modify, delete the tube given by barcode.
        Long startRevId = revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                LabVessel labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                labVesselDao.persist(labVessel);
                labVesselDao.flush();
            }
        }).first();

        // Gets the new tube's entity id.
        BarcodedTube persistedTube = barcodedTubeDao.findByBarcode(barcode);
        Assert.assertNotNull(persistedTube.getLabVesselId());

        revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(barcode);
                tube.setTubeType(BarcodedTube.BarcodedTubeType.Cryovial2018);
                barcodedTubeDao.persist(tube);
                barcodedTubeDao.flush();
            }
        });

        Long endRevId = revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(barcode);
                barcodedTubeDao.remove(tube);
                barcodedTubeDao.flush();
            }
        }).last();

        // List of all values from start to end. Should have at least the above transactions.
        List<Long> revIds = new ArrayList<>();
        for (long i = startRevId; i <= endRevId; ++i) {
            revIds.add(i);
        }
        Assert.assertTrue(revIds.size() >= 4);

        // Iterates on all the BarcodedTube audits for the list of revIds, and picks the
        // one that created our barcodedTube, and the one that modified it.
        Long tubeCreatedRevId = null;
        Long tubeModifiedRevId = null;
        Long tubeDeletedRevId = null;

        List<EnversAudit> auditEntities = auditReaderDao.fetchEnversAudits(revIds, BarcodedTube.class);
        for (EnversAudit<BarcodedTube> enversAudit : auditEntities) {
            BarcodedTube auditedTube = enversAudit.getEntity();
            Long revId = enversAudit.getRevInfo().getRevInfoId();
            RevisionType revType = enversAudit.getRevType();

            if (persistedTube.getLabVesselId().equals(auditedTube.getLabVesselId())) {
                switch(revType) {
                case ADD:
                    tubeCreatedRevId = revId;
                    break;
                case MOD:
                    tubeModifiedRevId = revId;
                    break;
                case DEL:
                    tubeDeletedRevId = revId;
                }
            }
        }
        Assert.assertNotNull(tubeCreatedRevId);
        Assert.assertNotNull(tubeModifiedRevId);
        Assert.assertNotNull(tubeDeletedRevId);

        BarcodedTube preCreationTube =
                auditReaderDao.getPreviousVersion(persistedTube, BarcodedTube.class, tubeCreatedRevId);
        Assert.assertNull(preCreationTube);

        BarcodedTube createdTube =
                auditReaderDao.getPreviousVersion(persistedTube, BarcodedTube.class, tubeModifiedRevId);
        Assert.assertEquals(createdTube.getTubeType(), BarcodedTube.BarcodedTubeType.MatrixTube);

        BarcodedTube modifiedTube =
                auditReaderDao.getPreviousVersion(persistedTube, BarcodedTube.class, tubeDeletedRevId);
        Assert.assertEquals(modifiedTube.getTubeType(), BarcodedTube.BarcodedTubeType.Cryovial2018);


        // Test getting at the revs another way.
        List<AuditedRevDto> auditedRevs = auditReaderDao.fetchAuditedRevs(revIds);
        Assert.assertTrue(auditedRevs.size() >= 4);
        Assert.assertTrue(auditedRevs.size() <= revIds.size());
        Map<Long, List<AuditedRevDto>> map = AuditedRevDto.mappedByRevId(auditedRevs);
        Assert.assertTrue(map.keySet().contains(tubeCreatedRevId));
        Assert.assertTrue(map.keySet().contains(tubeModifiedRevId));
        Assert.assertTrue(map.keySet().contains(tubeDeletedRevId));
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

    /**
     * Returns the revInfoId after running the transactionToBracket.
     *
     * May include more revs than just what's in transactionToBracket, depending on what else
     * the Mercury app is doing.
     *
     * RevInfo.revInfoId is non-monotonic so must use RevInfo revDate.
     */
    private SortedSet<Long> revsBracketingTransaction(Runnable transactionToBracket) {
        long startTimeSec = System.currentTimeMillis() / 1000;
        try {
            utx.begin();
            transactionToBracket.run();
            utx.commit();
        } catch (Exception e) {
            Assert.fail("Caught exception ", e);
        }
        long endTimeSec = System.currentTimeMillis() / 1000 + 1;
        return (SortedSet<Long>) auditReaderDao.fetchAuditIds(startTimeSec, endTimeSec).keySet();
    }
}
