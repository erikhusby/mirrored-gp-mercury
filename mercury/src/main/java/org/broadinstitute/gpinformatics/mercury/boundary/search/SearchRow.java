package org.broadinstitute.gpinformatics.mercury.boundary.search;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Row in SearchResponse.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchRow {
    List<String> fields;
    List<SearchResponse> nestedTables;

    /** For JAXB. */
    public SearchRow() {
    }

    public SearchRow(List<String> fields, List<SearchResponse> nestedTables) {
        this.fields = fields;
        this.nestedTables = nestedTables;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<SearchResponse> getNestedTables() {
        return nestedTables;
    }
}
