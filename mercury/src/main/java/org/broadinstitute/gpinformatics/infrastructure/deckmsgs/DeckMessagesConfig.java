package org.broadinstitute.gpinformatics.infrastructure.deckmsgs;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.Serializable;

/**
 * Configuration for the service that accepts messages from liquid handling decks
 */
@ConfigKey("deckMsgs")
public class DeckMessagesConfig extends AbstractConfig implements Serializable {

    private String messageStoreDirRoot;

    public DeckMessagesConfig() {
    }

    public String getMessageStoreDirRoot() {
        return messageStoreDirRoot;
    }

    public void setMessageStoreDirRoot(String messageStoreDirRoot) {
        this.messageStoreDirRoot = messageStoreDirRoot;
    }


    public static DeckMessagesConfig produce(Deployment deployment) {
        return produce(DeckMessagesConfig.class, deployment);
    }
}
