package org.broadinstitute.gpinformatics.mercury.presentation;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockServletContext;
import net.sourceforge.stripes.validation.ValidationErrors;

/**
 * The test Stripes action bean context that gets passed around in the ActionBeans.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class TestCoreActionBeanContext extends CoreActionBeanContext {
    private MockServletContext context = new MockServletContext("Mercury");

    private List<Message> messages = new ArrayList<Message>();

    private ValidationErrors validationErrors = new ValidationErrors();

    private MockHttpSession session = new MockHttpSession(context);

    @Override
    public HttpServletRequest getRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("/Mercury", "/default");
        request.setSession(session);
        return request;
    }

    @Override
    public MockServletContext getServletContext() {
        return context;
    }

    public void setContext(MockServletContext context) {
        this.context = context;
    }

    /**
     * Short hand method to fetch the session.
     *
     * @return HttpSession
     */
    @Override
    public HttpSession getSession() {
        return session;
    }

    /**
     * Remove the HTTP session.
     */
    @Override
    public void invalidateSession() {
        getSession().invalidate();
    }

    /**
     * Get the JAAS username from the mock request.
     *
     * @return the JAAS username
     */
    public String getUsername() {
        HttpServletRequest request = getRequest();
        return request.getRemoteUser();
    }

    /*
     * @return the messages
     */
    @Override
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * @param messages the messages to set
     */
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * @return the validationErrors
     */
    @Override
    public ValidationErrors getValidationErrors() {
        return this.validationErrors;
    }

    /**
     * @param validationErrors the validationErrors to set
     */
    @Override
    public void setValidationErrors(ValidationErrors validationErrors) {
        this.validationErrors = validationErrors;
    }

}
