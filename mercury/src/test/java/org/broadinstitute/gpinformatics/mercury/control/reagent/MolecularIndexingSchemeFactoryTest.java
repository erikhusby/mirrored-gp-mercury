package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STUBBY;

/**
 * Test creation and retrieval of indexes
 */
@Test(groups = TestGroups.STUBBY)
public class MolecularIndexingSchemeFactoryTest extends ContainerTest {

    @Inject
    private MolecularIndexingSchemeFactory molecularIndexingSchemeFactory;

    @Inject
    private MolecularIndexDao molecularIndexDao;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @BeforeMethod(groups = STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @Test
    public void testFindOrCreateIndexingScheme() throws Exception {
        String sequence = "AAAAAAAAAAAAAAAAAAAA";
        molecularIndexDao.persist(new MolecularIndex(sequence));
        molecularIndexDao.flush();
        molecularIndexDao.clear();

        List<MolecularIndexingSchemeFactory.IndexPositionPair> indexPositionPairs =
                new ArrayList<>();
        indexPositionPairs.add(new MolecularIndexingSchemeFactory.IndexPositionPair(
                MolecularIndexingScheme.IndexPosition.ILLUMINA_P7, sequence));
        MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeFactory.findOrCreateIndexingScheme(
                indexPositionPairs);
        Assert.assertEquals(molecularIndexingScheme.getName(), "Illumina_P7-Babababababab", "Wrong name");

        molecularIndexingSchemeDao.flush();
        molecularIndexingSchemeDao.clear();
        MolecularIndexingScheme fetchedMolecularIndexingScheme = molecularIndexingSchemeDao.findSingleIndexScheme(
                MolecularIndexingScheme.IndexPosition.ILLUMINA_P7, sequence);
        SortedMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> mapPositionToIndex =
                fetchedMolecularIndexingScheme.getIndexes();
        Assert.assertEquals(mapPositionToIndex.size(), 1, "Wrong number of indexes");
        Assert.assertEquals(mapPositionToIndex.values().iterator().next().getSequence(), sequence,
                "Wrong sequence");
    }
}
