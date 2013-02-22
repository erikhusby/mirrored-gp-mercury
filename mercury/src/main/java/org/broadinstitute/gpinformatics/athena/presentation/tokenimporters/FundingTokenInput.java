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

        JSONArray itemList = new JSONArray();
        for (Funding funding : fundingList) {
            createAutocomplete(itemList, funding);
        }

        return itemList.toString();
    }

    @Override
    public String generateCompleteData() throws JSONException {

        JSONArray itemList = new JSONArray();
        for (Funding funding : getTokenObjects()) {
            createAutocomplete(itemList, funding);
        }

        return itemList.toString();
    }

    private static void createAutocomplete(JSONArray itemList, Funding funding) throws JSONException {
        JSONObject item = getJSONObject(funding.getDisplayName(), funding.getDisplayName(), false);
        item.put("matchDescription", funding.getMatchDescription());
        itemList.put(item);
    }
}
