package org.broadinstitute.sequel.control.quote;

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

    
    private FundingLevel fundingLevel;
    
    @XmlElement(name = "FundingLevel")
    public FundingLevel getFundingLevel() {
        return fundingLevel;
    }

    public void setFundingLevel(FundingLevel fundingLevel) {
        this.fundingLevel = fundingLevel;
    }

}
