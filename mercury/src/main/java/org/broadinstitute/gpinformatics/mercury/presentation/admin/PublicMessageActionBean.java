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

package org.broadinstitute.gpinformatics.mercury.presentation.admin;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import org.broadinstitute.gpinformatics.athena.control.dao.admin.PublicMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;

/**
 * This ActionBean is used for setting or clearing the mercury public message.
 */
@UrlBinding(PublicMessageActionBean.URL_BINDING)
public class PublicMessageActionBean extends CoreActionBean {
    public static final String URL_BINDING = "/admin/public_message.action";
    private static final String ADD_MESSAGE = "addMessage";
    private static final String CLEAR_MESSAGE = "clearMessage";
    private static final String MANAGE_PUBLIC_MESSAGE = "/admin/manage_public_message.jsp";
    private static final String TEXT = "text";

    @Inject
    private PublicMessageDao publicMessageDao;

    @ValidateNestedProperties(@Validate(
            field = "message", label = "Public message text", required = true, maxlength = 255, on = ADD_MESSAGE))
    private PublicMessage publicMessage;

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        publicMessage = publicMessageDao.getMessage();
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(MANAGE_PUBLIC_MESSAGE);
    }


    @HandlesEvent(TEXT)
    public Resolution text() {
        PublicMessage publicMessage = getPublicMessage();
        String messageText="";
        if (publicMessage != null) {
            messageText=publicMessage.getMessage();
        }
        return new StreamingResolution("text", messageText);
    }

    @HandlesEvent(ADD_MESSAGE)
    public Resolution addMessage() {
        publicMessageDao.persist(publicMessage);
        return getSourcePageResolution();
    }

    @HandlesEvent(CLEAR_MESSAGE)
    public Resolution clearMessage() {
        publicMessageDao.remove(publicMessage);
        return getSourcePageResolution();
    }

    public PublicMessage getPublicMessage() {
        return publicMessage;
    }

    public void setPublicMessage(PublicMessage publicMessage) {
        this.publicMessage = publicMessage;
    }
}
