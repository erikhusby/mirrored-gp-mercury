package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang.NotImplementedException;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePlatformType;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This is intended to be a "happy" quote service that accepts all billed work and returns work item ids that are
 * unique within a test run.  Currently some of the methods are set up to throw {@link NotImplementedException}s, but
 * that's only because the current clients of this class don't care about those methods.  These 'angry' methods could
 * be modified to be happier if new clients so require.
 */
@Alternative
@ApplicationScoped
class AcceptsAllWorkRegistrationsQuoteServiceStub implements QuoteService {

    public AcceptsAllWorkRegistrationsQuoteServiceStub(){}

    private static int counter = 0;

    public static final String WORK_ITEM_PREPEND = "workItemId\t";

    @Override
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        return new PriceList();
    }

    @Override
    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
        return new Quotes();
    }

    @Override
    public String registerNewWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                  Date reportedCompletionDate, BigDecimal numWorkUnits, String callbackUrl,
                                  String callbackParameterName, String callbackParameterValue,
                                  BigDecimal priceAdjustment) {
        return WORK_ITEM_PREPEND + (1000 + counter++);
    }

    @Override
    public String registerNewSAPWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                     Date reportedCompletionDate, BigDecimal numWorkUnits, String callbackUrl,
                                     String callbackParameterName, String callbackParameterValue,
                                     BigDecimal priceAdjustment) {
        return WORK_ITEM_PREPEND + (1000 + counter++);
    }

    @Override
    public Quote getQuoteByAlphaId(String alphaId, boolean forceDevQuoteRefresh)
        throws QuoteServerException, QuoteNotFoundException {
        FundingLevel level = new FundingLevel("100", Collections.singleton(new Funding(Funding.PURCHASE_ORDER,null, null)));
        QuoteFunding funding = new QuoteFunding(Collections.singleton(level));
        final Quote quote = new Quote("test1", funding, ApprovalStatus.FUNDED);

        return quote;
    }

    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return getQuoteByAlphaId(alphaId, false);
    }

    @Override
    public Quote getQuoteWithPriceItems(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        FundingLevel level = new FundingLevel("100", Collections.singleton(new Funding(Funding.PURCHASE_ORDER,null, null)));
        QuoteFunding funding = new QuoteFunding(Collections.singleton(level));
        final Quote quote = new Quote("test1", funding, ApprovalStatus.FUNDED);


        quote.setQuoteItems(Collections.singleton(new QuoteItem("test1", "priceitem1","Price Item", "10", "1000",
                "each", "Genomics Platform", "testing")));

        return quote;
    }

    @Override
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException{
        return null;
    }

    @Override
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {
        return null;
    }

    @Override
    public PriceList getPriceItemsForDate(List<QuoteImportItem> targetedPriceItemCriteria)
            throws QuoteServerException, QuoteNotFoundException {
        final PriceList allPriceItems = getAllPriceItems();
        for (QuoteImportItem quoteImportItem : targetedPriceItemCriteria) {

            final QuotePriceItem quotePriceItem =
                    QuotePriceItem.convertMercuryPriceItem(quoteImportItem.getPriceItem());
            quotePriceItem.setPrice("50.00");
            allPriceItems.add(quotePriceItem);
        }

        return allPriceItems;
    }

    @Override
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType)
            throws QuoteServerException, QuoteNotFoundException {
        return getAllPriceItems();
    }
}
