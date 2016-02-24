package org.broadinstitute.gpinformatics.mercury.boundary.search;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * A term and value in a Configurable Search.
 */
@XmlRootElement(namespace = Namespaces.SEARCH)
@XmlType(namespace = Namespaces.SEARCH)
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchValueBean {
    private String termName;
//    private SearchInstance.Operator operator;
    private List<String> values;

    /** For JAXB. */
    public SearchValueBean() {
    }

    public SearchValueBean(String termName, List<String> values) {
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
