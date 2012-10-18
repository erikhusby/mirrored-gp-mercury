package org.broadinstitute.gpinformatics.athena.boundary;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Could not seem to inject the BSPUserList directly into the xhtml file, so this is a wrapper that does
 * the injection and provides the access to the find for anything that wants user names instead of the stored
 * ID.
 */
@Named
@RequestScoped
public class FundingListBean {

    @Inject
    private QuoteFundingList fundingList;

    public List<Funding> searchFunding(String query) {
        return fundingList.find(query);
    }

    public String getFundingListString(String[] fundingIds) {
        if ((fundingIds == null) || (fundingIds.length == 0)) {
            return "";
        }

        return StringUtils.join(fundingIds, ", ");
    }
}

