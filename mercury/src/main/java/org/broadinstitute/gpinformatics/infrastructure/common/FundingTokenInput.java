package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.athena.boundary.FundingListBean;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

/**
 * This class is the funding implementation of the token object
 *
 * @author hrafal
 */
public class FundingTokenInput extends TokenInput<Funding> {

    private FundingListBean fundingListBean;

    public FundingTokenInput(FundingListBean fundingListBean) {
        super();
        this.fundingListBean = fundingListBean;
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
            itemList.put(new AutoCompleteToken(String.valueOf(funding.getDisplayName()), fullName, false).getJSONObject());
        }

        return itemList.toString();
    }

    public static String getFundingCompleteData(FundingListBean fundingList, String[] fundingIds) throws JSONException {

        JSONArray itemList = new JSONArray();
        for (String fundingId : fundingIds) {
            Funding funding = fundingList.getById(fundingId);
            itemList.put(new AutoCompleteToken(fundingId, funding.getDisplayName(), false).getJSONObject());
        }

        return itemList.toString();
    }
}
