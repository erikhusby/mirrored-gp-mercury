/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2012 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.mercury.presentation;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.FlashScope;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.exception.SourcePageNotFoundException;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.BuildInfoBean;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

/*
 * This class is a core class to extend Stripes actions from, providing some basic functionality for
 * other classes to leverage.
 *
 * Converted this from abstract because the report.jsp needs to instantiate to get context info
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class CoreActionBean implements ActionBean {
    private static final Log log = LogFactory.getLog(CoreActionBean.class);

    // These are used for the create and edit strings in the UI and the submit string.
    public static final String CREATE = "Create ";
    public static final String EDIT = "Edit ";

    public static final String CREATE_ACTION = "create";
    public static final String EDIT_ACTION = "edit";
    public static final String LIST_ACTION = "list";
    public static final String SAVE_ACTION = "save";
    public static final String VIEW_ACTION = "view";

    public boolean hasFlashError;

    public static final String FLASH_ERROR = "flash_error";

    public static final String FLASH_MESSAGE = "flash_message";

    private CoreActionBeanContext context;

    private String submitString;

    @Inject
    private BuildInfoBean buildInfoBean;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private JiraLink jiraLink;

    // The date range widget can be used by simply adding a div with a class of dateRangeDiv. If only one date is
    // needed, this will work for any action bean. If more are needed, then ids should be used and configured directly.
    private DateRangeSelector dateRange;

    /**
     * @return the context
     */
    @Override
    public CoreActionBeanContext getContext() {
        return context;
    }

    /**
     * @param context the context to set
     */
    @Override
    public void setContext(ActionBeanContext context) {
        this.context = (CoreActionBeanContext) context;
    }

    /**
     * Retrieves a storage error message from the previous FlashScope and adds it to the context.
     */
    @Before(stages = LifecycleStage.EventHandling)
    public void getErrorAndMessage() {
        if (context != null) {
            ValidationError error = (ValidationError) context.getRequest().getAttribute(FLASH_ERROR);
            if (error != null) {
                context.getValidationErrors().addGlobalError(error);
            }
            Message message = (Message) context.getRequest().getAttribute(FLASH_MESSAGE);
            if (message != null) {
                context.getMessages().add(message);
            }
        }
    }

    /**
     * Places a single error message into the flash scope to be shown on the next request.
     *
     * @param error ValidationError message to be flashed
     */
    protected void flashErrorMessage(ValidationError error) {
        FlashScope scope = FlashScope.getCurrent(context.getRequest(), true);
        hasFlashError = true;
        scope.put(FLASH_ERROR, error);
    }

    /**
     * Places a single message into the flash scope to be shown on the next request.
     *
     * @param message Message  to be flashed
     */
    protected void flashMessage(Message message) {
        FlashScope scope = FlashScope.getCurrent(context.getRequest(), true);
        scope.put(FLASH_MESSAGE, message);
    }

    /**
     * @return Resolution
     */
    public Resolution getSourcePageResolution() {
        Resolution res;
        try {
            res = getContext().getSourcePageResolution();
        } catch (SourcePageNotFoundException spnfe) {
            if (log.isTraceEnabled()) {
                log.trace("Couldn't find the source page resolution the normal way", spnfe);
            }
            res = new ForwardResolution(getContext().getRequest().getServletPath());
        }

        if (res instanceof OnwardResolution) {
            return ((OnwardResolution<?>) res).addParameters(getContext().getRequest().getParameterMap());
        }
        return res;
    }

    /**
     * Load file data from a form upload.
     *
     * @param inFile The file
     * @return String from file contents.
     * @throws java.io.IOException Any errors that occur
     */
    protected InputStream importFile(FileBean inFile) throws IOException {
        InputStream istream = null;

        try {
            istream = inFile.getInputStream();
        } catch (Exception e) {
            getContext().getValidationErrors().addGlobalError(
                    new LocalizableError("Sorry, there was a problem reading the file you supplied"));
        } finally {
            if (inFile != null) {
                inFile.delete();
            }
        }
        return istream;
    }

    /**
     * Convenience method for adding a SimpleMessage to the context.
     *
     * @param message The message to put into a SimpleMessage
     */
    protected void addMessage(String message, Object... arguments) {
        flashMessage(new SimpleMessage(message, arguments));
    }

    /**
     * Convenience method for adding a SimpleError to the context.
     *
     * @param field        The form field
     * @param errorMessage The message to put into a SimpleError
     */
    protected void addValidationError(String field, String errorMessage, Object... arguments) {
        getContext().getValidationErrors().add(field, new SimpleError(errorMessage, arguments));
    }

    /**
     * Add an error that must be included literally in the response, without any formatting.  This should
     * only be used in the case where the message may contain characters that will cause MessageFormat.format
     * to generate errors.
     * @param message the message to include.
     */
    public void addLiteralErrorMessage(String message) {
        addGlobalValidationError("{2}", message);
    }

    /**
     * Convenience method for adding an error message to the context.  The message and its arguments, if any, is
     * formatted using MessageFormat.format().
     *
     * @param errorMessage The message to put into a SimpleError
     * @param arguments optional message parameters
     */
    public void addGlobalValidationError(String errorMessage, Object... arguments) {
        flashErrorMessage(new SimpleError(errorMessage, arguments));
    }

    /**
     * Manage errors gracefully with this.
     */
    public final String formatValidationErrors(ValidationErrors errors) throws Exception {
        StringBuilder message = new StringBuilder();

        for (List<ValidationError> fieldErrors : errors.values()) {
            for (ValidationError error : fieldErrors) {
                message.append("<div class=\"error\">");
                message.append(error.getMessage(getContext().getLocale()));
                message.append("</div>");
            }
        }

        return message.toString();
    }

    /**
     * If the error list isn't empty then it has errors.
     *
     * @return true if there are some errors
     */
    public boolean hasErrors() {
        return !getContext().getValidationErrors().isEmpty();
    }

    /**
     * Get the build info bean.
     *
     * @return the injected BuildInfoBean
     */
    public BuildInfoBean getBuildInfoBean() {
        return buildInfoBean;
    }

    /**
     * Get the user bean.
     *
     * @return the injected userBean
     */
    public UserBean getUserBean() {
        return userBean;
    }

    /**
     * set the preconfigured, single date range widget.
     *
     * @param dateRange The date range selector to configure
     */
    public void setDateRange(DateRangeSelector dateRange) {
        this.dateRange = dateRange;
    }

    /**
     * @return The preconfigured date range widget
     */
    public DateRangeSelector getDateRange() {
        return dateRange;
    }

    /**
     * @return The string for submitting a save on an object (create or edit)
     */
    public String getSubmitString() {
        return submitString;
    }

    /**
     * Set the string from the UI (if passed in from a hidden field)
     *
     * @param submitString The string
     */
    public void setSubmitString(String submitString) {
        this.submitString = submitString;
    }

    /**
     * @return Utility to figure out whether a save is edit or create
     */
    public boolean isCreating() {
        return submitString != null && submitString.startsWith(CREATE);
    }

    public String getUserFullName(long userId) {
        BspUser bspUser = bspUserList.getById(userId);
        if (bspUser == null) {
            return "(Unknown user: " + userId + ")";
        }

        return bspUser.getFirstName() + " " + bspUser.getLastName();
    }

    /**
     * Given a list of user IDs, return a comma separated list of full user names.
     *
     * @param userIds list of user IDs
     * @return string representation of named users in CSV format
     */
    // Argument type must be Long, not long, to work with Stripes.
    public String getUserListString(Long[] userIds) {
        String userListString = "";

        if (userIds != null) {
            String[] nameList = new String[userIds.length];
            int i = 0;
            for (long userId : userIds) {
                nameList[i++] = getUserFullName(userId);
            }

            userListString = StringUtils.join(nameList, ", ");
        }

        return userListString;
    }

    /**
     * Set HTTP response headers appropriately for a file download
     * (as opposed to a normal HTTP response).
     *
     * The content type may be null, in which case we default it
     * to application/octet-stream, which will typically cause the
     * receiving browser to offer to save the file or allow the
     * user to pick an application to use to open it.
     *
     * The file name argument specifies the default file name the
     * browser should give the file on the client.  If the file
     * name is null, we do not send a file name in the headers.
     * In this case, the browser will typically default the file
     * name based on the URL used to download the file.
     *
     * @param contentType The MIME type of the response or null.
     * @param fileName The name of the downloaded file or null.
     */
    public void setFileDownloadHeaders(String contentType, String fileName) {
        // Some applications also muck with the character encoding
        // and cache control headers on file download, but it
        // doesn't seem that we need this.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        if (fileName == null) {
            getContext().getResponse().setContentType(contentType);
        } else {
            String doubleQuotedFileName = '"' + fileName + '"';
            getContext().getResponse().setContentType(contentType + "; file=" + doubleQuotedFileName);
            getContext().getResponse().setHeader("Content-Disposition", "attachment; filename=" + doubleQuotedFileName);
        }
    }

    /**
     * Helper method to create a streaming text resolution, used to create the response from an AJAX call.
     * @param text text to return
     * @return the resolution.
     */
    public Resolution createTextResolution(String text) {
        return new StreamingResolution("text", new StringReader(text));
    }

    /**
     * Call this method from code where we want to map an empty result to a valid javascript empty string.
     * Without this change, the inserted text is missing.
     */
    public String ensureStringResult(String s) {
        if (s.isEmpty()) {
            return "''";
        }
        return s;
    }

    /**
     * Given a JIRA ticket key, create a URL to open the ticket in JIRA.
     * @param jiraTicketKey the key
     * @return the JIRA URL
     */
    public String jiraUrl(String jiraTicketKey) {
        return jiraLink.browseUrl(jiraTicketKey);
    }
}
