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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.action.OnwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.controller.FlashScope;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.exception.SourcePageNotFoundException;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.BuildInfoBean;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObjectFinder;
import org.broadinstitute.gpinformatics.infrastructure.presentation.JiraLink;
import org.broadinstitute.gpinformatics.infrastructure.presentation.PortalLink;
import org.broadinstitute.gpinformatics.infrastructure.presentation.SampleLink;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SAPServiceFailure;
import org.owasp.encoder.Encode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletResponse;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/*
 * This class is a core class to extend Stripes actions from, providing some basic functionality for
 * other classes to leverage.
 *
 * Converted this from abstract because the report.jsp needs to instantiate to get context info
 */
public abstract class CoreActionBean implements ActionBean, MessageReporter {
    private static final Log log = LogFactory.getLog(CoreActionBean.class);

    public static final String ERROR_CONTACT_SUPPORT =
        "Please contact support using the <span class='badge'>Feedback</span> link above.";
    public static final String DATE_PATTERN = "MM/dd/yyyy";
    private static final String DATE_TIME_PATTERN = "MM/dd/yyyy HH:mm";
    private static final String PRECISE_DATE_TIME_PATTERN = "MM/dd/yyyy HH:mm:ss.S";

    // These are used for the create and edit strings in the UI and the submit string.
    public static final String CREATE = "Create ";
    public static final String EDIT = "Edit ";

    public static final String CREATE_ACTION = "create";
    public static final String EDIT_ACTION = "edit";
    public static final String LIST_ACTION = "list";
    public static final String SAVE_ACTION = "save";
    public static final String VIEW_ACTION = "view";
    public static final String SAP_SERVICE_FAILURE = "Unable to communicate with SAP.";

    public boolean hasFlashError;

    public static final String FLASH_ERROR = "flash_error";

    public static final String FLASH_MESSAGE = "flash_message";

    private CoreActionBeanContext context;

    private String submitString;

    @Inject
    private BuildInfoBean buildInfoBean;

    @Inject
    protected UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private JiraLink jiraLink;

    @Inject
    private PortalLink portalLink;

    @Inject
    private SampleLink.Factory sampleLinkFactory;

    // These fields are generic title fields used by the master layout to determine what links to show in the title
    // pull right field.
    private String createTitle;
    private String editTitle;
    private String editBusinessKeyName;

    // The date range widget can be used by simply adding a div with a class of dateRangeDiv. If only one date is
    // needed, this will work for any action bean. If more are needed, then ids should be used and configured directly.
    private DateRangeSelector dateRange = new DateRangeSelector(DateRangeSelector.THIS_MONTH);

    @SuppressWarnings("CdiInjectionPointsInspection")
    protected QuoteService quoteService;
    protected SapIntegrationService sapService;

    public enum ErrorLevel {
        WARNING,
        ERROR,
        ;
    }

    // Needed for managed bean.
    public CoreActionBean() {
    }

    protected CoreActionBean(String createTitle, String editTitle, String editBusinessKeyName) {
        this.createTitle = createTitle;
        this.editTitle = editTitle;
        this.editBusinessKeyName = editBusinessKeyName;
    }

    /**
     * @return the context
     */
    @Override
    public CoreActionBeanContext getContext() {
        return context;
    }

    public String getCreateAction() {
        return CREATE_ACTION;
    }

    public String getEditAction() {
        return EDIT_ACTION;
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
            List<ValidationError> errors = (List<ValidationError>) context.getRequest().getAttribute(FLASH_ERROR);

            if (errors != null) {
                for (ValidationError error : errors) {
                    context.getValidationErrors().addGlobalError(error);
                }
            }

            List<Message> messages = (List<Message>) context.getRequest().getAttribute(FLASH_MESSAGE);

            if (messages != null) {
                for (Message message : messages) {
                    context.getMessages().add(message);
                }
            }
        }
    }

    /**
     * Adds a single error message into the flash scope to be shown on the next request.
     *
     * @param error ValidationError message to be flashed
     */
    protected void flashErrorMessage(ValidationError error) {
        FlashScope scope = FlashScope.getCurrent(context.getRequest(), true);
        hasFlashError = true;

        List<ValidationError> errors = (List<ValidationError>) scope.get(FLASH_ERROR);

        if (errors == null) {
            errors = new ArrayList<>();
        }

        errors.add(error);

        scope.put(FLASH_ERROR, errors);
    }

    /**
     * Checks for any validation errors in the flash scope and in the validation errors scope
     *
     * @return returns true if any errors are found in either place.  Returns false otherwise.
     */
    public boolean hasErrors() {
        FlashScope scope = FlashScope.getCurrent(context.getRequest(), true);
        List<ValidationError> errors = (List<ValidationError>) scope.get(FLASH_ERROR);

        return !(getContext().getValidationErrors().isEmpty() && (errors == null || errors.isEmpty()));
    }

    /**
     * Adds a single message into the flash scope to be shown on the next request.
     *
     * @param message Message  to be flashed
     */
    protected void flashMessage(Message message) {
        FlashScope scope = FlashScope.getCurrent(context.getRequest(), true);

        List<Message> messages = (List<Message>) scope.get(FLASH_MESSAGE);

        if (messages == null) {
            messages = new ArrayList<>();
        }

        messages.add(message);
        scope.put(FLASH_MESSAGE, messages);
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
     * Create a 'safe' message for stripes message reporting. If no arguments are supplied, we format the entire
     * string as an argument to MessageFormat, to avoid issues with strings that inadvertently include format
     * patterns, such as {, }, or '.
     *
     * @param message   the message to format safely
     * @param arguments the arguments to the message format string
     *
     * @return the stripes message object
     */
    protected static Message createSafeMessage(String message, Object... arguments) {
        if (arguments.length == 0) {
            return new SimpleMessage("{0}", message);
        }
        return new SimpleMessage(message, arguments);
    }

    /**
     * Create a 'safe' message for stripes message reporting. If no arguments are supplied, we format the entire
     * string as an argument to MessageFormat, to avoid issues with strings that inadvertently include format
     * patterns, such as {, }, or '.
     *
     * @param message   the message to format safely
     * @param arguments the arguments to the message format string
     *
     * @return the stripes message object
     */
    protected static SimpleError createSafeErrorMessage(String message, Object... arguments) {
        if (arguments.length == 0) {
            return new SimpleError("{2}", message);
        }
        return new SimpleError(message, arguments);
    }

    /**
     * Convenience method for adding a SimpleMessage to the context.
     *
     * @param message The message to put into a SimpleMessage
     */
    @Override
    public String addMessage(String message, Object... arguments) {
        Message safeMessage = createSafeMessage(message, arguments);
        getContext().getMessages().add(safeMessage);
        return safeMessage.getMessage(getContext().getLocale());
    }

    /**
     * Convenience method for adding a SimpleError to the context.
     *
     * @param field        The form field
     * @param errorMessage The message to put into a SimpleError
     */
    protected void addValidationError(String field, String errorMessage, Object... arguments) {
        getContext().getValidationErrors().add(field, createSafeErrorMessage(errorMessage, arguments));
    }

    /**
     * Convenience method used only for testing.
     */
    public ValidationErrors getValidationErrors() {
        return getContext().getValidationErrors();
    }

    /**
     * Convenience method used only for testing.
     */
    public void clearValidationErrors() {
        getContext().getValidationErrors().clear();
    }

    /**
     * Support for converting a message collection to ValidationErrors/SimpleErrors.
     */
    protected void addMessages(MessageCollection messages) {
        for (String error : messages.getErrors()) {
            addGlobalValidationError(error);
        }
        for (String warn : messages.getWarnings()) {
            addMessage("Warning: " + warn);
        }
        for (String info : messages.getInfos()) {
            addMessage(info);
        }
    }

    /**
     * Convenience method for adding an error message to the context.  The message and its arguments, if any, is
     * formatted using MessageFormat.format().
     *
     * @param errorMessage The message to put into a SimpleError
     * @param arguments    optional message parameters
     */
    public void addGlobalValidationError(String errorMessage, Object... arguments) {
        getContext().getValidationErrors().addGlobalError(createSafeErrorMessage(errorMessage, arguments));
    }

    /**
     * Convenience method for adding error messages to the context.
     *
     * @param errorMessages List of error messages be returned to the user.
     */
    public void addGlobalValidationErrors(List<String> errorMessages) {
        for (String error : errorMessages) {
            addGlobalValidationError(error);
        }
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
     * @return formatted messages collected in the context
     */
    public List<String> getFormattedMessages() {
        return transformMessages(getContext().getMessages());
    }

    private List<String> transformMessages(List<? extends Message> messages) {
        return new ArrayList<>(Collections2.transform(messages, new Function<Message, String>() {
            @Override
            public String apply(@Nullable Message input) {
                if (input != null) {
                    return input.getMessage(getContext().getLocale());
                }
                return null;
            }
        }));
    }

    /**
     * @return formatted errors collected in the context
     */
    public List<String> getFormattedErrors() {
        final List<String> formattedMessages = new ArrayList<>();
        for (String errorKey : getValidationErrors().keySet()) {
            List<ValidationError> validationErrors = getValidationErrors().get(errorKey);
            formattedMessages.addAll(transformMessages(validationErrors));
        }
        return formattedMessages;
    }

    /**
     * Get the build info bean.
     *
     * @return the injected BuildInfoBean
     */
    public BuildInfoBean getBuildInfoBean() {
        return buildInfoBean;
    }

    public String getError(Map<String, Object> requestScope) {
        return Encode.forHtml(((Throwable)requestScope.get(RequestDispatcher.ERROR_EXCEPTION)).getMessage());
    }

    public StackTraceElement[] getStackTrace(Map<String, Object> requestScope) {
        return ((Throwable)requestScope.get(RequestDispatcher.ERROR_EXCEPTION)).getStackTrace();
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

        return bspUser.getFullName();
    }

    public String getUserFullNameOrBlank(long userId) {
        BspUser bspUser = bspUserList.getById(userId);
        return (bspUser != null) ? bspUser.getFullName() : "";
    }

    /**
     * Given a list of user IDs, return a comma separated list of full user names.
     *
     * @param userIds list of user IDs
     *
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
     * for this action bean.
     *
     * @param contentType The MIME type of the response or null.
     * @param fileName    The name of the downloaded file or null.
     *
     * @see #setFileDownloadHeaders(String, String, javax.servlet.http.HttpServletResponse)
     */
    public void setFileDownloadHeaders(String contentType, String fileName) {
        setFileDownloadHeaders(contentType, fileName, getContext().getResponse());
    }

    /**
     * Set HTTP response headers appropriately for a file download
     * (as opposed to a normal HTTP response).
     * <p/>
     * The content type may be null, in which case we default it
     * to application/octet-stream, which will typically cause the
     * receiving browser to offer to save the file or allow the
     * user to pick an application to use to open it.
     * <p/>
     * The file name argument specifies the default file name the
     * browser should give the file on the client.  If the file
     * name is null, we do not send a file name in the headers.
     * In this case, the browser will typically default the file
     * name based on the URL used to download the file.
     *
     * @param contentType The MIME type of the response or null.
     * @param fileName    The name of the downloaded file or null.
     * @param response    The HTTP response to set headers for.
     */
    public static void setFileDownloadHeaders(String contentType, String fileName, HttpServletResponse response) {
        // Some applications also muck with the character encoding
        // and cache control headers on file download, but it
        // doesn't seem that we need this.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        if (fileName == null) {
            response.setContentType(contentType);
        } else {
            String doubleQuotedFileName = '"' + fileName + '"';
            response.setContentType(contentType + "; file=" + doubleQuotedFileName);
            response.setHeader("Content-Disposition", "attachment; filename=" + doubleQuotedFileName);
        }
    }

    /**
     * Helper method to create a streaming text resolution, used to create the response from an AJAX call.
     *
     * @param text text to return
     *
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
     *
     * @param jiraTicketKey the key
     *
     * @return the JIRA URL
     */
    public String jiraUrl(String jiraTicketKey) {
        return jiraLink.browseUrl(jiraTicketKey);
    }

    /**
     * Given a Portal requisition key, create a URL to open the requisition in the Portal.
     *
     * @param requisitionKey the key
     *
     * @return the Portal URL
     */
    public String portalRequisitionUrl(String requisitionKey) {
        return portalLink.browseRequisitionUrl(requisitionKey);
    }

    /**
     * @return The name of the business key parameter used in URLs to edit the object being viewed.
     */
    public String getEditBusinessKeyName() {
        return editBusinessKeyName;
    }

    /**
     * @return The title of the link when editing the object.
     */
    public String getEditTitle() {
        return editTitle;
    }

    /**
     * @return The title of the page when creating the object.
     */
    public String getCreateTitle() {
        return createTitle;
    }

    protected Quote validateQuote(ProductOrder productOrder) {
        Quote quoteDetails = null;
        try {
            quoteDetails = productOrder.getQuote(quoteService);
        } catch (QuoteServerException e) {
            addGlobalValidationError("The quote ''{2}'' is not valid: {3}", productOrder.getQuoteId(), e.getMessage());
        } catch (QuoteNotFoundException e) {
            addGlobalValidationError("The quote ''{2}'' was not found ", productOrder.getQuoteId());
        }
        return quoteDetails;
    }

    public SapQuote getSapQuote(ProductOrder productOrder) throws SAPIntegrationException {
        SapQuote sapQuote=null;
        try {
            sapQuote = productOrder.getSapQuote(sapService);
        } catch (SAPServiceFailure e) {
            addGlobalValidationError(SAP_SERVICE_FAILURE);
            log.error(SAP_SERVICE_FAILURE, e);
        }
        return sapQuote;
    }

    protected SapQuote validateSapQuote(ProductOrder productOrder) {
        SapQuote quoteDetails = null;
        try {
            quoteDetails = productOrder.getSapQuote(sapService);
        } catch (SAPIntegrationException e) {
            addGlobalValidationError("The quote ''{2}'' was not found ", productOrder.getQuoteId());
        } catch (SAPServiceFailure e) {
            addGlobalValidationError(SAP_SERVICE_FAILURE);
        }
        return quoteDetails;
    }

    /**
     * @return By default, edit is always allowed. Subclasses can put protections around that by overriding it.
     */
    public boolean isCreateAllowed() {
        return true;
    }

    /**
     * @return By default, edit is always allowed. Subclasses can put protections around that by overriding it.
     */
    public boolean isEditAllowed() {
        return true;
    }

    public String getDatePattern() {
        return DATE_PATTERN;
    }

    public String getDateTimePattern() {
        return DATE_TIME_PATTERN;
    }

    public String getPreciseDateTimePattern() {
        return PRECISE_DATE_TIME_PATTERN;
    }

    /**
     * Given a sample, return an object that can be used to generate a hyperlink in HTML.
     *
     * @return the link object
     */
    public SampleLink getSampleLink(@Nonnull AbstractSample sample) {
        return sampleLinkFactory.create(sample);
    }

    public DisplayableItem getDisplayableItemInfo(String businessKey, BusinessObjectFinder dao) {
        BusinessObject object = dao.findByBusinessKey(businessKey);
        if (object == null) {
            // Object of that business key was not found.
            return null;
        }

        DisplayableItem displayableItem = new DisplayableItem(object.getBusinessKey(), object.getName());


        return displayableItem;
    }

    public Collection<DisplayableItem> makeDisplayableItemCollection(List<? extends BusinessObject> items) {
        Collection<DisplayableItem> displayableItems = new ArrayList<>(items.size());

        for (BusinessObject item : items) {
            displayableItems.add(new DisplayableItem(item.getBusinessKey(), item.getName()));
        }
        return displayableItems;
    }

    @Inject
    public void setQuoteService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Inject
    public void setSapService(SapIntegrationService sapService) {
        this.sapService = sapService;
    }
}
