package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

/**
 * This class is the funding implementation of the token object
 *
 * @author hrafal
 */
@Dependent
public class FundingTokenInput extends TokenInput<Funding> {

    @Inject
    private QuoteFundingList quoteFundingList;

    public FundingTokenInput() {
        super(DOUBLE_LINE_FORMAT);
    }

    @Override
    protected Funding getById(String fundingId) {
        return quoteFundingList.getById(fundingId);
    }

    public String getJsonString(String query) throws JSONException {
        List<Funding> fundingList = quoteFundingList.find(query);
        return createItemListString(fundingList);
    }

    @Override
    protected String getTokenId(Funding funding) {
        return funding.getDisplayName();
    }

    @Override
    protected String formatMessage(String messageString, Funding funding) {
        return MessageFormat.format(messageString, funding.getDisplayName(), funding.getMatchDescription());
    }

    @Override
    protected String getTokenName(Funding funding) {
        return funding.getDisplayName();
    }
}
