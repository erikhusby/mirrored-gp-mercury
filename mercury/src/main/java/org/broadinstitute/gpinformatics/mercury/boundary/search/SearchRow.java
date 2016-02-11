package org.broadinstitute.gpinformatics.mercury.boundary.search;

import java.util.List;

/**
 * Row in SearchResponse.
 */
public class SearchRow {
    List<String> fields;
    List<SearchResponse> nestedTables;
}
