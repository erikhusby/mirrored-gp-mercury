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
 * Storing {@link PriceItem} data in a conversation to enable crosstalk between "all priceitems" and "default priceitem"
 * components.
 *
 */
@ConversationScoped
public class ProductFormConversationData implements Serializable {

    @Inject
    private Conversation conversation;

    /**
     * JAXB {@link PriceItem} DTOs
     */
    private List<PriceItem> priceItems;

    /**
     * JAXB {@link PriceItem} DTOs, there can be only one default price item but this is a list so the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete} can be styled consistently with all the other
     * multi-selecting {@link org.primefaces.component.autocomplete.AutoComplete}s in Mercury
     */
    private List<PriceItem> defaultPriceItems;

    /**
     * Record of the ID in case it gets lost across AJAX requests so we can always tell whether we're in create or edit
     * mode.
     */
    private Long id;

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
            // start every conversation with initialized data!
//            priceItems = new ArrayList<PriceItem>();
            defaultPriceItems = new ArrayList<PriceItem>();

/*
            if (product != null) {
                id = product.getProductId();
                if (product.getPriceItems() != null) {
                    for (org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItem : product.getPriceItems()) {
                        addPriceItem(entityToDto(priceItem));
                    }
                }
                if (product.getDefaultPriceItem() != null) {
                    defaultPriceItems.add(entityToDto(product.getDefaultPriceItem()));
                }
            }
*/
        }
    }


    public void endConversation() {
        if (! conversation.isTransient()) {
            conversation.end();
        }
    }


    /**
     * Get all the latest PriceItems as visualized on the Create / Edit Product page
     * @return
     */
    public List<PriceItem> getPriceItems() {
        return priceItems;
    }

    /**
     * Setter for UI price items
     * @param priceItems
     */
    public void setPriceItems(List<PriceItem> priceItems) {
        this.priceItems = priceItems;
    }

    /**
     * List version of default price items getter, as used by the actual {@link org.primefaces.component.autocomplete.AutoComplete}
     * component in the create.xhtml
     * @return
     */
    public List<PriceItem> getDefaultPriceItems() {
        return defaultPriceItems;
    }

    /**
     * List version of default price items setter, as used by the actual {@link org.primefaces.component.autocomplete.AutoComplete}
     * component in the create.xhtml
     * @return
     */

    public void setDefaultPriceItems(List<PriceItem> defaultPriceItems) {
        this.defaultPriceItems = defaultPriceItems;
    }

    /**
     * Convenience setter to box the price item into its list
     * @param priceItem
     */
    public void setDefaultPriceItem(PriceItem priceItem) {
        if (defaultPriceItems == null) {
            defaultPriceItems = new ArrayList<PriceItem>();
        }

        defaultPriceItems.clear();

        if (priceItem != null) {
            defaultPriceItems.add(priceItem);
        }
    }

    /**
     * Convenience getter to "unbox" the default price item from its list
     */
    public PriceItem getDefaultPriceItem() {
        if (defaultPriceItems == null || defaultPriceItems.size() == 0) {
            return null;
        }

        return defaultPriceItems.get(0);
    }

    /**
     * Convenience methods to add to the list of current price items
     * @param priceItem
     */
    public void addPriceItem(PriceItem priceItem) {
        priceItems.add(priceItem);
    }

    /**
     * Convenience methods to remove from the list of current price items
     * @param priceItem
     */
    public void removePriceItem(PriceItem priceItem) {
        priceItems.remove(priceItem);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
