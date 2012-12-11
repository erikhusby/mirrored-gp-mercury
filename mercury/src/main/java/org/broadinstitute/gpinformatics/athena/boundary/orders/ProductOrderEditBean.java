package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.apache.commons.logging.Log;
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
public class ProductOrderEditBean extends AbstractJsfBean implements Serializable {

    private ProductOrder productOrder;

    @Inject
    private ProductOrderManager productOrderManager;

    @Inject
    private ProductOrderUtil productOrderUtil;

    @Inject
    private Log logger;

    private List<String> selectedAddOnPartNumbers = new ArrayList<String>();

    private List<Product> addOns = new ArrayList<Product>();


    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(ProductOrder productOrder) {
        this.productOrder = productOrder;

        if (productOrder != null) {
            for (Product product : productOrder.getProduct().getAddOns()) {
                addOns.add(product);
            }
            Collections.sort(addOns);

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
            addErrorMessage("quote", errorMessage, errorMessage + ": " + e);
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


    public String update() {

        try {

            productOrderManager.update(productOrder, selectedAddOnPartNumbers);

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
