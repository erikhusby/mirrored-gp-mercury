package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Scott Matthews
 *         Date: 10/11/12
 *         Time: 12:02 PM
 */
public class ProductOrderSampleContainerTest extends ContainerTest {

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testOrderSampleConstruction() {

        ProductOrderSample testSample = new ProductOrderSample("SM-1P3XN");

        Assert.assertTrue ( testSample.isInBspFormat ( ) );

        Assert.assertFalse(testSample.hasBSPDTOBeenInitialized());

        try {

            Assert.assertTrue(testSample.isSampleReceived());
            Assert.assertTrue(testSample.isActiveStock());

            Assert.assertEquals(testSample.getVolume(), BSPSampleSearchServiceStub.SM_1P3XN_VOLUME);
            Assert.assertEquals(testSample.getRootSample(), BSPSampleSearchServiceStub.SM_1P3XN_ROOT_SAMP);
            Assert.assertEquals(testSample.getStockSample(), BSPSampleSearchServiceStub.SM_1P3XN_STOCK_SAMP);
            Assert.assertEquals(testSample.getCollection(), BSPSampleSearchServiceStub.SM_1P3XN_COLL);
            Assert.assertEquals(testSample.getCollaboratorsSampleName(), BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_SAMP_ID);
            Assert.assertEquals(testSample.getContainerId(), BSPSampleSearchServiceStub.SM_1P3XN_CONTAINER_ID);
            Assert.assertEquals(testSample.getParticipantId(), BSPSampleSearchServiceStub.SM_1P3XN_PATIENT_ID);
            Assert.assertEquals(testSample.getOrganism(), BSPSampleSearchServiceStub.SM_1P3XN_SPECIES);
            Assert.assertEquals(testSample.getSampleLsid(), BSPSampleSearchServiceStub.SM_1P3XN_LSID);


            Assert.assertTrue(testSample.hasFingerprint ( ));
            Assert.assertEquals(testSample.getCollaboratorParticipantId(), BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_PID);
            Assert.assertEquals(testSample.getMaterialType(), BSPSampleSearchServiceStub.SM_1P3XN_MAT_TYPE);
            Assert.assertEquals(testSample.getTotal(), BSPSampleSearchServiceStub.SM_1P3XN_DNA);
            Assert.assertEquals(testSample.getSampleType(), BSPSampleDTO.NORMAL_IND);
            Assert.assertEquals(testSample.getDisease(), BSPSampleSearchServiceStub.SM_1P3XN_DISEASE);
            Assert.assertEquals(testSample.getGender(), BSPSampleDTO.MALE_IND);
            Assert.assertEquals(testSample.getStockType(), BSPSampleDTO.ACTIVE_IND);
            Assert.assertEquals(testSample.getFingerprint(), BSPSampleSearchServiceStub.SM_1P3XN_FP);

        } catch (IllegalStateException ise) {
            Assert.fail();
        }

    }

}
