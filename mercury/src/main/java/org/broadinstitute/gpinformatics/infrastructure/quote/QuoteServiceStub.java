package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Stub
@Alternative
@ApplicationScoped
public class QuoteServiceStub implements QuoteService {

    private static int workItemId = 1;
    private static volatile int invocationCount = 0;

    public QuoteServiceStub() {}

    @Override
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        invocationCount++;
        PriceList priceList;

        try {
            priceList = QuoteServerDataSnapshotter.readPriceListFromTestFile();
        }
        catch(Exception e) {
            throw new RuntimeException("Failed to read price list from disk",e);
        }
        return priceList;
    }


    @Override
    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
        Quotes quotes;
        try {
            quotes = QuoteServerDataSnapshotter.readAllQuotesFromTestFile();
        }
        catch(Exception e) {
            throw new RuntimeException("Failed to read quotes from disk",e);
        }
        return quotes;
    }


    @Override
    public String registerNewWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                  Date reportedCompletionDate,
                                  BigDecimal numWorkUnits, String callbackUrl, String callbackParameterName,
                                  String callbackParameterValue, BigDecimal priceAdjustment) {
        return Integer.toString(workItemId++);
    }

    @Override
    public String registerNewSAPWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                     Date reportedCompletionDate, BigDecimal numWorkUnits, String callbackUrl,
                                     String callbackParameterName, String callbackParameterValue,
                                     BigDecimal priceAdjustment) {
        return Integer.toString(workItemId++);
    }

    @Override
    public Quote getQuoteByAlphaId(String id) throws QuoteServerException, QuoteNotFoundException {
        Quote quote = null;

        for (Quote aQuote : getAllSequencingPlatformQuotes().getQuotes()) {
            if (aQuote.getAlphanumericId().equals(id)) {
                quote = aQuote;
                break;
            }
        }
        return quote;
    }

    @Override
    public Quote getQuoteWithPriceItems(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return getQuoteByAlphaId(alphaId);
    }

    @Override
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException{
        return null;
    }

    @Override
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {
        return getAllSequencingPlatformQuotes();
    }

    @Override
    public PriceList getPriceItemsForDate(List<QuoteImportItem> targetedPriceItemCriteria)
            throws QuoteServerException, QuoteNotFoundException {
        invocationCount++;
        PriceList priceList;

        try {
            priceList = QuoteServerDataSnapshotter.readPriceListFromTestFile();

            for (QuoteImportItem targetedPriceItemCriterion : targetedPriceItemCriteria) {
                final QuotePriceItem quotePriceItem =
                        QuotePriceItem.convertMercuryPriceItem(targetedPriceItemCriterion.getPriceItem());
                quotePriceItem.setPrice("50.00");
                priceList.add(quotePriceItem);
            }

        }
        catch(Exception e) {
            throw new RuntimeException("Failed to read price list from disk",e);
        }
        return priceList;
    }

    @Override
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType)
            throws QuoteServerException, QuoteNotFoundException {
        return getAllPriceItems();
    }

    public static void resetInvocationCount() {
        invocationCount = 0;
    }

    public static int getInvocationCount() {
        return invocationCount;
    }
}
