package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.presentation.products.ProductForm;
import org.broadinstitute.gpinformatics.infrastructure.jsf.TableData;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

@Named("productView")
@RequestScoped
public class ProductViewBean extends AbstractJsfBean {

    /**
     * Model object
     */
    private Product product;

    /**
     * Store add-ons in conversation scope to support p:dataTable sorting.
     */
    @ConversationScoped public static class AddOnTableData extends TableData {}
    @Inject private AddOnTableData addOnData;

    /**
     * Store price items in conversation scope to support p:dataTable sorting.
     */
    @ConversationScoped public static class PriceItemTableData extends TableData {}
    @Inject private PriceItemTableData priceItemData;

    @Inject
    private FacesContext facesContext;

    @Inject
    private Conversation conversation;

    public void initView() {
        if (!facesContext.isPostback()) {
            addOnData.setValues(new ArrayList<Product>(product.getAddOns()));
            Collections.sort(addOnData.getValues());
            priceItemData.setValues(new ArrayList<PriceItem>(product.getPriceItems()));
            Collections.sort(priceItemData.getValues());
            if (conversation.isTransient()) {
                conversation.begin();
            }
        }
    }

    public void onPreRenderView() throws IOException {
        if (!facesContext.isPostback()) {
            if (product == null) {
                addErrorMessage("No product with this part number exists.");
                facesContext.renderResponse();
            }
        }
    }

    // TODO: create and use secondsToDaysConverter
    public Integer getExpectedCycleTimeDays() {
        return ProductForm.convertCycleTimeSecondsToDays(product.getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeDays(final Integer expectedCycleTimeDays) {
        product.setExpectedCycleTimeSeconds(ProductForm.convertCycleTimeDaysToSeconds(expectedCycleTimeDays));
    }

    public Integer getGuaranteedCycleTimeDays() {
        return ProductForm.convertCycleTimeSecondsToDays(product.getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeDays(final Integer guaranteedCycleTimeDays) {
        product.setGuaranteedCycleTimeSeconds(ProductForm.convertCycleTimeDaysToSeconds(guaranteedCycleTimeDays));
    }

    public boolean shouldRenderForm() {
        if (!facesContext.isPostback()) {
            return product != null;
        } else {
            return true;
        }
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public AddOnTableData getAddOnData() {
        return addOnData;
    }

    public void setAddOnData(AddOnTableData addOnData) {
        this.addOnData = addOnData;
    }

    public PriceItemTableData getPriceItemData() {
        return priceItemData;
    }

    public void setPriceItemData(PriceItemTableData priceItemData) {
        this.priceItemData = priceItemData;
    }
}
