package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import java.io.Serializable;


/**
 * Conversation data for Product create / edit, hopefully moving towards obsolescence
 *
 */
@ConversationScoped
public class ProductFormConversationData implements Serializable {

    @Inject
    private Conversation conversation;

    /**
     * Record of the ID in case it gets lost across AJAX requests so we can always tell whether we're in create or edit
     * mode.
     */
    private Long id;


    /**
     * Method that initiates the long-running conversation.
     *
     * @param product
     */
    public void beginConversation(Product product) {
        if (conversation.isTransient()) {
            conversation.begin();
        }
    }


    public void endConversation() {
        if (! conversation.isTransient()) {
            conversation.end();
        }
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
