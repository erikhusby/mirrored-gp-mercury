package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public Collection<FundingLevel> getActiveFundingLevel() {
        return Optional.ofNullable(fundingLevel).orElse(Collections.emptyList()).stream()
                .filter(FundingLevel::isActive)
                .collect(Collectors.toList());
    }

    public String getFundsRemaining() {
        return fundsRemaining;
    }


}
