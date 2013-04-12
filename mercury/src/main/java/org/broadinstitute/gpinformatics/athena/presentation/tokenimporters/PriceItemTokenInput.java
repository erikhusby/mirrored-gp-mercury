package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is the user implementation of the token object.  The key is the concatenated key of the price item.
 *
 * @author hrafal
 */
public class PriceItemTokenInput extends TokenInput<QuotePriceItem> {

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private PriceListCache priceListCache;

    public PriceItemTokenInput() {
        super(DOUBLE_LINE_FORMAT);
    }

    @Override
    protected QuotePriceItem getById(String key) {
        return priceListCache.findByConcatenatedKey(key);
    }

    public String getJsonString(String query) throws JSONException {

        Collection<QuotePriceItem> quotePriceItems = priceListCache.searchPriceItems(query);
        return createItemListString(new ArrayList<QuotePriceItem>(quotePriceItems));
    }

    @Override
    protected String getTokenId(QuotePriceItem quotePriceItem) {
        return org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.makeConcatenatedKey(
                quotePriceItem.getPlatformName(), quotePriceItem.getCategoryName(), quotePriceItem.getName());
    }

    @Override
    protected String formatMessage(String messageString, QuotePriceItem quotePriceItem) {
        return MessageFormat.format(messageString, quotePriceItem.getName(),
            quotePriceItem.getPlatformName() + " " + quotePriceItem.getCategoryName());
    }

    @Override
    protected String getTokenName(QuotePriceItem quotePriceItem) {
        return quotePriceItem.getName();
    }

    @Override
    public JSONObject createAutocomplete(QuotePriceItem quotePriceItem) throws JSONException {
        if (quotePriceItem == null) {
            JSONObject item = getJSONObject(getListOfKeys(), "unknown price item id");
            item.put("dropdownItem", "");
            return item;
        }

        return super.createAutocomplete(quotePriceItem);
    }

    public org.broadinstitute.gpinformatics.athena.entity.products.PriceItem getMercuryTokenObject() {
        List<QuotePriceItem> quotePriceItems = getTokenObjects();

        if ((quotePriceItems == null) || (quotePriceItems.isEmpty())) {
            return null;
        }

        if (quotePriceItems.size() > 1) {
            throw new IllegalArgumentException("If you want to get more than one price item, use #getMercuryTokenObjects.");
        }

        return getMercuryPriceItem(quotePriceItems.get(0));
    }

    public Collection<? extends org.broadinstitute.gpinformatics.athena.entity.products.PriceItem> getMercuryTokenObjects() {
        List<org.broadinstitute.gpinformatics.athena.entity.products.PriceItem> mercuryTokenObjects =
                new ArrayList<org.broadinstitute.gpinformatics.athena.entity.products.PriceItem>();

        for (QuotePriceItem quotePriceItem : getTokenObjects()) {
            org.broadinstitute.gpinformatics.athena.entity.products.PriceItem mercuryPriceItem = getMercuryPriceItem(
                    quotePriceItem);
            if (mercuryPriceItem != null) {
                mercuryTokenObjects.add(mercuryPriceItem);
            }
        }

        return mercuryTokenObjects;
    }

    private org.broadinstitute.gpinformatics.athena.entity.products.PriceItem getMercuryPriceItem(QuotePriceItem quotePriceItem) {
        // Find the existing Mercury price item.
        if (quotePriceItem == null) {
            return null;
        }

        org.broadinstitute.gpinformatics.athena.entity.products.PriceItem mercuryPriceItem =
                priceItemDao.find(quotePriceItem.getPlatformName(), quotePriceItem.getCategoryName(), quotePriceItem.getName());

        if (mercuryPriceItem == null) {
            mercuryPriceItem = new org.broadinstitute.gpinformatics.athena.entity.products.PriceItem(
                    quotePriceItem.getId(), quotePriceItem.getPlatformName(), quotePriceItem.getCategoryName(), quotePriceItem
                    .getName());
            priceItemDao.persist(mercuryPriceItem);
        }

        return mercuryPriceItem;
    }
}
