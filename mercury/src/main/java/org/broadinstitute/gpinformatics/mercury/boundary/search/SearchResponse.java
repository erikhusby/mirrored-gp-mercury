package org.broadinstitute.gpinformatics.mercury.boundary.search;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Response from SearchResource.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResponse {
    private List<String> headers;
    private List<SearchRow> searchRows;

    /** For JAXB. */
    public SearchResponse() {
    }

    public SearchResponse(List<String> headers, List<SearchRow> searchRows) {
        this.headers = headers;
        this.searchRows = searchRows;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<SearchRow> getSearchRows() {
        return searchRows;
    }
}
