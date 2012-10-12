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
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductOrderSampleContainerTest extends ContainerTest {

    public void testOrderSampleConstruction() {

        ProductOrderSample testSample = new ProductOrderSample("SM-1P3XN");

        Assert.assertTrue ( testSample.isInBspFormat ( ) );

        Assert.assertFalse(testSample.hasBSPDTOBeenInitialized());

        try {

            Assert.assertTrue(testSample.isSampleReceived());
            Assert.assertTrue(testSample.isActiveStock());

            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_VOLUME , testSample.getVolume());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_ROOT_SAMP , testSample.getRootSample());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_STOCK_SAMP, testSample.getStockSample());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_COLL , testSample.getCollection());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_SAMP_ID, testSample.getCollaboratorsSampleName());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_CONTAINER_ID, testSample.getContainerId());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_PATIENT_ID,testSample.getParticipantId());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_SPECIES, testSample.getOrganism());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_LSID, testSample.getSampleLsid());


            Assert.assertTrue(testSample.hasFingerprint ( ));
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_PID , testSample.getCollaboratorParticipantId());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_MAT_TYPE, testSample.getMaterialType());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_DNA, testSample.getTotal());
            Assert.assertEquals( BSPSampleDTO.NORMAL_IND, testSample.getSampleType());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_DISEASE, testSample.getDisease());
            Assert.assertEquals( BSPSampleDTO.MALE_IND, testSample.getGender());
            Assert.assertEquals( BSPSampleDTO.ACTIVE_IND, testSample.getStockType());
            Assert.assertEquals( BSPSampleSearchServiceStub.SM_1P3XN_FP, testSample.getFingerprint());

        } catch (IllegalStateException ise) {
            Assert.fail();
        }

    }

}
