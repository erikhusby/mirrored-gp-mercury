package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named
@ConversationScoped
public class ProductOrderConversationData implements Serializable {

    @Inject
    private Conversation conversation;

    private Product product;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }


    /**
     * Method that initiates the long-running conversation.  Price items are pulled from the
     * {@link org.broadinstitute.gpinformatics.athena.entity.products.Product} parameter.
     *
     * @param order The order that we are using
     */
    public void beginConversation(ProductOrder order) {
        if (conversation.isTransient()) {
            conversation.begin();

            // start every conversation with initialized data!
            setProduct(order.getProduct());
        }
    }


    public void endConversation() {
        conversation.end();
    }

}
