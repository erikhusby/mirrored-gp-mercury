package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.SQLQuery;
import org.hibernate.envers.RevisionType;
import org.hibernate.type.LongType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

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
        Pair<Long, Long> revPair = revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                labVesselDao.persist(labVessel);
                labVesselDao.flush();
            }
        });

        boolean found = false;
        for (long revId = revPair.getLeft(); revId <= revPair.getRight(); ++revId) {
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

        Long startRevId;
        Long endRevId;
        Pair<Long, Long> revPair;

        // Create, modify, delete the tube given by barcode.
        revPair = revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                LabVessel labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                labVesselDao.persist(labVessel);
                labVesselDao.flush();
            }
        });
        startRevId = revPair.getLeft();

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

        revPair = revsBracketingTransaction(new Runnable() {
            @Override
            public void run() {
                BarcodedTube tube = barcodedTubeDao.findByBarcode(barcode);
                barcodedTubeDao.remove(tube);
                barcodedTubeDao.flush();
            }
        });
        endRevId = revPair.getRight();

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

        List<EnversAudit> auditEntities = auditReaderDao.fetchDataChanges(revIds, BarcodedTube.class);
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

    }

    /**
     * Returns the revInfoId before and after running the transactionToBracket.
     * May include more revs than just what's in transactionToBracket, depending on what else
     * the Mercury app is doing.
     */
    private Pair<Long, Long> revsBracketingTransaction(Runnable transactionToBracket) throws Exception {
        // Gets the current revId before the transaction is run.
        String queryString = "select max(rev_info_id) max_id from rev_info";
        Query query = auditReaderDao.getEntityManager().createNativeQuery(queryString);
        query.unwrap(SQLQuery.class).addScalar("max_id", LongType.INSTANCE);
        Long startRevId = (Long)query.getSingleResult();
        Assert.assertTrue(startRevId != null && startRevId > 1L);

        utx.begin();
        transactionToBracket.run();
        utx.commit();

        // Gets the now current revId after the transaction.
        Long endRevId = (Long)query.getSingleResult();
        Assert.assertTrue(endRevId > startRevId);
        return new ImmutablePair(startRevId, endRevId);
    }
}
