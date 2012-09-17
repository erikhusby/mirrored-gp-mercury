package org.broadinstitute.pmbridge.infrastructure.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "FundingLevel")
public class FundingLevel {

    private String percent;
    
    private Funding funding;

    public FundingLevel() {}

    public FundingLevel(String percent,Funding funding) {
        this.percent = percent;
        this.funding = funding;
    }

    @XmlAttribute(name = "percent")
    public String getPercent() {
        return percent;
    }

    public void setPercent(String percent) {
        this.percent = percent;
    }

    @XmlElement(name = "Funding")
    public Funding getFunding() {
        return funding;
    }

    public void setFunding(Funding funding) {
        this.funding = funding;
    }
}
