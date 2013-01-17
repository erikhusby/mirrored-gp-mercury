package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.boundary.FundingListBean;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import java.util.List;

/**
 * This class is the funding implementation of the token object
 *
 * @author hrafal
 */
public class FundingTokenInput extends TokenInput<Funding> {

    @Inject
    private FundingListBean fundingListBean;

    public FundingTokenInput() {
        super();
    }

    @Override
    protected Funding getById(String fundingId) {
        return fundingListBean.getById(fundingId);
    }

    public static String getJsonString(FundingListBean fundingListBean, String query) throws JSONException {
        List<Funding> fundingList = fundingListBean.searchFunding(query);

        JSONArray itemList = new JSONArray();
        for (Funding funding : fundingList) {
            String fullName = funding.getDisplayName();
            itemList.put(new AutoCompleteToken(funding.getDisplayName(), fullName, false).getJSONObject());
        }

        return itemList.toString();
    }

    public String getFundingCompleteData() throws JSONException {

        JSONArray itemList = new JSONArray();
        for (Funding funding : getTokenObjects()) {
            itemList.put(new AutoCompleteToken(funding.getDisplayName(), funding.getDisplayName(), false).getJSONObject());
        }

        return itemList.toString();
    }
}
