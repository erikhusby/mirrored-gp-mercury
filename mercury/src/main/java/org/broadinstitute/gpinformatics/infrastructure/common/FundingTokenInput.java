package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.athena.boundary.FundingListBean;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;

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
}
