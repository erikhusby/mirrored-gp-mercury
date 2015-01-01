/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.PublicMessageEjb;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;

import javax.inject.Inject;

/**
 * This ActionBean is used for displaying the mercury public message.
 */
@UrlBinding(PublicMessageActionBean.URL_BINDING)
public class PublicMessageActionBean extends CoreActionBean {
    public static final String URL_BINDING = "/public/public_message.action";
    public static final String TEXT = "text";
    private PublicMessage publicMessage;
    @Inject
    private PublicMessageEjb publicMessageEjb;

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        publicMessage = publicMessageEjb.getPublicMessage();
    }

    @DefaultHandler
    @HandlesEvent(TEXT)
    public Resolution text() {
        String messageText = "";
        if (publicMessage != null) {
            messageText = publicMessage.getMessage();
        }
        return new StreamingResolution("text", messageText);
    }

    public void setPublicMessage(PublicMessage publicMessage) {
        this.publicMessage = publicMessage;
    }
}
