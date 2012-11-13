package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Storing {@link PriceItem} data in a conversation to record the selected priceitems across AJAX requests in the
 * {@link Product} create / edit page.
 *
 */
@ConversationScoped
public class ProductFormConversationData implements Serializable {

    @Inject
    private Conversation conversation;

    /**
     * JAXB {@link PriceItem} DTOs, there can be only one default price item but this is a list so the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete} can be styled consistently with all the other
     * multi-selecting {@link org.primefaces.component.autocomplete.AutoComplete}s in Mercury.  Selection and
     * unselection managed by the AJAX listeners in {@link ProductForm}
     */
    private List<PriceItem> selectedDefaultPriceItems;

    /**
     * Currently selected optional price items as noted by the AJAX select / unselect listeners in {@link ProductForm}.
     */
    private List<PriceItem> selectedOptionalPriceItems;

    /**
     * maps between entity and JAXB DTOs for price items
     * @param entity
     * @return
     */
    private PriceItem entityToDto(org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity) {
        return new PriceItem(entity.getQuoteServerId(), entity.getPlatform(), entity.getCategory(), entity.getName());
    }

    /**
     * Method that initiates the long-running conversation.  Price items are pulled from the {@link Product} parameter.
     *
     * @param product
     */
    public void beginConversation(Product product) {
        if (conversation.isTransient()) {
            conversation.begin();

            selectedDefaultPriceItems = new ArrayList<PriceItem>();
            selectedOptionalPriceItems = new ArrayList<PriceItem>();

            if (product != null) {
                if (product.getPriceItems() != null) {
                    for (org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItem : product.getPriceItems()) {
                        addSelectedOptionalPriceItem(entityToDto(priceItem));
                    }
                }
                if (product.getDefaultPriceItem() != null) {
                    setSelectedDefaultPriceItem(entityToDto(product.getDefaultPriceItem()));
                }
            }
        }
    }


    public void endConversation() {
        if (! conversation.isTransient()) {
            conversation.end();
        }
    }


    /**
     * Convenience setter to box the price item into its list
     * @param priceItem
     */
    public void setSelectedDefaultPriceItem(PriceItem priceItem) {
        if (selectedDefaultPriceItems == null) {
            selectedDefaultPriceItems = new ArrayList<PriceItem>();
        }

        selectedDefaultPriceItems.clear();

        if (priceItem != null) {
            selectedDefaultPriceItems.add(priceItem);
        }
    }

    /**
     * Convenience getter to "unbox" the default price item from its list
     */
    public PriceItem getSelectedDefaultPriceItem() {
        if (selectedDefaultPriceItems == null || selectedDefaultPriceItems.size() == 0) {
            return null;
        }

        return selectedDefaultPriceItems.get(0);
    }


    public void addSelectedOptionalPriceItem(PriceItem priceItem) {
        selectedOptionalPriceItems.add(priceItem);
    }


    public void removeSelectedOptionalPriceItem(PriceItem priceItem) {
        selectedOptionalPriceItems.remove(priceItem);
    }


    public List<PriceItem> getAllSelectedPriceItems() {
        List<PriceItem> allSelectedPriceItems = new ArrayList<PriceItem>();
        allSelectedPriceItems.addAll(selectedDefaultPriceItems);
        allSelectedPriceItems.addAll(selectedOptionalPriceItems);
        return allSelectedPriceItems;
    }


}
