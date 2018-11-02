package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class QuoteTest {


    public void testQuoteValuesPurchaseOrder() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        final String quoteId = "MPG1X6";
        Quote testQuote = stubbedQuoteService.getQuoteByAlphaId(quoteId);

        assertThat(testQuote.getAlphanumericId(), is(equalToIgnoringCase(quoteId)));
        assertThat(testQuote.getApprovalStatus(), is(equalTo(ApprovalStatus.FUNDED)));
        assertThat(testQuote.getExpired(), is(equalTo(true)));
        final FundingLevel firstRelevantFundingLevel = testQuote.getFirstRelevantFundingLevel();
        assertThat(firstRelevantFundingLevel, is(notNullValue()));
        assertThat(testQuote.getId(), is(equalTo("2490")));
        assertThat(testQuote.getName(), is(equalTo("BSP_Pfizer_CAMP_batch4_080510")));
        assertThat(testQuote.getQuoteItems(), hasSize(6));
        assertThat(testQuote.getExpirationDate(), is(notNullValue()));

        assertThat(testQuote.isFunded(), is(true));

    }

    public void testQuoteValuesFundsReservation() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        final String quoteId = "MPG20W";
        Quote testQuote = stubbedQuoteService.getQuoteByAlphaId(quoteId);

        assertThat(testQuote.getAlphanumericId(), is(equalToIgnoringCase(quoteId)));
        assertThat(testQuote.getApprovalStatus(), is(equalTo(ApprovalStatus.FUNDED)));
        assertThat(testQuote.getExpired(), is(equalTo(true)));
        final FundingLevel firstRelevantFundingLevel = testQuote.getFirstRelevantFundingLevel();
        assertThat(firstRelevantFundingLevel, is(notNullValue()));
        final Funding funding = firstRelevantFundingLevel.getFunding().iterator().next();
        assertThat(funding.getGrantStartDate(), is(notNullValue()));
        assertThat(funding.getGrantEndDate(), is(notNullValue()));

        assertThat(testQuote.getId(), is(equalTo("2624")));
        assertThat(testQuote.getName(), is(equalTo("MCKD1_kits_plating")));
        assertThat(testQuote.getQuoteItems(), hasSize(5));
        assertThat(testQuote.getExpirationDate(), is(notNullValue()));

        assertThat(testQuote.isFunded(), is(false));

        Calendar cal = Calendar.getInstance();
        cal.set(2013, Calendar.JANUARY, 9); //Year, month, day of month
        Date date = cal.getTime();

        assertThat(testQuote.isFunded(date), is(true));

    }

    public void testQuoteIneligibilitySplitFunding() throws Exception{
        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote gp87Uquote = stubbedQuoteService.getQuoteByAlphaId("GP87U");

        boolean quoteEligibility = true;

        final FundingLevel firstRelevantFundingLevel = gp87Uquote.getFirstRelevantFundingLevel();
        assertThat(firstRelevantFundingLevel, is(nullValue()));

        try {
            quoteEligibility = gp87Uquote.isFunded();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }

    }

    public void testTrueSplitFundingEligibility() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote splitFundedQuote = stubbedQuoteService.getQuoteByAlphaId("GXP2B1");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = splitFundedQuote.isFunded();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }
    public void testSplitCostObjectEligibility() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote splitCostObjectQuote = stubbedQuoteService.getQuoteByAlphaId("STCIL1");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = splitCostObjectQuote.isFunded();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }
    public void testTrueEligibleQuote() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote eligibleQuote = stubbedQuoteService.getQuoteByAlphaId("STC3ZW");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = eligibleQuote.isFunded();
            Assert.assertTrue(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }

    public void testIssuedNoFundingIneligibleQuote() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote eligibleQuote = stubbedQuoteService.getQuoteByAlphaId("BSP2A3");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = eligibleQuote.isFunded();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }
    public void testIssuedNoFunding2IneligibleQuote() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote eligibleQuote = stubbedQuoteService.getQuoteByAlphaId("BSP1CK");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = eligibleQuote.isFunded();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }
    public void testInligibleCostObjectQuote() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote eligibleQuote = stubbedQuoteService.getQuoteByAlphaId("MPG20W");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = eligibleQuote.isFunded();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }

}
