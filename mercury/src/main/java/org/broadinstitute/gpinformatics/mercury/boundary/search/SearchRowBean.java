package org.broadinstitute.gpinformatics.mercury.boundary.search;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * Row in SearchResponseBean.
 */
@XmlRootElement(namespace = Namespaces.SEARCH)
@XmlType(namespace = Namespaces.SEARCH)
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchRowBean {
    List<String> fields;
    List<SearchResponseBean> nestedTables;

    /** For JAXB. */
    public SearchRowBean() {
    }

    public SearchRowBean(List<String> fields, List<SearchResponseBean> nestedTables) {
        this.fields = fields;
        this.nestedTables = nestedTables;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<SearchResponseBean> getNestedTables() {
        return nestedTables;
    }
}
