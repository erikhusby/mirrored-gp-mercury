package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


/**
 *
 * Application-wide cache of all price items, potentially filtering by platform. {@link @Named} and
 * {@link javax.enterprise.context.ApplicationScoped} annotated to be directly usable from JSF templates or backing
 * beans
 *
 */
@Named
@ApplicationScoped
public class PriceListCache implements Serializable {
    
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
            if (quotePlatformType.getPlatformName().equalsIgnoreCase(priceItem.getPlatformName())) {
                priceItems.add(priceItem);
            }
        }
        return priceItems;
    }

    
    public Collection<PriceItem> getGSPPriceItems() {
        return getPriceItemsByPlatform(QuotePlatformType.SEQ);
    }


    public List<PriceItem> searchPriceItems(Collection<PriceItem> priceItems, String query) {
        List<PriceItem> results = new ArrayList<PriceItem>();
        String lowerQuery = query.toLowerCase();

        // Currently searching all price items, not filtering by platform or anything else
        if (priceItems != null) {
            for (PriceItem priceItem : priceItems) {
                if (priceItem != null &&
                        ((priceItem.getPlatformName() != null && priceItem.getPlatformName().toLowerCase().contains(lowerQuery)) ||
                                (priceItem.getCategoryName() != null && priceItem.getCategoryName().toLowerCase().contains(lowerQuery)) ||
                                (priceItem.getName() != null && priceItem.getName().toLowerCase().contains(lowerQuery)))) {
                    results.add(priceItem);
                }
            }
        }

        return results;

    }


    public List<PriceItem> searchPriceItems(String query) {
        return searchPriceItems(getPriceItems(), query);
    }


    /**
     * The input is search keys concatenated like platform|category|name
     *
     * @param concatenatedKey
     * @return
     */
    public PriceItem findByConcatenatedKey(String concatenatedKey) {

        if (concatenatedKey == null) {
            throw new RuntimeException("Invalid search key: " + concatenatedKey);
        }

        for (PriceItem priceItem : getPriceItems()) {
            StringBuilder sb = new StringBuilder();
            sb.append(priceItem.getPlatformName());
            sb.append('|');
            sb.append(priceItem.getCategoryName());
            sb.append('|');
            sb.append(priceItem.getName());

            if (concatenatedKey.equals(sb.toString())) {
                return priceItem;
            }
        }

        return null;
    }
}
