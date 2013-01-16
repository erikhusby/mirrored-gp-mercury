package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDaoTest;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;

/**
 * @author Scott Matthews
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductOrderSampleContainerTest extends ContainerTest {

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;


    public void testOrderSampleConstruction() {

        ProductOrder productOrder = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao);
        ProductOrderSample testSample = new ProductOrderSample("SM-1P3XN");
        testSample.setProductOrder(productOrder);

        productOrder.setSamples(Collections.singletonList(testSample));

        Assert.assertTrue(testSample.isInBspFormat());

        Assert.assertTrue(testSample.needsBspMetaData());

        try {
            BSPSampleDTO bspDTO = testSample.getBspDTO();
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

            Assert.assertTrue(bspDTO.getHasFingerprint());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_PID, bspDTO.getCollaboratorParticipantId());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_MAT_TYPE, bspDTO.getMaterialType());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_DNA, String.valueOf(bspDTO.getTotal()));
            Assert.assertEquals(ProductOrderSample.NORMAL_IND, bspDTO.getSampleType());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_DISEASE, bspDTO.getPrimaryDisease());
            Assert.assertEquals(ProductOrderSample.MALE_IND, bspDTO.getGender());
            Assert.assertEquals(ProductOrderSample.ACTIVE_IND, bspDTO.getStockType());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_FP, bspDTO.getFingerprint());

        } catch (IllegalStateException ise) {
            Assert.fail();
        }
    }
}
