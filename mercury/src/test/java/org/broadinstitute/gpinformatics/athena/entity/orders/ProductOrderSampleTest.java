package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfigProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/3/12
 * Time: 5:59 PM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class ProductOrderSampleTest {

    @Test
    public void testIsInBspFormat() throws Exception {

        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG"));
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG5"));
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG6"));
        Assert.assertFalse(ProductOrderSample.isInBspFormat(null));

    }

    public void testOrderSampleConstruction() {

        ProductOrderSample testSample = new ProductOrderSample("SM-1P3XN");

        Assert.assertTrue(testSample.isInBspFormat());

        Assert.assertFalse(testSample.hasBSPDTOBeenInitialized());

        try {
            Assert.assertTrue(testSample.hasFootprint());
            Assert.assertEquals(testSample.getCollaboratorParticipantId(),BSPSampleSearchServiceStub.SM_1P3XN_COLLAB_PID);
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
