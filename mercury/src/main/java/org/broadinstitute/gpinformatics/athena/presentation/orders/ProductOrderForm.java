package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

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

    private String sampleIDs = "";

    public String getSampleIDs() {
        return sampleIDs;
    }

    public void setSampleIDs(String sampleIDs) {
        this.sampleIDs = sampleIDs;
    }

    /**
     * Load local state before bringing up the UI.
     */
    public void load() {
        productOrderDetail.load();
        if (sampleIDs == null && productOrderDetail != null) {
            StringBuilder sb = new StringBuilder();
            String separator = "";
            for (ProductOrderSample sample : productOrderDetail.getProductOrder().getSamples()) {
                sb.append(separator).append(sample.getSampleName());
                separator = ", ";
            }

            sampleIDs = sb.toString();
        }
    }
}
