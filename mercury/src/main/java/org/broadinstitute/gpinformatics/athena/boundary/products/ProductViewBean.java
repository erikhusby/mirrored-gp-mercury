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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    @ConversationScoped public static class AddOnTableData extends TableData<Product> {}
    @Inject private AddOnTableData addOnData;

    /**
     * Store price items in conversation scope to support p:dataTable sorting.
     */
    @ConversationScoped public static class PriceItemTableData extends TableData<PriceItem> {}
    @Inject private PriceItemTableData optionalPriceItemData;

    @Inject
    private FacesContext facesContext;

    @Inject
    private Conversation conversation;

    public void onPreRenderView() {

        if (! facesContext.isPostback()) {
            if (product != null) {
                List<Product> addOnList = new ArrayList<Product>(product.getAddOns());
                Collections.sort(addOnList);
                addOnData.setValues(addOnList);

                List<PriceItem> priceItemList = new ArrayList<PriceItem>(product.getOptionalPriceItems());
                Collections.sort(priceItemList);
                optionalPriceItemData.setValues(priceItemList);

                if (conversation.isTransient()) {
                    conversation.begin();
                }
            } else {
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
        if (! facesContext.isPostback()) {
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

    public PriceItemTableData getOptionalPriceItemData() {
        return optionalPriceItemData;
    }

    public void setOptionalPriceItemData(PriceItemTableData optionalPriceItemData) {
        this.optionalPriceItemData = optionalPriceItemData;
    }
}
