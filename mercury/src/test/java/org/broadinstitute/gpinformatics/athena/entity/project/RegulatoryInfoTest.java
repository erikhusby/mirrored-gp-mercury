/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class RegulatoryInfoTest {
    private ResearchProject researchProject=null;
    private ProductOrder productOrder;
    private RegulatoryInfo regulatoryInfo1;
    private RegulatoryInfo regulatoryInfo2;
    @BeforeTest
    public void setUp() {
        productOrder = ProductOrderTestFactory.createDummyProductOrder();
        researchProject = productOrder.getResearchProject();
        regulatoryInfo1 = new RegulatoryInfo("My IRB Consent", RegulatoryInfo.Type.IRB, "IRB-12345");
        regulatoryInfo2 = new RegulatoryInfo("My Not Human Consent", RegulatoryInfo.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH, "213412");
    }

    public void testCreateConsent() {
        String identifier = "IRB-12345";
        RegulatoryInfo.Type irb = RegulatoryInfo.Type.IRB;
        String name = "My IRB Consent";
        RegulatoryInfo regulatoryInfo = new RegulatoryInfo(name, irb, identifier);

        Assert.assertEquals(regulatoryInfo.getIdentifier(), identifier);
        Assert.assertEquals(regulatoryInfo.getType(), irb);
    }

    public void testGetConsentFromProductOrder() {
        researchProject.setRegulatoryInfos(Arrays.asList(regulatoryInfo1, regulatoryInfo2));
        productOrder.setResearchProject(researchProject);

        Assert.assertTrue(productOrder.findAvailableRegulatoryInfos().contains(regulatoryInfo1));
        Assert.assertTrue(productOrder.findAvailableRegulatoryInfos().contains(regulatoryInfo2));
    }

    public void testAddConsentToProductOrder() {
        researchProject.setRegulatoryInfos(Arrays.asList(regulatoryInfo1, regulatoryInfo2));
        productOrder.setRegulatoryInfos(Arrays.asList(regulatoryInfo1));

        Assert.assertTrue(productOrder.getRegulatoryInfos().contains(regulatoryInfo1));
        Assert.assertFalse(productOrder.getRegulatoryInfos().contains(regulatoryInfo2));
    }

    public void testAddConsentToResearchProject() {
        researchProject.setRegulatoryInfos(Arrays.asList(regulatoryInfo1, regulatoryInfo2));

        Assert.assertTrue(researchProject.getRegulatoryInfos().contains(regulatoryInfo1));
        Assert.assertTrue(researchProject.getRegulatoryInfos().contains(regulatoryInfo2));
    }

    public void testRegulatoryInfoTypeForOrspServiceId() {
        for (RegulatoryInfo.Type type : RegulatoryInfo.Type.values()) {
            assertThat(RegulatoryInfo.Type.forOrspServiceId(type.getOrspServiceId()), equalTo(type));
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRegulatoryInfoTypeForOrspServiceIdUnknown() {
        RegulatoryInfo.Type.forOrspServiceId("unknown");
    }
}
