package org.broadinstitute.gpinformatics.infrastructure.deckmsgs;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nullable;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for the service that accepts messages from liquid handling decks
 */
@ConfigKey("deckMsgs")
public class DeckMessagesConfig extends AbstractConfig implements Serializable {

    private String messageStoreDirRoot;

    @Inject
    public DeckMessagesConfig(@Nullable Deployment deployment) {
        super(deployment);
    }

    public String getMessageStoreDirRoot() {
        return messageStoreDirRoot;
    }

    public void setMessageStoreDirRoot(String messageStoreDirRoot) {
        this.messageStoreDirRoot = messageStoreDirRoot;
    }

}
