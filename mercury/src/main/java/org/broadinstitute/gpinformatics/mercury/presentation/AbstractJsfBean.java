package org.broadinstitute.gpinformatics.mercury.presentation;

import javax.annotation.Nullable;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;

/**
 * Class to define some common useful functions to remove boiler plate code for JSF functionality.  This class is
 * purposely not made serializable so that the implementing classes can choose to do so if they require it.
 */
public abstract class AbstractJsfBean {
    public String redirect(String result) {
        return result + "?faces-redirect=true&includeViewParams=true";
    }

    /**
     * Add a flash message so it can be used after a redirect.
     *
     * @param message the message to add
     */
    public static void addFlashMessage(String message) {
        Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
        flash.setKeepMessages(true);
        flash.setRedirect(true);
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(message));
    }

    /**
     * Add a flash error message so it can be used after a redirect.
     *
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    public static void addFlashErrorMessage(String summary, String detail) {
        Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
        flash.setKeepMessages(true);
        flash.setRedirect(true);
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    /**
     * Adds a global FacesMessage with the INFO severity level and summary and detail messages.
     *
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    protected void addInfoMessage(String summary, String detail) {
        addMessage(null, FacesMessage.SEVERITY_INFO, summary, detail);
    }

    /**
     * Adds a global FacesMessage with the WARN severity level and summary and detail messages.
     *
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    protected void addWarnMessage(String summary, String detail) {
        addMessage(null, FacesMessage.SEVERITY_WARN, summary, detail);
    }

    /**
     * Adds a global FacesMessage with the ERROR severity level and summary and detail messages.
     *
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    protected void addErrorMessage(String summary, String detail) {
        addMessage(null, FacesMessage.SEVERITY_ERROR, summary, detail);
    }

    /**
     * Adds a global FacesMessage with the FATAL severity level and summary and detail messages.
     *
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    protected void addFatalMessage(String summary, String detail) {
        addMessage(null, FacesMessage.SEVERITY_FATAL, summary, detail);
    }

    /**
     * Adds a FacesMessage to the client ID, if not null, with the appropriate severity level and summary and detail
     * messages.  If the client ID is null, then it adds the message globally on the page.
     *
     * @param clientId The client ID
     * @param severity The message notification level
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    private static void addMessage(@Nullable String clientId, FacesMessage.Severity severity, String summary, String detail) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(clientId, new FacesMessage(severity, summary, detail));
    }

    /**
     * Adds an INFO severity level FacesMessage to the context assigning it to the client ID.  This method should be
     * used judiciously, as most messages are global and the client ID should be null.
     *
     * @param clientId The client ID
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    protected void addInfoMessage(String clientId, String summary, String detail) {
        addMessage(clientId, FacesMessage.SEVERITY_INFO, summary, detail);
    }

    /**
     * Adds a WARN severity level FacesMessage to the context assigning it to the client ID.
     *
     * @param clientId The client ID
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    protected void addWarnMessage(String clientId, String summary, String detail) {
        addMessage(clientId, FacesMessage.SEVERITY_WARN, summary, detail);
    }

    /**
     * Adds an ERROR severity level FacesMessage to the context assigning it to the client ID.
     *
     * @param clientId The client ID
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    protected void addErrorMessage(String clientId, String summary, String detail) {
        addMessage(clientId, FacesMessage.SEVERITY_ERROR, summary, detail);
    }

    /**
     * Adds a FATAL severity level FacesMessage to the context assigning it to the client ID.
     *
     * @param clientId The client ID
     * @param summary The displayed message on the web page
     * @param detail The detailed information of the message
     */
    protected void addFatalMessage(String clientId, String summary, String detail) {
        addMessage(clientId, FacesMessage.SEVERITY_FATAL, summary, detail);
    }
}
