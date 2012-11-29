package org.broadinstitute.gpinformatics.athena.presentation.billing;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named
@ConversationScoped
public class BillingUploadConversationData implements Serializable {

    @Inject
    private Conversation conversation;

    private String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    public void beginConversation( ) {
        if (conversation.isTransient()) {
            conversation.begin();
//            setFilename( null );
        }
    }

}
