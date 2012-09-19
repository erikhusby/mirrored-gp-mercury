package org.broadinstitute.gpinformatics.athena.infrastructure.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by IntelliJ IDEA.
 * User: andrew
 * Date: 2/29/12
 * Time: 2:35 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "QuoteFunding")
public class QuoteFunding {

    private String fundsRemaining;
    private FundingLevel fundingLevel;

    public QuoteFunding() {}
    
    public QuoteFunding(FundingLevel fundLevel) {
        this.fundingLevel = fundLevel;
    }

    public QuoteFunding(final String fundsRemaining, final FundingLevel fundingLevel) {
        this.fundsRemaining = fundsRemaining;
        this.fundingLevel = fundingLevel;
    }

    @XmlElement(name = "FundingLevel")
    public FundingLevel getFundingLevel() {
        return fundingLevel;
    }

    public void setFundingLevel(FundingLevel fundingLevel) {
        this.fundingLevel = fundingLevel;
    }

    @XmlAttribute(name = "fundsRemaining")
    public String getFundsRemaining() {
        return fundsRemaining;
    }

    public void setFundsRemaining(final String fundsRemaining) {
        this.fundsRemaining = fundsRemaining;
    }
}
