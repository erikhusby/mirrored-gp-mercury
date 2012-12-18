package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderUtil;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ManagedBean(name = "pdoEditBean")
@ViewScoped
/**
 * Backing bean for {@link ProductOrder} edit
 */
public class ProductOrderEditBean extends AbstractJsfBean implements Serializable {

    private ProductOrder productOrder;

    @Inject
    private ProductOrderManager productOrderManager;

    @Inject
    private ProductOrderUtil productOrderUtil;

    private Log logger = LogFactory.getLog(ProductOrderEditBean.class);

    /**
     * List of selected add-on part numbers pushed in from the selectManyCheckbox
     */
    private List<String> selectedAddOnPartNumbers = new ArrayList<String>();

    /**
     * List of all add-ons available for the currently selected Product used to render the selectManyCheckbox
     */
    private List<Product> addOns = new ArrayList<Product>();


    public ProductOrder getProductOrder() {
        return productOrder;
    }


    public void setProductOrder(ProductOrder productOrder) {
        this.productOrder = productOrder;

        setupAddOns();

        if (productOrder != null) {
            for (ProductOrderAddOn pdoAddOn : productOrder.getAddOns()) {
                selectedAddOnPartNumbers.add(pdoAddOn.getAddOn().getPartNumber());
            }
            Collections.sort(selectedAddOnPartNumbers);
        }

    }


    public String getFundsRemaining() {
        try {
            return productOrderUtil.getFundsRemaining(productOrder);
        } catch (QuoteNotFoundException e) {
            String errorMessage = MessageFormat.format("The Quote ID ''{0}'' is invalid.", productOrder.getQuoteId());
            logger.error(errorMessage);
            // mlc do not have an error message for this, this method will be invoked on blur from the quote entry field,
            // so it's not really a big deal.
            // It results in multiple disturbing red messages at the bottom of the screen on the next full request cycle.

            // The real quote validation will be performed in the ProductOrderManager#update method

            // addErrorMessage("quote", errorMessage, errorMessage + ": " + e);
            return "";
        }
    }


    public List<Product> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<Product> addOns) {
        this.addOns = addOns;
    }

    public List<String> getSelectedAddOnPartNumbers() {
        return selectedAddOnPartNumbers;
    }

    public void setSelectedAddOnPartNumbers(List<String> selectedAddOnPartNumbers) {
        this.selectedAddOnPartNumbers = selectedAddOnPartNumbers;
    }

    public void setupAddOns() {

        addOns.clear();
        if (productOrder != null) {
            for (Product product : productOrder.getProduct().getAddOns()) {
                addOns.add(product);
            }
        }

        Collections.sort(addOns);
        selectedAddOnPartNumbers.clear();
    }


    private String handleError(String message, Exception e) {
        logger.error(message, e);
        addErrorMessage(message);
        return null;
    }


    public String update() {

        try {
            productOrderManager.update(productOrder, selectedAddOnPartNumbers);

        } catch (QuoteNotFoundException e) {

            return handleError("Error attempting to update Product Order: Invalid Quote ID '" + productOrder.getQuoteId() + "'", e);

        } catch (Exception e) {

            return handleError("Error attempting to update Product Order: " + e.getMessage(), e);
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
