package org.broadinstitute.gpinformatics.mercury.boundary.search;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * A term and value in a Configurable Search.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchValue {
    private String termName;
//    private SearchInstance.Operator operator;
    private List<String> values;

    /** For JAXB. */
    public SearchValue() {
    }

    public SearchValue(String termName, List<String> values) {
        this.termName = termName;
        this.values = values;
    }

    public String getTermName() {
        return termName;
    }

    public List<String> getValues() {
        return values;
    }
}
