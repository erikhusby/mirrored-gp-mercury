package org.broadinstitute.gpinformatics.mercury.boundary.search;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Request for Configurable Search.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchRequest {
    private String entityName;
    private String searchName;
    private List<SearchValue> searchValueList;

    /** For JAXB. */
    public SearchRequest() {
    }

    public SearchRequest(String entityName, String searchName, List<SearchValue> searchValueList) {
        this.entityName = entityName;
        this.searchName = searchName;
        this.searchValueList = searchValueList;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getSearchName() {
        return searchName;
    }

    public List<SearchValue> getSearchValueList() {
        return searchValueList;
    }
}
