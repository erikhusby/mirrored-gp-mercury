package org.broadinstitute.pmbridge.entity.project;

import org.broadinstitute.pmbridge.entity.common.Name;

import java.util.Date;

/**
 * A class to describe a funding source within PMBridge.
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 2:54 PM
 */
public class FundingSource {

    public final GrantId grantId;
    public final Name grantDescription;
    public final Date startDate;
    public final Date endDate;
    public final Name sponsorName;

    public FundingSource(GrantId grantId, Name grantDescription, Date startDate, Date endDate, Name sponsorName) {
        this.grantId = grantId;
        this.grantDescription = grantDescription;
        this.startDate = startDate;
        this.endDate = endDate;
        this.sponsorName = sponsorName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FundingSource)) return false;

        final FundingSource that = (FundingSource) o;

        if (endDate != null ? !endDate.equals(that.endDate) : that.endDate != null) return false;
        if (grantDescription != null ? !grantDescription.equals(that.grantDescription) : that.grantDescription != null)
            return false;
        if (grantId != null ? !grantId.equals(that.grantId) : that.grantId != null) return false;
        if (sponsorName != null ? !sponsorName.equals(that.sponsorName) : that.sponsorName != null) return false;
        if (startDate != null ? !startDate.equals(that.startDate) : that.startDate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = grantId != null ? grantId.hashCode() : 0;
        result = 31 * result + (grantDescription != null ? grantDescription.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + (sponsorName != null ? sponsorName.hashCode() : 0);
        return result;
    }

}
