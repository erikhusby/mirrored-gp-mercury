package org.broadinstitute.gpinformatics.mercury.boundary.search;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * Request for Configurable Search.
 */
@XmlRootElement(namespace = Namespaces.SEARCH)
@XmlType(namespace = Namespaces.SEARCH)
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchRequestBean {
    private String entityName;
    private String searchName;
    private List<SearchValueBean> searchValueBeanList;

    /** For JAXB. */
    public SearchRequestBean() {
    }

    public SearchRequestBean(String entityName, String searchName, List<SearchValueBean> searchValueBeanList) {
        this.entityName = entityName;
        this.searchName = searchName;
        this.searchValueBeanList = searchValueBeanList;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getSearchName() {
        return searchName;
    }

    public List<SearchValueBean> getSearchValueBeanList() {
        return searchValueBeanList;
    }
}
