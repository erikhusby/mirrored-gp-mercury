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

    public void testQuoteFundingDifferingOnlyByFundsReservationNumber() throws Exception {
        // Tests a case where a quote was mistakenly being identified as being eligible for SAP when it was not. The
        // cause turned out to be the equals method not taking fundsReservationNumber into account
        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        final String quoteId = "GPLBK";
        Quote testQuote = stubbedQuoteService.getQuoteByAlphaId(quoteId);
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

    public void testUnFundedQuote() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote eligibleQuote = stubbedQuoteService.getQuoteByAlphaId("STC3ZW");
        FundingLevel fundingLevel = eligibleQuote.getQuoteFunding().getFundingLevel().iterator().next();
        fundingLevel.setPercent("0");

        try {
            Assert.assertFalse(eligibleQuote.isFunded());
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
