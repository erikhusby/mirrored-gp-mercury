package org.broadinstitute.gpinformatics.mercury.presentation;

import org.jboss.weld.context.http.HttpConversationContext;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * @author breilly
 */
@Startup
@Singleton
// TODO DELETE
public class Config {

    @Inject
    HttpConversationContext conversationContext;

    @PostConstruct
    public void init() {
        conversationContext.setDefaultTimeout(1000 * 60 * 30);
    }
}
