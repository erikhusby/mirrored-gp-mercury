package org.broadinstitute.gpinformatics.mercury.boundary.search;

import java.util.List;

/**
 * Response from SearchResource.
 */
public class SearchResponse {
    private List<String> headers;
    private List<SearchRow> searchRows;

    public SearchResponse(List<String> headers, List<SearchRow> searchRows) {
        this.headers = headers;
        this.searchRows = searchRows;
    }
}
