package org.broadinstitute.gpinformatics.athena.entity.orders;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * @author Scott Matthews
 *         Date: 10/11/12
 *         Time: 4:38 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductOrderContainerTest extends Arquillian {

    private static final Long TEST_CREATOR = 10950L;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    public static ProductOrder createSimpleProductOrder() {
        ProductOrder productOrder = new ProductOrder(TEST_CREATOR, "containerTest Product Order Test1",
                null,
                "newQuote",
                AthenaClientServiceStub.createDummyProduct (),
                createDummyResearchProject("Test Research Project"));
        productOrder.setSamples(ProductOrderTest.createSampleList("SM-1P3X9,SM-1P3WY,SM-1P3XN", productOrder));
        return productOrder;
    }

    public void testSimpleProductOrder() throws IOException, IllegalStateException{

        ProductOrder testOrder = createSimpleProductOrder();

        Assert.assertEquals(3, testOrder.getUniqueParticipantCount());
        Assert.assertEquals(3, testOrder.getUniqueSampleCount());
        Assert.assertEquals(0, testOrder.getTumorNormalCounts().getNormalCount());
        Assert.assertEquals(0, testOrder.getTumorNormalCounts().getTumorCount());

        Assert.assertEquals(3, testOrder.getTotalSampleCount());
        Assert.assertEquals(0, testOrder.getDuplicateCount());
        Assert.assertEquals(3, testOrder.getBspSampleCount());
        Assert.assertEquals(3, testOrder.getMaleFemaleCounts().getFemaleCount());
        Assert.assertEquals(0, testOrder.getMaleFemaleCounts().getMaleCount());

        Assert.assertEquals(3, testOrder.getFingerprintCount());

        Assert.assertTrue(testOrder.getCountsByStockType().containsKey(BSPSampleDTO.ACTIVE_IND));
        Assert.assertEquals(3, testOrder.getCountsByStockType().get(BSPSampleDTO.ACTIVE_IND).intValue());

        //BSP data in BSP QA is different than this.
//        Assert.assertTrue( testOrder.getPrimaryDiseaseCount().containsKey( BSPSampleSearchServiceStub.SM_12CO4_DISEASE));
//        Assert.assertEquals( 0 , testOrder.getPrimaryDiseaseCount().get(BSPSampleSearchServiceStub.SM_12CO4_DISEASE).intValue());
//        Assert.assertTrue( testOrder.getPrimaryDiseaseCount ( ).containsKey( BSPSampleSearchServiceStub.SM_1P3XN_DISEASE));
//        Assert.assertEquals( 1 , testOrder.getPrimaryDiseaseCount ( ).get(BSPSampleSearchServiceStub.SM_1P3XN_DISEASE).intValue());

        Assert.assertEquals(3, testOrder.getReceivedSampleCount());

        Assert.assertEquals(3, testOrder.getActiveSampleCount());

        testOrder.submitProductOrder();

//        testOrder.closeProductOrder();

        Assert.assertTrue(StringUtils.isNotEmpty(testOrder.getJiraTicketKey()));

    }

    public void testSimpleNonBspProductOrder() {

        ProductOrder testOrder =
                new ProductOrder(TEST_CREATOR, "containerTest Product Order Test2",
                        null,
                        "newQuote",
                        AthenaClientServiceStub.createDummyProduct (),
                        createDummyResearchProject("Test Research Project"));
        testOrder.setSamples(ProductOrderTest.createSampleList("SM_12CO4,SM_1P3WY,SM_1P3XN", testOrder));

        Assert.assertEquals(testOrder.getUniqueSampleCount(), 3);

        Assert.assertEquals(3, testOrder.getTotalSampleCount());
        Assert.assertEquals(0, testOrder.getDuplicateCount());
        Assert.assertEquals(0, testOrder.getBspSampleCount());

//        try {
//            testOrder.getUniqueParticipantCount();
//            Assert.fail();
//        } catch (IllegalStateException ise) {
//
//        }
    }

    public static ResearchProject createDummyResearchProject(String researchProjectTitle) {
        ResearchProject dummyProject = new ResearchProject(1L, researchProjectTitle, "Simple test object for unit tests", true);

        dummyProject.setJiraTicketKey("RP-1");

        return dummyProject;
    }
}
