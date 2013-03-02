package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is the user implementation of the token object. The key is the concatenated key of the price item
 *
 * @author hrafal
 */
public class PriceItemTokenInput extends TokenInput<PriceItem> {

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private PriceListCache priceListCache;

    public PriceItemTokenInput() {
    }

    @Override
    protected PriceItem getById(String key) {
        return priceListCache.findByConcatenatedKey(key);
    }

    public String getJsonString(String query) throws JSONException {

        Collection<PriceItem> priceItems = priceListCache.searchPriceItems(query);
        return createItemListString(new ArrayList<PriceItem>(priceItems));
    }

    @Override
    protected JSONObject createAutocomplete(JSONArray itemList, PriceItem priceItem) throws JSONException {
        if (priceItem == null) {
            JSONObject item = getJSONObject(getListOfKeys(), "unknown price item id", false);
            item.put("dropdownItem", "");
            itemList.put(item);
            return item;
        }

        String key = org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.makeConcatenatedKey(
                priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());

        JSONObject item = getJSONObject(key, priceItem.getName(), false);
        String list =  "<div class=\"ac-dropdown-text\">" + priceItem.getName() + "</div>" +
                       "<div class=\"ac-dropdown-subtext\">" +
                            priceItem.getPlatformName() + " " + priceItem.getCategoryName() +
                       "</div>";
        item.put("dropdownItem", list);
        itemList.put(item);

        return item;
    }

    public org.broadinstitute.gpinformatics.athena.entity.products.PriceItem getMercuryTokenObject() {
        List<PriceItem> priceItems = getTokenObjects();

        if ((priceItems == null) || (priceItems.isEmpty())) {
            return null;
        }

        if (priceItems.size() > 1) {
            throw new IllegalArgumentException("If you want to get more than one price item, use getMercuryTokenObjecs");
        }

        return getMercuryPriceItem(priceItems.get(0));
    }

    public Collection<? extends org.broadinstitute.gpinformatics.athena.entity.products.PriceItem> getMercuryTokenObjects() {
        List<org.broadinstitute.gpinformatics.athena.entity.products.PriceItem> mercuryTokenObjects =
                new ArrayList<org.broadinstitute.gpinformatics.athena.entity.products.PriceItem>();

        for (PriceItem priceItem : getTokenObjects()) {
            org.broadinstitute.gpinformatics.athena.entity.products.PriceItem mercuryPriceItem = getMercuryPriceItem(priceItem);
            if (mercuryPriceItem != null) {
                mercuryTokenObjects.add(mercuryPriceItem);
            }
        }

        return mercuryTokenObjects;
    }

    private org.broadinstitute.gpinformatics.athena.entity.products.PriceItem getMercuryPriceItem(PriceItem priceItem) {
        // Find the existing mercury price item
        if (priceItem == null) {
            return null;
        }

        org.broadinstitute.gpinformatics.athena.entity.products.PriceItem mercuryPriceItem =
                priceItemDao.find(priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());

        if (mercuryPriceItem == null) {
            mercuryPriceItem = new org.broadinstitute.gpinformatics.athena.entity.products.PriceItem(
                    priceItem.getId(), priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());
            priceItemDao.persist(mercuryPriceItem);
        }

        return mercuryPriceItem;
    }
}
