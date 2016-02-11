package org.broadinstitute.gpinformatics.mercury.boundary.search;

import java.util.List;

/**
 * Request for Configurable Search.
 */
public class SearchRequest {
    private String entityName;
    private String searchName;
    private List<SearchValue> searchValueList;

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
