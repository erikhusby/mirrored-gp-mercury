package org.broadinstitute.gpinformatics.mercury.boundary.search;

import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;

import java.util.List;

/**
 * A term and value in a Configurable Search.
 */
public class SearchValue {
    private String termName;
    private SearchInstance.Operator operator;
    private List<String> values;
}
