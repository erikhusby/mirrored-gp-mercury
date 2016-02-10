package org.broadinstitute.gpinformatics.mercury.boundary.search;

import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;

import java.util.List;

/**
 * A web service front-end to Configurable Search.
 */
public class SearchResource {
    // runSearch(name, params)
    // SearchRequest/Param object?
    // alternative is List<Pair<String,List>>
    // Also need to think about operators?
    // List<String termName, Operator operator, List<String> values>
    // Return ResultList?

    public static class SearchValue {
        private String termName;
        private SearchInstance.Operator operator;
        private List<String> values;
    }

    public static class SearchRequest {
        private String entityName;
        private String searchName;
        private List<SearchValue> searchValueList;
    }

    public static class Row {
        List<String> fields;
        List<SearchResponse> nestedTables;
    }

    public static class SearchResponse {
        private List<String> headers;
        private List<Row> rows;

        public SearchResponse(List<String> headers, List<Row> rows) {
            this.headers = headers;
            this.rows = rows;
        }
    }

    public SearchResponse runSearch(SearchInstance searchInstance) {
        return new SearchResponse(null, null);
    }
}
