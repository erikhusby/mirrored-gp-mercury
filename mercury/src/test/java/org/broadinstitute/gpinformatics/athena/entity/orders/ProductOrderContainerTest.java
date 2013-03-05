package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.testng.Assert;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductOrderContainerTest extends Arquillian {

    private static final long TEST_CREATOR = 10950;

    @Inject
    private BSPUserList userList;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    public ProductOrder createSimpleProductOrder() throws Exception {
        return new ProductOrder(TEST_CREATOR, "containerTest Product Order Test1",
                ProductOrderTest.createSampleList("SM-1P3X9", "SM-1P3WY", "SM-1P3XN"),
                "newQuote", AthenaClientServiceStub.createDummyProduct("Exome Express", "partNumber"),
                createDummyResearchProject(userList, "Test Research Project"));
    }

    public void testSimpleProductOrder() throws Exception {

        ProductOrder testOrder = createSimpleProductOrder();

        Assert.assertEquals(3, testOrder.getUniqueParticipantCount());
        Assert.assertEquals(3, testOrder.getUniqueSampleCount());
        Assert.assertEquals(0, testOrder.getNormalCount());
        Assert.assertEquals(0, testOrder.getTumorCount());

        Assert.assertEquals(3, testOrder.getTotalSampleCount());
        Assert.assertEquals(0, testOrder.getDuplicateCount());
        Assert.assertEquals(3, testOrder.getBspSampleCount());
        Assert.assertEquals(3, testOrder.getFemaleCount());
        Assert.assertEquals(0, testOrder.getMaleCount());

        Assert.assertEquals(3, testOrder.getFingerprintCount());

        Assert.assertTrue(testOrder.getCountsByStockType().containsKey(ProductOrderSample.ACTIVE_IND));
        Assert.assertEquals(3, testOrder.getCountsByStockType().get(ProductOrderSample.ACTIVE_IND).intValue());

        // Test the sample order should be the same as when created.
        Assert.assertEquals("SM-1P3X9", testOrder.getSamples().get(0).getSampleName());
        Assert.assertEquals("SM-1P3WY", testOrder.getSamples().get(1).getSampleName());
        Assert.assertEquals("SM-1P3XN", testOrder.getSamples().get(2).getSampleName());

        Assert.assertEquals(3, testOrder.getReceivedSampleCount());

        Assert.assertEquals(3, testOrder.getActiveSampleCount());

        BspUser bspUser = new BspUser();
        bspUser.setUserId(TEST_CREATOR);
        testOrder.prepareToSave(bspUser, true);
        testOrder.placeOrder();

        Assert.assertTrue(StringUtils.isNotEmpty(testOrder.getJiraTicketKey()));
    }

    public void testSimpleNonBspProductOrder() throws Exception {

        ProductOrder testOrder =
                new ProductOrder(TEST_CREATOR, "containerTest Product Order Test2",
                        ProductOrderTest.createSampleList("SM_12CO4", "SM_1P3WY", "SM_1P3XN"),
                        "newQuote",
                        AthenaClientServiceStub.createDummyProduct("Exome Express", "partNumber"),
                        createDummyResearchProject(userList, "Test Research Project"));

        Assert.assertEquals(testOrder.getUniqueSampleCount(), 3);

        Assert.assertEquals(3, testOrder.getTotalSampleCount());
        Assert.assertEquals(0, testOrder.getDuplicateCount());
        Assert.assertEquals(0, testOrder.getBspSampleCount());
    }

    public static ResearchProject createDummyResearchProject(
            BSPUserList userList, String researchProjectTitle) throws IOException {
        ResearchProject dummyProject = new ResearchProject(TEST_CREATOR, researchProjectTitle, "Simple test object for unit tests", true);

        BspUser user = userList.getById(TEST_CREATOR);
        dummyProject.addPeople(RoleType.PM, Collections.singletonList(user));
        dummyProject.submit();
        return dummyProject;
    }
}
