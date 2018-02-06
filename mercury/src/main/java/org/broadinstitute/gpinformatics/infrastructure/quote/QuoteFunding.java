package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.collections4.CollectionUtils;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: andrew
 * Date: 2/29/12
 * Time: 2:35 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "QuoteFunding")
public class QuoteFunding {

    @XmlAttribute(name = "fundsRemaining" )
    private String fundsRemaining;

    @XmlElement(name = "FundingLevel")
    private Collection<FundingLevel> fundingLevel;

    public QuoteFunding() {}

    public QuoteFunding(String fundsRemaining) {
        this.fundsRemaining = fundsRemaining;
    }
    
    public QuoteFunding(Collection<FundingLevel> fundLevel) {
        this.fundingLevel = fundLevel;
    }

    public QuoteFunding(String fundsRemaining,
                        Collection<FundingLevel> fundingLevel) {
        this.fundsRemaining = fundsRemaining;
        this.fundingLevel = fundingLevel;
    }

    public Collection<FundingLevel> getFundingLevel() {
        return fundingLevel;
    }

    public Collection<FundingLevel> getFundingLevel(boolean excludeInactiveSources) {
        List<FundingLevel> condensedFundingLevles = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(fundingLevel)) {
            for (FundingLevel level : fundingLevel) {
                if(excludeInactiveSources && Integer.valueOf(level.getPercent()) == 0)  {
                    continue;
                } else {
                    condensedFundingLevles.add(level);
                }
            }
        }
        return condensedFundingLevles;

    }

    public String getFundsRemaining() {
        return fundsRemaining;
    }


}
