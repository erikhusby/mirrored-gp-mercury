package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Scott Matthews
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductOrderSampleContainerTest extends ContainerTest {

    public void testOrderSampleConstruction() {
        ProductOrderSample testSample = new ProductOrderSample("SM-1P3XN", new ProductOrder());

        Assert.assertTrue(testSample.isInBspFormat());

        Assert.assertTrue(testSample.needsBspMetaData());

        try {
            Assert.assertTrue(testSample.getBspDTO().isSampleReceived());
            Assert.assertTrue(testSample.getBspDTO().isActiveStock());

            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_VOLUME, testSample.getBspDTO().getVolume());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_ROOT_SAMP, testSample.getBspDTO().getRootSample());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_STOCK_SAMP, testSample.getBspDTO().getStockSample());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_COLL, testSample.getBspDTO().getCollection());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_SAMP_ID, testSample.getBspDTO().getCollaboratorsSampleName());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_CONTAINER_ID, testSample.getBspDTO().getContainerId());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_PATIENT_ID, testSample.getBspDTO().getPatientId());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_SPECIES, testSample.getBspDTO().getOrganism());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_LSID, testSample.getBspDTO().getSampleLsid());


            Assert.assertTrue(testSample.getBspDTO().getHasFingerprint());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_PID, testSample.getBspDTO().getCollaboratorParticipantId());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_MAT_TYPE, testSample.getBspDTO().getMaterialType());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_DNA, testSample.getBspDTO().getTotal());
            Assert.assertEquals(BSPSampleDTO.NORMAL_IND, testSample.getBspDTO().getSampleType());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_DISEASE, testSample.getBspDTO().getPrimaryDisease());
            Assert.assertEquals(BSPSampleDTO.MALE_IND, testSample.getBspDTO().getGender());
            Assert.assertEquals(BSPSampleDTO.ACTIVE_IND, testSample.getBspDTO().getStockType());
            Assert.assertEquals(BSPSampleSearchServiceStub.SM_1P3XN_FP, testSample.getBspDTO().getFingerprint());

        } catch (IllegalStateException ise) {
            Assert.fail();
        }
    }
}
