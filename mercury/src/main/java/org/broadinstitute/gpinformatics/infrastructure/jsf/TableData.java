package org.broadinstitute.gpinformatics.infrastructure.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author breilly
 */
public class TableData<T> implements Serializable {

    private List<T> values = new ArrayList<T>();

    private List<T> filteredValues = new ArrayList<T>();

    public List<T> getValues() {
        return values;
    }

    public void setValues(List<T> values) {
        this.values = values;
    }

    public List<T> getFilteredValues() {
        return filteredValues;
    }

    public void setFilteredValues(List<T> filteredValues) {
        this.filteredValues = filteredValues;
    }
}
