package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement(name = "FundingLevel")
public class FundingLevel {

    private String percent;
    
    private Collection<Funding> funding;

    public FundingLevel() {}

    public FundingLevel(String percent,Collection<Funding> funding) {
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
    public Collection<Funding> getFunding() {
        return funding;
    }

    public void setFunding(Collection<Funding> funding) {
        this.funding = funding;
    }
}
