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

@Test(groups = TestGroups.DATABASE_FREE)
public class ConsentTest {
    private ResearchProject researchProject=null;
    private ProductOrder productOrder;
    private Consent consent1;
    private Consent consent2;
    @BeforeTest
    public void setUp() {
        productOrder = ProductOrderTestFactory.createDummyProductOrder();
        researchProject = productOrder.getResearchProject();
        consent1 = new Consent("My IRB Consent", Consent.Type.IRB, "IRB-12345");
        consent2 = new Consent("My Not Human Consent", Consent.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH, "213412");

    }

    public void testCreateConsent() {
        String identifier = "IRB-12345";
        Consent.Type irb = Consent.Type.IRB;
        String name = "My IRB Consent";
        Consent consent = new Consent(name, irb, identifier);

        Assert.assertEquals(consent.getBusinessKey(), identifier);
        Assert.assertEquals(consent.getIdentifier(), identifier);
        Assert.assertEquals(consent.getType(), irb);
    }
    public void testGetConsentFromProductOrder() {
        researchProject.addConsent(consent1, consent2);
        productOrder.setResearchProject(researchProject);

        Assert.assertTrue(productOrder.findAvailableConsents().contains(consent1));
        Assert.assertTrue(productOrder.findAvailableConsents().contains(consent2));
    }

    public void testAddConsentToProductOrder() {
        researchProject.addConsent(consent1, consent2);
        productOrder.addConsent(consent1);

        Assert.assertTrue(productOrder.getConsents().contains(consent1));
        Assert.assertFalse(productOrder.getConsents().contains(consent2));
    }

    public void testAddConsentToResearchProject() {
        researchProject.addConsent(consent1, consent2);

        Assert.assertTrue(researchProject.getConsents().contains(consent1));
        Assert.assertTrue(researchProject.getConsents().contains(consent2));
    }
}
