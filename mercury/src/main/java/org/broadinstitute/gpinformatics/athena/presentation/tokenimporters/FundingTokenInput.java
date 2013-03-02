package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.List;

/**
 * This class is the funding implementation of the token object
 *
 * @author hrafal
 */
public class FundingTokenInput extends TokenInput<Funding> {

    @Inject
    private QuoteFundingList quoteFundingList;

    @Override
    protected Funding getById(String fundingId) {
        return quoteFundingList.getById(fundingId);
    }

    public String getJsonString(String query) throws JSONException {
        List<Funding> fundingList = quoteFundingList.find(query);
        return createItemListString(fundingList);
    }

    @Override
    protected JSONObject createAutocomplete(JSONArray itemList, Funding funding) throws JSONException {
        JSONObject item = getJSONObject(funding.getDisplayName(), funding.getDisplayName(), false);
        String list =  "<div class=\"ac-dropdown-text\">" + funding.getDisplayName() + "</div>" +
                       "<div class=\"ac-dropdown-subtext\">" + funding.getMatchDescription() + "</div>";
        item.put("dropdownItem", list);
        itemList.put(item);

        return item;
    }
}
