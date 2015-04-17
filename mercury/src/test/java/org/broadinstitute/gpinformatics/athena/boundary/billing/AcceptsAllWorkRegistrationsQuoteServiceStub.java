package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang.NotImplementedException;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;

import javax.enterprise.inject.Alternative;
import java.util.Date;

/**
 * This is intended to be a "happy" quote service that accepts all billed work and returns work item ids that are
 * unique within a test run.  Currently some of the methods are set up to throw {@link NotImplementedException}s, but
 * that's only because the current clients of this class don't care about those methods.  These 'angry' methods could
 * be modified to be happier if new clients so require.
 */
@Alternative
class AcceptsAllWorkRegistrationsQuoteServiceStub implements QuoteService {

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
                                  Date reportedCompletionDate, double numWorkUnits, String callbackUrl,
                                  String callbackParameterName, String callbackParameterValue) {
        return WORK_ITEM_PREPEND + (1000 + counter++);
    }

    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return new Quote();
    }

    @Override
    public Quote getQuoteWithPriceItems(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return new Quote();
    }
}
