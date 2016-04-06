package org.broadinstitute.gpinformatics.mercury.boundary.search;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * Response from SearchResource.
 */
@XmlRootElement(namespace = Namespaces.SEARCH)
@XmlType(namespace = Namespaces.SEARCH)
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResponseBean {
    private List<String> headers;
    private List<SearchRowBean> searchRowBeans;

    /** For JAXB. */
    public SearchResponseBean() {
    }

    public SearchResponseBean(List<String> headers, List<SearchRowBean> searchRowBeans) {
        this.headers = headers;
        this.searchRowBeans = searchRowBeans;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<SearchRowBean> getSearchRowBeans() {
        return searchRowBeans;
    }
}
