package org.broadinstitute.sequel.control.quote;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="Quotes")
public class Quotes {
    private List<Quote> quote = new ArrayList<Quote>();

    @XmlElement(name="Quote")    
    public List<Quote> getQuotes() {
        return quote;
    }

    public void setQuotes(List<Quote> quote) {
        this.quote = quote;
    }
}
    