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
import java.util.Arrays;
import java.util.Collection;
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

    @Test(enabled = false)
    public void testCustomerSearch() {

        String testUser = "ScottmATT@broadinstitute.org";
        String testBadUser = "scottnobody@broadInstitute.org";

        Funding fundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        fundingDefined.setPurchaseOrderContact(testUser);
        FundingLevel fundingLevel = new FundingLevel("100", Collections.singleton(fundingDefined));
        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));
        Quote testGoodQuote = new Quote("GPTest", quoteFunding, ApprovalStatus.FUNDED);


        Funding badContactFundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        badContactFundingDefined.setPurchaseOrderContact(testBadUser);
        FundingLevel badContactPurchaseOrderFundingLevel = new FundingLevel("100", Collections.singleton(badContactFundingDefined));
        QuoteFunding quoteFundingBadContact = new QuoteFunding(Collections.singleton(badContactPurchaseOrderFundingLevel));
        Quote testBadContactQuote = new Quote("GPBadContact", quoteFundingBadContact, ApprovalStatus.FUNDED);


        Funding test3POFundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        test3POFundingDefined.setPurchaseOrderContact(testUser);
        FundingLevel test3PurchaseOrderFundingLevel = new FundingLevel("50", Collections.singleton(test3POFundingDefined));

        Funding test3PO2FundingDefined = new Funding(Funding.PURCHASE_ORDER, null, null);
        test3PO2FundingDefined.setPurchaseOrderContact("Second"+testUser);
        FundingLevel test3PO2FundingLevel = new FundingLevel("50", Collections.singleton(test3PO2FundingDefined));
        QuoteFunding test3Funding = new QuoteFunding(Arrays.asList(new FundingLevel[]{test3PurchaseOrderFundingLevel,test3PO2FundingLevel}));
        Quote testMultipleLevelQuote = new Quote("GPTest", test3Funding, ApprovalStatus.FUNDED);


        Funding testFRFundingDefind = new Funding(Funding.FUNDS_RESERVATION, null, null);

        FundingLevel testFRFundingLevel = new FundingLevel("50", Collections.singleton(testFRFundingDefind));
        QuoteFunding testFRFunding = new QuoteFunding(Collections.singletonList(testFRFundingLevel));
        Quote testFRQuote = new Quote("GPTest", testFRFunding, ApprovalStatus.FUNDED);

        try {
            String goodUserNumber = sapIntegrationClient.findCustomer(
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                    testGoodQuote.getFirstRelevantFundingLevel());
            Assert.assertEquals(goodUserNumber , "0000300325");
            goodUserNumber = sapIntegrationClient.findCustomer(
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES,
                    testGoodQuote.getFirstRelevantFundingLevel());
            Assert.assertEquals(goodUserNumber , "0000300325");
        } catch (SAPIntegrationException e) {
            Assert.fail(e.getMessage());
        }

        try {


            for (Funding funding :testGoodQuote.getFirstRelevantFundingLevel().getFunding()) {
                funding.setPurchaseOrderContact("zarasearle@broadinstitute.org");
                String dupeName2 = sapIntegrationClient.findCustomer(
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                        testGoodQuote.getFirstRelevantFundingLevel());
                Assert.assertEquals(dupeName2 , "0000300022");

                funding.setPurchaseOrderContact("zarasearle1@broadinstitute.org");
                String dupeNameOne = sapIntegrationClient.findCustomer(
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                        testGoodQuote.getFirstRelevantFundingLevel());
                Assert.assertEquals(dupeNameOne , "0000300023");
            }

        } catch (SAPIntegrationException e) {
            Assert.fail(e.getMessage());
        }

//        try {
//
//            testGoodQuote.getFirstRelevantFundingLevel().getFunding().setPurchaseOrderContact("SusanM@gmail.com");
//            String dupeName3 = sapIntegrationClient.findCustomer(
//                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES,
//                    testGoodQuote.getFirstRelevantFundingLevel());
//            Assert.assertNotNull(dupeName3);
////            Assert.assertEquals(dupeName3 , "0000300022");
//
//            testGoodQuote.getFirstRelevantFundingLevel().getFunding().setPurchaseOrderContact("SusanM@yahoo.com");
//            String dupeName4 = sapIntegrationClient.findCustomer(
//                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES,
//                    testGoodQuote.getFirstRelevantFundingLevel());
//            Assert.assertNotNull(dupeName4);
////            Assert.assertEquals(dupeName4 , "0000300023");
//
//        } catch (SAPIntegrationException e) {
//            Assert.fail(e.getMessage());
//        }

        try {
            String missingCustomerNumber = sapIntegrationClient.findCustomer(
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD, testBadContactQuote.getFirstRelevantFundingLevel());
            Assert.fail("This should have thrown a system error");
        } catch (SAPIntegrationException e) {
            log.debug(e.getMessage());
            Assert.assertTrue(e.getMessage().contains("the email address specified on the Quote is not attached to any SAP Customer account."));
        }

        try {

            for (Funding funding : testBadContactQuote.getFirstRelevantFundingLevel().getFunding()) {
                funding.setPurchaseOrderContact("Shriekrvce@gmail.com");

                String duplicateCustomerNumber = sapIntegrationClient.findCustomer(
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                        testBadContactQuote.getFirstRelevantFundingLevel());
                Assert.fail("This should have thrown a system error");
            }
        } catch (SAPIntegrationException e) {
            log.debug(e.getMessage());
            Assert.assertTrue(e.getMessage().contains("the Quote is associated with more than 1 SAP Customer account"));
        }


//        try {
//            testBadContactQuote.getFirstRelevantFundingLevel().getFunding().setPurchaseOrderContact("ScottM@gmail.com");
//
//            String duplicateCustomerNumber = sapIntegrationClient.findCustomer(
//                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES,
//                    testBadContactQuote.getFirstRelevantFundingLevel());
//            Assert.fail("This should have thrown a system error");
//        } catch (SAPIntegrationException e) {
//            log.debug(e.getMessage());
//            Assert.assertTrue(e.getMessage().contains("the Quote is associated with more than 1 SAP Customer account"));
//        }
//
        try {
            String invalidQuote = sapIntegrationClient.findCustomer(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                    testMultipleLevelQuote.getFirstRelevantFundingLevel());
            Assert.fail("Should not have been able to find a customer with multiple funding levels");
        } catch (SAPIntegrationException e) {
            Assert.assertEquals(e.getMessage(),"Unable to continue with SAP.  The associated quote has either too few or too many funding sources");
        }

        try {
            String fundsReservationQuote = sapIntegrationClient.findCustomer(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                    testFRQuote.getFirstRelevantFundingLevel());
            Assert.assertNull(fundsReservationQuote);
        } catch (SAPIntegrationException e) {
            Assert.fail("An exception is not expected in this scenario");
        }
    }
}
