package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Manage the list of filtered objects for the product order view page when the user selects different filters on the data table.
 *
 * @author Michael Dinsmore
 */
@ManagedBean
@ViewScoped
public class ProductOrderFilterBean implements Serializable {
    private List<ProductOrderSample> filteredSamples;

    public List<ProductOrderSample> getFilteredSamples() {
        return filteredSamples;
    }

    public void setFilteredSamples(List<ProductOrderSample> filteredSamples) {
        this.filteredSamples = filteredSamples;
    }
}
