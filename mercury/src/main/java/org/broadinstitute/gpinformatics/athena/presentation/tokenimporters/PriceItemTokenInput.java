package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.json.JSONException;
import org.json.JSONObject;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class is the user implementation of the token object.  The key is the concatenated key of the price item.
 *
 * @author hrafal
 */
@Dependent
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

        Collection<QuotePriceItem> quotePriceItems = priceListCache.searchPriceItems(query,
                PriceListCache.PriceGrouping.ALL);
        return createItemListString(new ArrayList<>(quotePriceItems));
    }

    public String getExternalJsonString(String query) throws JSONException {

        Collection<QuotePriceItem> quotePriceItems = priceListCache.searchPriceItems(query,
                PriceListCache.PriceGrouping.External_Only);
        return createItemListString(new ArrayList<>(quotePriceItems));
    }

    @Override
    protected String getTokenId(QuotePriceItem quotePriceItem) {
        return PriceItem.makeConcatenatedKey(quotePriceItem.getPlatformName(),
                quotePriceItem.getCategoryName(), quotePriceItem.getName());
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

    public PriceItem getItem() {
        return getPriceItem(getTokenObject());
    }

    private PriceItem getPriceItem(QuotePriceItem quotePriceItem) {
        // Find the existing Mercury price item.
        if (quotePriceItem == null) {
            return null;
        }

        PriceItem priceItem = priceItemDao.find(quotePriceItem.getPlatformName(),
                quotePriceItem.getCategoryName(), quotePriceItem.getName());

        if (priceItem == null) {
            priceItem = new PriceItem(quotePriceItem.getId(), quotePriceItem.getPlatformName(),
                    quotePriceItem.getCategoryName(), quotePriceItem.getName());
            priceItemDao.persist(priceItem);
        }

        return priceItem;
    }
}
