package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;
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
    protected boolean isSingleLineMenuEntry() {
        return false;
    }

    @Override
    protected String getTokenId(Funding funding) {
        return funding.getDisplayName();
    }

    @Override
    protected String getTokenName(Funding funding) {
        return funding.getDisplayName();
    }

    @Override
    protected String[] getMenuLines(Funding funding) {
        String[] lines = new String[2];
        lines[0] = funding.getDisplayName();
        lines[1] = funding.getMatchDescription();
        return lines;
    }
}
