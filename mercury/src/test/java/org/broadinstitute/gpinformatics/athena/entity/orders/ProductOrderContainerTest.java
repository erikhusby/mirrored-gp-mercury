package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductOrderContainerTest extends Arquillian {
    @Inject
    private BSPUserList userList;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    public ProductOrder createSimpleProductOrder() throws Exception {
        return new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "containerTest Product Order Test1",
                ProductOrderSampleTestFactory.createSampleList("SM-1P3X9", "SM-1P3WY", "SM-1P3XN"),
                "newQuote", ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber"),
                ResearchProjectTestFactory.createDummyResearchProject(userList, "Test Research Project"));
    }

    public void testSimpleProductOrder() throws Exception {
        ProductOrder testOrder = createSimpleProductOrder();

        Assert.assertEquals(testOrder.getUniqueParticipantCount(), 3);
        Assert.assertEquals(testOrder.getUniqueSampleCount(), 3);
        Assert.assertEquals(testOrder.getNormalCount(), 0);
        Assert.assertEquals(testOrder.getTumorCount(), 0);

        Assert.assertEquals(testOrder.getTotalSampleCount(), 3);
        Assert.assertEquals(testOrder.getDuplicateCount(), 0);
        Assert.assertEquals(testOrder.getBspSampleCount(), 3);
        Assert.assertEquals(testOrder.getFemaleCount(), 3);
        Assert.assertEquals(testOrder.getMaleCount(), 0);

        Assert.assertEquals(testOrder.getFingerprintCount(), 3);

        Assert.assertTrue(testOrder.getCountsByStockType().containsKey(ProductOrderSample.ACTIVE_IND));
        Assert.assertEquals(testOrder.getCountsByStockType().get(ProductOrderSample.ACTIVE_IND).intValue(), 3);

        // Test the sample order should be the same as when created.
        Assert.assertEquals(testOrder.getSamples().get(0).getSampleName(), "SM-1P3X9");
        Assert.assertEquals(testOrder.getSamples().get(1).getSampleName(), "SM-1P3WY");
        Assert.assertEquals(testOrder.getSamples().get(2).getSampleName(), "SM-1P3XN");

        Assert.assertEquals(testOrder.getReceivedSampleCount(), 3);

        Assert.assertEquals(testOrder.getActiveSampleCount(), 3);

        BspUser bspUser = new BspUser();
        bspUser.setUserId(ResearchProjectTestFactory.TEST_CREATOR);
        testOrder.prepareToSave(bspUser, ProductOrder.SaveType.creating);
        testOrder.placeOrder();

        Assert.assertTrue(StringUtils.isNotEmpty(testOrder.getJiraTicketKey()));
    }

    public void testSimpleNonBspProductOrder() throws Exception {
        ProductOrder testOrder =
                new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "containerTest Product Order Test2",
                        ProductOrderSampleTestFactory.createSampleList("SM_12CO4", "SM_1P3WY", "SM_1P3XN"),
                        "newQuote",
                        ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber"),
                        ResearchProjectTestFactory.createDummyResearchProject(userList, "Test Research Project"));

        Assert.assertEquals(testOrder.getUniqueSampleCount(), 3);

        Assert.assertEquals(testOrder.getTotalSampleCount(), 3);
        Assert.assertEquals(testOrder.getDuplicateCount(), 0);
        Assert.assertEquals(testOrder.getBspSampleCount(), 0);
    }

}
