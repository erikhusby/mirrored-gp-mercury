package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectDetail;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Bean for tracking details about a product order.
 */
@Named
@RequestScoped
public class ProductOrderDetail {
    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ResearchProjectDetail researchProjectDetail;

    @Inject
    UserBean userBean;

    /** Key used to look up this product order. */
    private String productOrderKey;

    /** The product order we're currently displaying */
    private ProductOrder productOrder;

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }

    public ProductOrder getProductOrder() {
        if (productOrder == null) {
            // FIXME: need to fix case when editing an existing product order.
            // This will occur when editing a new productOrder.
            productOrder = new ProductOrder(userBean.getBspUser(), researchProjectDetail.getProject());
        }
        return productOrder;
    }

    public void setProductOrder(ProductOrder productOrder) {
        this.productOrder = productOrder;
    }

    public void load() {
        if (productOrder != null) {
            productOrder.loadBspData();
        }
    }
}