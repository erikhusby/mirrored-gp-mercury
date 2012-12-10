package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderUtil;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.text.MessageFormat;

@ManagedBean(name = "pdoEditBean")
@ViewScoped
public class ProductOrderEditBean extends AbstractJsfBean implements Serializable {

    private ProductOrder productOrder;

    @Inject
    private ProductOrderManager productOrderManager;

    @Inject
    private ProductOrderUtil productOrderUtil;

    @Inject
    private Log logger;


    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(ProductOrder productOrder) {
        this.productOrder = productOrder;
    }

    public String getFundsRemaining() {
        try {
            return productOrderUtil.getFundsRemaining(productOrder);
        } catch (QuoteNotFoundException e) {
            String errorMessage = MessageFormat.format("The Quote ID ''{0}'' is invalid.", productOrder.getQuoteId());
            logger.error(errorMessage);
            addErrorMessage("quote", errorMessage, errorMessage + ": " + e);
            return "";
        }
    }


    public String update() {

        try {
            productOrderManager.update(productOrder);

        } catch (Exception e) {

            String message = "Error attempting to update ProductOrder: " + e.getMessage();
            logger.error(message, e);
            addErrorMessage(message);
            return null;
        }

        addInfoMessage(
                MessageFormat.format("Product Order ''{0}'' ({1}) has been updated.",
                        productOrder.getTitle(), productOrder.getJiraTicketKey()));
        // not sure why I need to explicitly add the productOrder parameter; the superclass
        // redirect alone which does includeViewParams=true does not include the productOrder view param that this
        // page received from the view page
        return redirect("view") + "&productOrder=" + productOrder.getBusinessKey();
    }
}
