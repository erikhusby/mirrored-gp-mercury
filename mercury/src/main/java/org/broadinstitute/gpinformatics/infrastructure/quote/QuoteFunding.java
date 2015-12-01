package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

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

    public Collection<FundingLevel> getFundingLevel() {
        return fundingLevel;
    }

    public String getFundsRemaining() {
        return fundsRemaining;
    }


}
