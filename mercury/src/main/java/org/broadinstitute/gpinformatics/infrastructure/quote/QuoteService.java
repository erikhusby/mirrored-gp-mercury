package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 *
 * Service to talk to the quote server.
 */
public interface QuoteService extends Serializable {
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException;

    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException;

    /**
     * This tells the quote server that some work has been done.
     *
     * @param quote The representation of the quote being updated.
     * @param quotePriceItem The price item on the quote that we are reporting some work on.
     * @param itemIsReplacing The item that the price item is replacing (null if it is primary or add-on).
     * @param reportedCompletionDate The completion date that we are reporting to the quote server. This determines
     *                               the actual price charged to the customer using quote rules.
     * @param numWorkUnits The number of pieces of work done on this price item.
     * @param callbackUrl The url back to mercury.
     * @param callbackParameterName The parameter name to get to the mercury billing object.
     * @param callbackParameterValue The value for this parameter.
     *
     * @return The work item id created by the quote server.
     */
    public String registerNewWork(
            Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing, Date reportedCompletionDate,
            double numWorkUnits, String callbackUrl, String callbackParameterName, String callbackParameterValue,
            BigDecimal priceAdjustment);


    public String registerNewSAPWork(
            Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing, Date reportedCompletionDate,
            double numWorkUnits, String callbackUrl, String callbackParameterName, String callbackParameterValue,
            BigDecimal priceAdjustment);
    /**
     * Get the quote for a particular quote identifier.
     *
     * @param alphaId The quote identifier.
     *
     * @return The quote representation.
     * @throws QuoteServerException Quote server problems.
     * @throws QuoteNotFoundException When the specified quote does not exist in the quote server.
     */
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException;

    /**
     * Get the quote and all its associated price items for a particular quote identifier.
     *
     * @param alphaId The quote identifier.
     *
     * @return The quote representation.
     * @throws QuoteServerException Quote server problems.
     * @throws QuoteNotFoundException When the specified quote does not exist in the quote server.
     */
    public Quote getQuoteWithPriceItems(String alphaId) throws QuoteServerException, QuoteNotFoundException;

    /**
     * Method to return all sources of funding from the quote server
     * @return The set of funding objects
     *
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException;

    /**
     * Method to return a list of PriceItems per Platform.
     * @return The full price list
     *
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType) throws QuoteServerException, QuoteNotFoundException;

    /**
     *  Method to return a list of all quotes
     */
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException;

    PriceList getPriceItemsForDate(List<QuoteImportItem> targetedPriceItemCriteria)
            throws QuoteServerException, QuoteNotFoundException;
}