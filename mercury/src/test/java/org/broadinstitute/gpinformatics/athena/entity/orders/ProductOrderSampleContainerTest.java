package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@Test(groups = TestGroups.STUBBY)
public class ProductOrderSampleContainerTest extends ContainerTest {

    public static final String PDO_TO_LEDGER_ENTRY_COUNT_PROVIDER = "PDO-To-Ledger-Entry-Count-Provider";

    @Inject
    private ProductOrderSampleDao dao;

    @Inject
    private ProductOrderDao pdoDao;


    public void testOrderSampleConstruction() {
        ProductOrderSample testSample = new ProductOrderSample("SM-1P3XN");
        Assert.assertTrue(testSample.isInBspFormat());
        Assert.assertTrue(testSample.needsBspMetaData());

        try {
            SampleData sampleData = testSample.getSampleData();
            Assert.assertTrue(testSample.isSampleAvailable());
            Assert.assertTrue(sampleData.isActiveStock());

            Assert.assertEquals(String.valueOf(sampleData.getVolume()), BSPSampleSearchServiceStub.SM_1P3XN_VOLUME);
            Assert.assertEquals(sampleData.getRootSample(), BSPSampleSearchServiceStub.SM_1P3XN_ROOT_SAMP);
            Assert.assertEquals(sampleData.getStockSample(), BSPSampleSearchServiceStub.SM_1P3XN_STOCK_SAMP);
            Assert.assertEquals(sampleData.getCollection(), BSPSampleSearchServiceStub.SM_1P3XN_COLL);
            Assert.assertEquals(sampleData.getCollaboratorsSampleName(), BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_SAMP_ID);
            Assert.assertEquals(sampleData.getContainerId(), BSPSampleSearchServiceStub.SM_1P3XN_CONTAINER_ID);
            Assert.assertEquals(sampleData.getPatientId(), BSPSampleSearchServiceStub.SM_1P3XN_PATIENT_ID);
            Assert.assertEquals(sampleData.getOrganism(), BSPSampleSearchServiceStub.SM_1P3XN_SPECIES);
            Assert.assertEquals(sampleData.getSampleLsid(), BSPSampleSearchServiceStub.SM_1P3XN_LSID);

            Assert.assertEquals(sampleData.getCollaboratorParticipantId(), BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_PID);
            Assert.assertEquals(sampleData.getMaterialType(), BSPSampleSearchServiceStub.SM_1P3XN_MAT_TYPE);
            Assert.assertEquals(String.valueOf(sampleData.getTotal()), BSPSampleSearchServiceStub.SM_1P3XN_DNA);
            Assert.assertEquals(sampleData.getSampleType(), ProductOrderSample.NORMAL_IND);
            Assert.assertEquals(sampleData.getPrimaryDisease(), BSPSampleSearchServiceStub.SM_1P3XN_DISEASE);
            Assert.assertEquals(sampleData.getGender(), ProductOrderSample.MALE_IND);
            Assert.assertEquals(sampleData.getStockType(), ProductOrderSample.ACTIVE_IND);

        } catch (IllegalStateException ise) {
            Assert.fail();
        }
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
