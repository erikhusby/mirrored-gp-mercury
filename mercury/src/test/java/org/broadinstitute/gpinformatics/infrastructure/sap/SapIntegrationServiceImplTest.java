package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;

@Test(groups = STANDARD)
public class SapIntegrationServiceImplTest extends Arquillian {

    @Inject
    SapIntegrationService sapIntegrationClient;

    private final static Log log = LogFactory.getLog(SapIntegrationServiceImplTest.class);

    @BeforeMethod
    public void setUp() {
        if (sapIntegrationClient == null) {
            return;
        }
    }

    @AfterMethod
    public void tearDown() {
        if (sapIntegrationClient == null) {
            return;
        }
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testCustomerSearch() {

        String testUser = "Scott.G.MATThEws@GMail.CoM";
        String testBadUser = "scottnobody@broadInstitute.org";

        Funding fundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        fundingDefined.setPurchaseOrderContact(testUser);
        FundingLevel fundingLevel = new FundingLevel("100",fundingDefined);
        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));
        Quote testGoodQuote = new Quote("GPTest", quoteFunding, ApprovalStatus.FUNDED);

        Funding badContactFundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        badContactFundingDefined.setPurchaseOrderContact(testBadUser);
        FundingLevel badContactPurchaseOrderFundingLevel = new FundingLevel("100",badContactFundingDefined);
        QuoteFunding quoteFundingBadContact = new QuoteFunding(Collections.singleton(badContactPurchaseOrderFundingLevel));
        Quote testBadContactQuote = new Quote("GPBadContact", quoteFundingBadContact, ApprovalStatus.FUNDED);


        Funding test3POFundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        test3POFundingDefined.setPurchaseOrderContact(testUser);
        FundingLevel test3PurchaseOrderFundingLevel = new FundingLevel("50",test3POFundingDefined);

        Funding test3PO2FundingDefined = new Funding(Funding.PURCHASE_ORDER, null, null);
        test3PO2FundingDefined.setPurchaseOrderContact("Second"+testUser);
        FundingLevel test3PO2FundingLevel = new FundingLevel("50", test3PO2FundingDefined);
        QuoteFunding test3Funding = new QuoteFunding(Arrays.asList(new FundingLevel[]{test3PurchaseOrderFundingLevel,test3PO2FundingLevel}));
        Quote testMultipleLevelQuote = new Quote("GPTest", test3Funding, ApprovalStatus.FUNDED);

        try {
            String badUserNumber = sapIntegrationClient.findCustomer(testBadContactQuote , SapIntegrationClientImpl.BROAD_COMPANY_CODE);
            Assert.fail("This should have thrown a system error");
        } catch (SAPIntegrationException e) {
            log.debug(e.getMessage());
            Assert.assertTrue(e.getMessage().contains("the email address specified on the Quote or Mercury PDO is not attached to any SAP Customer account."));
        }

        try {
            String badQuote = sapIntegrationClient.findCustomer(testMultipleLevelQuote, SapIntegrationClientImpl.BROAD_COMPANY_CODE);
            Assert.fail("Should not have been able to find a customer with multiple funding levels");
        } catch (SAPIntegrationException e) {
            Assert.assertEquals(e.getMessage(),"Unable to continue with SAP.  The associated quote has multiple funding sources");
        }

        try {
            String goodUserNumber = sapIntegrationClient.findCustomer(testGoodQuote, SapIntegrationClientImpl.BROAD_COMPANY_CODE);
            Assert.assertEquals(goodUserNumber , "0000300325");
        } catch (SAPIntegrationException e) {
            Assert.fail(e.getMessage());
        }
    }
}
