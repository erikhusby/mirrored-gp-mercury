package org.broadinstitute.gpinformatics.infrastructure.deckmsgs;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

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
}
