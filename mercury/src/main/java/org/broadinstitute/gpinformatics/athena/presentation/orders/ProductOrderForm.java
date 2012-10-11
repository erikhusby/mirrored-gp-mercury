package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Class for creating a product order, or editing a draft product order.
 */
@Named
@RequestScoped
public class ProductOrderForm extends AbstractJsfBean {
    @Inject
    ProductOrderDetail productOrderDetail;

    @Inject
    ProductOrderDao productOrderDao;

    // Add state that can be edited here.

    private List<String> sampleIDs;

    public List<String> getSampleIDs() {
        return sampleIDs;
    }

    public void setSampleIDs(List<String> sampleIDs) {
        this.sampleIDs = sampleIDs;
    }
}
