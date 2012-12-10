package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.annotation.Nullable;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import java.io.Serializable;

@ConversationScoped
public class ProductCreateEditConversationData implements Serializable {

    @Inject
    private Conversation conversation;

    private Product product;

    public void beginConversation(@Nullable Product product) {
        this.product = product;

        if (conversation.isTransient()) {
            conversation.begin();
        }
    }


    public void endConversation() {
        conversation.end();
    }

    public Product getProduct() {
        return product;
    }
}
