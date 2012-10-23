package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.HashSet;


/**
 *
 * Application-wide cache of all price items, potentially filtering by platform. {@link @Named} and
 * {@link javax.enterprise.context.ApplicationScoped} annotated to be directly usable from JSF templates or backing
 * beans
 *
 */
@Named
@ApplicationScoped
public class PriceListCache {
    
    private PriceList priceList;

    @Inject
    private PMBQuoteService quoteService;

    public PriceListCache() {}

    /**
     * Not sure we really need this constructor any more except there are tests using it
     *
     * @param priceList
     */
    public PriceListCache(PriceList priceList) {
        if (priceList == null) {
             throw new NullPointerException("priceList cannot be null."); 
        }
        this.priceList = priceList;
    }


    /**
     * Pull down and cache the full list of prices from the quote server
     *
     * @return
     */
    public PriceList getPriceList() {
        try {
            if (priceList == null) {
                priceList = quoteService.getAllPriceItems();
            }
            return priceList;
        }
        catch (QuoteServerException e) {
            throw new RuntimeException(e);
        }
        catch (QuoteNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Drill into the {@link PriceList} to save a level of EL'ing
     *
     * @return
     */
    public Collection<PriceItem> getPriceItems() {
        return getPriceList().getPriceItems();
    }


    /**
     * Filter {@link PriceItem}s by known platforms
     *
     * @param quotePlatformType
     * @return
     */
    public Collection<PriceItem> getPriceItemsByPlatform(QuotePlatformType quotePlatformType) {
        Collection<PriceItem> priceItems = new HashSet<PriceItem>();
        for (PriceItem priceItem : getPriceList().getPriceItems()) {
            if (quotePlatformType.getPlatformName().equalsIgnoreCase(priceItem.getPlatform())) {
                priceItems.add(priceItem);
            }
        }
        return priceItems;
    }

    
    public Collection<PriceItem> getGSPPriceItems() {
        return getPriceItemsByPlatform(QuotePlatformType.SEQ);
    }
}
