package org.broadinstitute.sequel.control.quote;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="Quotes")
public class Quotes {

    private List<Quote> quotes = new ArrayList<Quote>();

    public void addQuote(Quote q) {
        quotes.add(q);
    }
    
    @XmlElement(name="Quote")    
    public List<Quote> getQuotes() {
        return quotes;
    }

    public void setQuotes(List<Quote> quote) {
        this.quotes = quote;
    }
}
    