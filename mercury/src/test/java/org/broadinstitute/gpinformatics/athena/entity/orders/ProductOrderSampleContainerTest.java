package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataSourceResolver;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collections;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
@Dependent
public class ProductOrderSampleContainerTest extends Arquillian {

    public ProductOrderSampleContainerTest(){}

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    public static final String PDO_TO_LEDGER_ENTRY_COUNT_PROVIDER = "PDO-To-Ledger-Entry-Count-Provider";

    @Inject
    private ProductOrderSampleDao dao;

    @Inject
    private ProductOrderDao pdoDao;

    @Inject
    private SampleDataSourceResolver sampleDataSourceResolver;

    public void testOrderSampleConstruction() {
        ProductOrderSample testSample = new ProductOrderSample("SM-1P3XN");
        Assert.assertTrue(testSample.isInBspFormat());
        Assert.assertTrue(testSample.needsBspMetaData());

        SampleData sampleData = testSample.getSampleData();

        sampleDataSourceResolver.populateSampleDataSources(Collections.singleton(testSample));

        Assert.assertNotNull(sampleData.getVolume());
        Assert.assertNotNull(sampleData.getRootSample());
    }


    /**
     * Simple {@link @DataProvider} that maps from PDO business keys to expected counts of PDO samples with billing ledger
     * entries.
     *
     * @return Full mapping of PDO keys to expected counts.
     */
    @DataProvider(name = PDO_TO_LEDGER_ENTRY_COUNT_PROVIDER)
    public Object[][] pdoToLedgerEntryCountProvider() {
        return new Object[][] {
                {"PDO-55", 0},
                {"PDO-50", 2},
                {"PDO-57", 41}
        };
    }


    /**
     * Test of counts of PDO samples with existing billing ledger entries.  It's not great that this is a container test
     * that uses real data, this should ideally be writing out its own PDOs with PDO samples and billing ledger entries.
     *
     * @param pdoBusinessKey The JIRA key for a PDO.
     *
     * @param expectedCount The number of PDO samples in the specified PDO that are expected to have billing ledger entries.
     */
    // TODO Rewrite this to not use existing data.
    @Test(dataProvider = PDO_TO_LEDGER_ENTRY_COUNT_PROVIDER, enabled = false)
    public void testCountSamplesWithLedgerEntries(@Nonnull String pdoBusinessKey, @Nonnull Integer expectedCount) {

        ProductOrder productOrder = pdoDao.findByBusinessKey(pdoBusinessKey);
        long actualCount = dao.countSamplesWithLedgerEntries(productOrder);
        Assert.assertEquals(actualCount, (long) expectedCount);
    }
}
