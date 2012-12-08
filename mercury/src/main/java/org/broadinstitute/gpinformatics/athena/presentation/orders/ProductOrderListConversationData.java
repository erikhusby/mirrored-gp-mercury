package org.broadinstitute.gpinformatics.athena.presentation.orders;


import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderListModel;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import java.io.Serializable;

@ConversationScoped
public class ProductOrderListConversationData implements Serializable {

    @Inject
    private Conversation conversation;

    @Inject
    private ProductOrderListEntryDao productOrderListEntryDao;

    // conversation scope the selected product orders so they survive ajax requests
    private ProductOrderListEntry[] selectedProductOrders;

    // cached product orders
    @Inject
    private ProductOrderListModel allProductOrders;


    public void beginConversation() {
        if (conversation.isTransient()) {
            allProductOrders.setWrappedData(productOrderListEntryDao.findProductOrderListEntries());
            conversation.begin();
        }
    }

    public ProductOrderListEntry[] getSelectedProductOrders() {
        return selectedProductOrders;
    }

    public void setSelectedProductOrders(ProductOrderListEntry[] selectedProductOrders) {
        this.selectedProductOrders = selectedProductOrders;
    }

    public ProductOrderListModel getAllProductOrders() {
        return allProductOrders;
    }
}
