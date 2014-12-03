package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collection;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class ProductOrderContainerTest extends Arquillian {
    @Inject
    private BSPUserList userList;

    @Inject
    ResearchProjectEjb researchProjectEjb;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Inject
    JiraService jiraService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    public static ProductOrder createSimpleProductOrder(ResearchProjectEjb researchProjectEjb,
                                                        BSPUserList userList) throws Exception {
        return new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "containerTest Product Order Test1",
                ProductOrderSampleTestFactory.createSampleList("SM-1P3X9", "SM-1P3WY", "SM-1P3XN"),
                "newQuote", ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber"),
                ResearchProjectTestFactory.createDummyResearchProject(researchProjectEjb, userList, "Test Research Project"));
    }

    public void testSimpleProductOrder() throws Exception {
        ProductOrder testOrder = createSimpleProductOrder(researchProjectEjb, userList);

        Assert.assertEquals(testOrder.getUniqueParticipantCount(), 3);
        Assert.assertEquals(testOrder.getUniqueSampleCount(), 3);
        Assert.assertEquals(testOrder.getNormalCount(), 0);
        Assert.assertEquals(testOrder.getTumorCount(), 0);

        Assert.assertEquals(testOrder.getTotalSampleCount(), 3);
        Assert.assertEquals(testOrder.getDuplicateCount(), 0);
        Assert.assertEquals(testOrder.getSampleCountForSource(MercurySample.MetadataSource.BSP), 3);
        Assert.assertEquals(testOrder.getFemaleCount(), 3);
        Assert.assertEquals(testOrder.getMaleCount(), 0);

        Assert.assertTrue(testOrder.getCountsByStockType().containsKey(ProductOrderSample.ACTIVE_IND));
        Assert.assertEquals(testOrder.getCountsByStockType().get(ProductOrderSample.ACTIVE_IND).intValue(), 3);

        // Test the sample order should be the same as when created.
        Assert.assertEquals(testOrder.getSamples().get(0).getName(), "SM-1P3X9");
        Assert.assertEquals(testOrder.getSamples().get(1).getName(), "SM-1P3WY");
        Assert.assertEquals(testOrder.getSamples().get(2).getName(), "SM-1P3XN");

        Assert.assertEquals(testOrder.getReceivedSampleCount(), 3);

        Assert.assertEquals(testOrder.getActiveSampleCount(), 3);
    }

    public void testSimpleProductOrderWithConsent() throws Exception {
        ProductOrder testOrder =  ProductOrderDBTestFactory.createTestExExProductOrder(researchProjectDao, productDao);
        testOrder.setCreatedBy(10950L);

        Collection<RegulatoryInfo> availableRegulatoryInfos = testOrder.findAvailableRegulatoryInfos();
        Assert.assertFalse(availableRegulatoryInfos.isEmpty());
        testOrder.setRegulatoryInfos(availableRegulatoryInfos);

        BspUser bspUser = new BspUser();
        bspUser.setUserId(10950L);
//        testOrder.setCreatedBy(10950l);
        testOrder.prepareToSave(bspUser, ProductOrder.SaveType.CREATING);
//        ProductOrderJiraUtil.createIssueForOrder(testOrder, jiraService);
        productOrderDao.persist(testOrder.getProduct());
        Assert.assertTrue(StringUtils.isNotEmpty(testOrder.getJiraTicketKey()));
}

    public void testSimpleNonBspProductOrder() throws Exception {
        ProductOrder testOrder =
                new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "containerTest Product Order Test2",
                        ProductOrderSampleTestFactory.createSampleList("SM_12CO4", "SM_1P3WY", "SM_1P3XN"),
                        "newQuote",
                        ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber"),
                        ResearchProjectTestFactory.createDummyResearchProject(researchProjectEjb, userList, "Test Research Project"));

        Assert.assertEquals(testOrder.getUniqueSampleCount(), 3);

        Assert.assertEquals(testOrder.getTotalSampleCount(), 3);
        Assert.assertEquals(testOrder.getDuplicateCount(), 0);
        Assert.assertEquals(testOrder.getSampleCount(), 0);
    }

}
