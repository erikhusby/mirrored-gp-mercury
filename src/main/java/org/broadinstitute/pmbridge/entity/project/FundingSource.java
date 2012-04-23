package org.broadinstitute.pmbridge.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
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
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
     }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
