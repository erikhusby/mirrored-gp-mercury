package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
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

    @Test(enabled = false)
    public void testOrderSampleConstruction() {
        ProductOrderSample testSample = new ProductOrderSample("SM-1P3XN");
        Assert.assertTrue(testSample.isInBspFormat());
        Assert.assertTrue(testSample.needsBspMetaData());

        try {
            BSPSampleDTO bspDTO = testSample.getBspSampleDTO();
            Assert.assertTrue(bspDTO.isSampleReceived());
            Assert.assertTrue(bspDTO.isActiveStock());

            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_VOLUME, String.valueOf(bspDTO.getVolume()));
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_ROOT_SAMP, bspDTO.getRootSample());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_STOCK_SAMP, bspDTO.getStockSample());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_COLL, bspDTO.getCollection());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_SAMP_ID, bspDTO.getCollaboratorsSampleName());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_CONTAINER_ID, bspDTO.getContainerId());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_PATIENT_ID, bspDTO.getPatientId());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_SPECIES, bspDTO.getOrganism());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_LSID, bspDTO.getSampleLsid());

            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_PID, bspDTO.getCollaboratorParticipantId());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_MAT_TYPE, bspDTO.getMaterialType());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_DNA, String.valueOf(bspDTO.getTotal()));
            Assert.assertEquals(ProductOrderSample.NORMAL_IND, bspDTO.getSampleType());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_DISEASE, bspDTO.getPrimaryDisease());
            Assert.assertEquals(ProductOrderSample.MALE_IND, bspDTO.getGender());
            Assert.assertEquals(ProductOrderSample.ACTIVE_IND, bspDTO.getStockType());

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
