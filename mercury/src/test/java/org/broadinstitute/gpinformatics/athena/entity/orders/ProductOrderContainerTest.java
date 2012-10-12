package org.broadinstitute.gpinformatics.athena.entity.orders;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.authentication.AuthenticationServiceTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;

/**
 * @author Scott Matthews
 *         Date: 10/11/12
 *         Time: 4:38 PM
 */
@Test (groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductOrderContainerTest extends ContainerTest {

    public void testSimpleProductOrder() throws IOException{

        ProductOrder testOrder =
                new ProductOrder("containerTest Product Order Test1",
                                 ProductOrderTest.createSampleList("SM-12CO4,SM-1P3WY,SM-1P3XN",
                                                                   new HashSet<BillableItem> ()),
                                 "newQuote",
                                 ProductOrderTest.createDummyProduct(),
                                 createDummyResearchProject ("Test Research Project" ) );

        Assert.assertEquals ( testOrder.getUniqueParticipantCount(), 1 );
        Assert.assertEquals ( testOrder.getUniqueSampleCount ( ), 3 );
        Assert.assertEquals ( 2 , testOrder.getTumorNormalCounts().getNormalCount ( ));
        Assert.assertEquals ( 1 , testOrder.getTumorNormalCounts().getTumorCount ( ));

        Assert.assertEquals ( 3 , testOrder.getTotalSampleCount());
        Assert.assertEquals ( 0 , testOrder.getDuplicateCount());
        Assert.assertEquals ( 3 , testOrder.getBspSampleCount());
        Assert.assertEquals ( 1 , testOrder.getMaleFemaleCounts().getFemaleCount());
        Assert.assertEquals ( 2 , testOrder.getMaleFemaleCounts().getMaleCount());

        Assert.assertTrue ( testOrder.getCountsByStockType ( ).containsKey ( BSPSampleDTO.ACTIVE_IND ) );
        Assert.assertEquals ( 2, testOrder.getCountsByStockType ( ).get ( BSPSampleDTO.ACTIVE_IND ).intValue ( ) );

        Assert.assertTrue( testOrder.getPrimaryDiseaseCount().containsKey( BSPSampleSearchServiceStub.SM_12CO4_DISEASE));
        Assert.assertEquals( 2 , testOrder.getPrimaryDiseaseCount().get(BSPSampleSearchServiceStub.SM_12CO4_DISEASE).intValue());

        Assert.assertTrue( testOrder.getPrimaryDiseaseCount ( ).containsKey( BSPSampleSearchServiceStub.SM_1P3XN_DISEASE));
        Assert.assertEquals( 1 , testOrder.getPrimaryDiseaseCount ( ).get(BSPSampleSearchServiceStub.SM_1P3XN_DISEASE).intValue());

        Assert.assertEquals( 2, testOrder.getReceivedSampleCount());

        Assert.assertEquals( 2 , testOrder.getActiveSampleCount());

        testOrder.submitProductOrder();

        Assert.assertTrue(StringUtils.isNotEmpty(testOrder.getJiraTicketKey()));

    }
    public void testSimpleNonBspProductOrder() {

        ProductOrder testOrder =
                new ProductOrder("containerTest Product Order Test2",
                                 ProductOrderTest.createSampleList("SM_12CO4,SM_1P3WY,SM_1P3XN",
                                                                   new HashSet<BillableItem> ()),
                                 "newQuote",
                                 ProductOrderTest.createDummyProduct(),
                                 createDummyResearchProject ("Test Research Project" ) );

        Assert.assertEquals ( testOrder.getUniqueSampleCount ( ), 3 );

        Assert.assertEquals ( 3 , testOrder.getTotalSampleCount());
        Assert.assertEquals ( 0 , testOrder.getDuplicateCount());
        Assert.assertEquals ( 0 , testOrder.getBspSampleCount ( ));

        try {
            testOrder.getUniqueParticipantCount();
            Assert.fail();
        } catch (IllegalStateException ise) {

        }


    }

    public static ResearchProject createDummyResearchProject ( String researchProjectTitle ) {
        ResearchProject dummyProject = new ResearchProject (1L, researchProjectTitle,"Simple test object for unit tests", true);

        dummyProject.setJiraTicketKey("RP-123");

        return dummyProject ;
    }

}
