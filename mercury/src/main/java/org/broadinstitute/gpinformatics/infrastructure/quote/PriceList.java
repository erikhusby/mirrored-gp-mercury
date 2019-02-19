package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.lang3.time.DateUtils;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@XmlRootElement(name = "response")
public class PriceList {
    
    @XmlElement(name = "priceItem")
    private final Collection<QuotePriceItem> quotePriceItems = new ArrayList<>();

    public PriceList() {}
    
    public Collection<QuotePriceItem> getQuotePriceItems() {
        return quotePriceItems;
    }
    
    public void add(@Nonnull QuotePriceItem quotePriceItem) {
        quotePriceItems.add(quotePriceItem);
    }

    public QuotePriceItem findByKeyFields(String platform, String category, String name) {
        return findByKeyFields(platform, category, name, null);
    }
    public QuotePriceItem findByKeyFields(String platform, String category, String name, Date effectiveDate) {

        QuotePriceItem foundItem = null;

        for (QuotePriceItem quotePriceItem : getQuotePriceItems()) {
            if (quotePriceItem.getPlatformName().equals(platform) &&
                quotePriceItem.getCategoryName().equals(category) &&
                quotePriceItem.getName().equals(name)) {
                if(effectiveDate == null ||
                   (DateUtils.truncate(effectiveDate, Calendar.DATE).equals(quotePriceItem.getEffectiveDate()))) {
                    foundItem = quotePriceItem;
                    break;
                }
            }
        }

        return foundItem;
    }

    public QuotePriceItem findByKeyFields(PriceItem priceItem) {
        return findByKeyFields(priceItem.getPlatform(), priceItem.getCategory(), priceItem.getName());
    }



    /**
     * Given a price item, this method will compare the price of the price item on the price list with the Quote
     * line item (if one exists) on a given quote to see which price is lower.  The lower of the two is returned
     *
     * @param primaryPriceItem Price item defined on a product
     * @param orderQuote       Quote associated with the product order from which the product that defined the
     *                         price item is associated
     * @return Lowest price between the pricelist item and the quote item (if one exists)
     * @throws InvalidProductException   Thrown if the price item from the product orders product is not found on the
     * price list
     */
    public String getEffectivePrice(PriceItem primaryPriceItem, Quote orderQuote, Date workCompleteDate) throws InvalidProductException {

        final QuotePriceItem cachedPriceItem = findByKeyFields(primaryPriceItem);
        if(cachedPriceItem == null) {
            throw new InvalidProductException("The price item "+primaryPriceItem.getDisplayName()+" does not exist");
        }
        return getEffectivePrice(cachedPriceItem, orderQuote, workCompleteDate);
    }

    private String getEffectivePrice(QuotePriceItem cachedPriceItem, Quote orderQuote, Date workCompleteDate) {
        String price = cachedPriceItem.getPrice();
        QuoteItem foundMatchingQuoteItem = orderQuote.findCachedQuoteItem(cachedPriceItem.getPlatformName(),
                cachedPriceItem.getCategoryName(), cachedPriceItem.getName());
        if (foundMatchingQuoteItem != null && workCompleteDate.before(orderQuote.getExpirationDate())) {
            if (new BigDecimal(foundMatchingQuoteItem .getPrice()).compareTo(new BigDecimal(cachedPriceItem.getPrice())) < 0) {
                price = foundMatchingQuoteItem .getPrice();
            }
        }
        return price;
    }

    public Collection<QuotePriceItem> getReplacementPriceItems(PriceItem primaryPriceItem) {
        try {
            QuotePriceItem quotePriceItem = findByKeyFields(primaryPriceItem);

            return quotePriceItem.getReplacementItems().getQuotePriceItems();
        } catch (Throwable t) {
            // Since this is coming from the quote server, we will just show nothing when there are any errors.
            return Collections.emptyList();
        }

    }

}
