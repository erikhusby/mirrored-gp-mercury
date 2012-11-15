package org.broadinstitute.gpinformatics.infrastructure.jsf;

import javax.enterprise.context.ConversationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author breilly
 */
//@ManagedBean
//@ViewScoped
@Named
@ConversationScoped
public class TableData implements Serializable {

    private List values = new ArrayList();

    private List filteredValues = new ArrayList();

    public List getValues() {
        return values;
    }

    public void setValues(List values) {
        this.values = values;
    }

    public List getFilteredValues() {
        return filteredValues;
    }

    public void setFilteredValues(List filteredValues) {
        this.filteredValues = filteredValues;
    }
}
