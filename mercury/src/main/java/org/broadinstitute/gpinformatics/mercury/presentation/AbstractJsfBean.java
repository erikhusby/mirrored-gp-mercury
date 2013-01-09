package org.broadinstitute.gpinformatics.mercury.presentation;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.common.BusinessKeySorter;

import javax.annotation.Nullable;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class to define some common useful functions to remove boiler plate code for JSF functionality.  This class is
 * purposely not made serializable so that the implementing classes can choose to do so if they require it.
 */
public abstract class AbstractJsfBean {

    private static final BusinessKeySorter businessKeySorter = new BusinessKeySorter();

    /**
     * Global redirect helper to preserve the usage of View params between page requests
     *
     * @param result
     * @return
     */
    public String redirect(String result) {
        return result + "?faces-redirect=true&includeViewParams=true";
    }

    /**
     * Extends the basic options of the {@link #redirect(String, String...)} method to allow users to add
     * 1->many parameters to the URL
     *
     * @param result
     * @param params
     * @return
     */
    public String redirect(String result, String... params) {
        StringBuilder initialResult = new StringBuilder(redirect(result));

        for (String param : params) {
            initialResult.append("&") .append(param);
        }

        return initialResult.toString();
    }

    /**
     * Adds a global FacesMessage with the INFO severity level and summary message.
     *
     * @param summary The displayed message on the web page
     */
    protected void addInfoMessage(String summary) {
        addMessage(null, FacesMessage.SEVERITY_INFO, summary, summary);
    }

    /**
     * Adds a global FacesMessage with the WARN severity level and summary message.
     *
     * @param summary The displayed message on the web page
     */
    protected void addWarnMessage(String summary) {
        addMessage(null, FacesMessage.SEVERITY_WARN, summary, summary);
    }

    /**
     * Adds a global FacesMessage with the ERROR severity level and summary message.
     *
     * @param summary The displayed message on the web page
     */
    protected void addErrorMessage(String summary) {
        addMessage(null, FacesMessage.SEVERITY_ERROR, summary, summary);
    }

    /**
     * Adds a global FacesMessage with the FATAL severity level and summary message.
     *
     * @param summary The displayed message on the web page
     */
    protected void addFatalMessage(String summary) {
        addMessage(null, FacesMessage.SEVERITY_FATAL, summary, summary);
    }

    /**
     * Adds a FacesMessage to the client ID, if not null, with the appropriate severity level and summary and detail
     * messages.  If the client ID is null, then it adds the message globally on the page.
     *
     * @param clientId The client ID
     * @param severity The message notification level
     * @param summary  The displayed message on the web page
     * @param detail   The detailed information of the message
     */
    protected static void addMessage(@Nullable String clientId, FacesMessage.Severity severity, String summary,
                                     String detail) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(clientId, new FacesMessage(severity, summary, detail));
    }

    /**
     * Adds an INFO severity level FacesMessage to the context assigning it to the client ID.  This method should be
     * used judiciously, as most messages are global and the client ID should be null.
     *
     * @param clientId The client ID
     * @param summary  The displayed message on the web page
     * @param detail   The detailed information of the message
     */
    protected void addInfoMessage(String clientId, String summary, String detail) {
        addMessage(clientId, FacesMessage.SEVERITY_INFO, summary, detail);
    }

    /**
     * Adds a WARN severity level FacesMessage to the context assigning it to the client ID.
     *
     * @param clientId The client ID
     * @param summary  The displayed message on the web page
     * @param detail   The detailed information of the message
     */
    protected void addWarnMessage(String clientId, String summary, String detail) {
        addMessage(clientId, FacesMessage.SEVERITY_WARN, summary, detail);
    }

    /**
     * Adds an ERROR severity level FacesMessage to the context assigning it to the client ID.
     *
     * @param clientId The client ID
     * @param summary  The displayed message on the web page
     * @param detail   The detailed information of the message
     */
    protected void addErrorMessage(String clientId, String summary, String detail) {
        addMessage(clientId, FacesMessage.SEVERITY_ERROR, summary, detail);
    }

    /**
     * Adds a FATAL severity level FacesMessage to the context assigning it to the client ID.
     *
     * @param clientId The client ID
     * @param summary  The displayed message on the web page
     * @param detail   The detailed information of the message
     */
    protected void addFatalMessage(String clientId, String summary, String detail) {
        addMessage(clientId, FacesMessage.SEVERITY_FATAL, summary, detail);
    }

    /**
     * This is a service for autocomplete removal of duplicates. It assumes that there will only be one
     * duplicate because it's called just after an item is added. If we had the item that was just added,
     * we could do a simpler search.
     *
     * @param objects     The list of items in the autocomplete
     * @param componentId The id of the component
     * @param <T>         The correct object
     */
    public <T> void removeDuplicates(List<T> objects, String componentId) {

        // The objects ARE THE ACTUAL MEMBERS that back the autocomplete lists.
        Set<T> seenObjects = new HashSet<T>();

        for (T object : objects) {
            if (!seenObjects.add(object)) {
                // Since this is called after every add, there can only be one duplicate.
                objects.remove(object);

                String name;
                if (object instanceof BspUser) {
                    // We do not own BSP User, so special case this.
                    BspUser user = (BspUser) object;
                    name = user.getFirstName() + " " + user.getLastName();
                } else if (object instanceof Displayable) {
                    Displayable displayable = (Displayable) object;
                    name = displayable.getDisplayName();
                } else {
                    name = object.toString();
                }

                String message = name + " was already in the list";
                addInfoMessage(componentId, "Duplicate item removed.", message);
                return;
            }
        }
    }

    public BusinessKeySorter getBusinessKeySorter() {
        return businessKeySorter;
    }
}
