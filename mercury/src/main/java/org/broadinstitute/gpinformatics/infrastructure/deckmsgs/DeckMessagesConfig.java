package org.broadinstitute.gpinformatics.infrastructure.deckmsgs;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for the service that accepts messages from liquid handling decks
 */
@ConfigKey("deckMsgs")
@ApplicationScoped
public class DeckMessagesConfig extends AbstractConfig implements Serializable {

    private String messageStoreDirRoot;

    public DeckMessagesConfig(){}

    @Inject
    public DeckMessagesConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    public String getMessageStoreDirRoot() {
        return messageStoreDirRoot;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setMessageStoreDirRoot(String messageStoreDirRoot) {
        this.messageStoreDirRoot = messageStoreDirRoot;
    }

}
