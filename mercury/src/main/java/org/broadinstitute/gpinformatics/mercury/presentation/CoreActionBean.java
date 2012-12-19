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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.BuildInfoBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

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

    public static final String CREATE = "Create ";
    public static final String EDIT = "Edit : ";

    public boolean hasFlashError;

    public static final String FLASH_ERROR = "flash_error";

    public static final String FLASH_MESSAGE = "flash_message";

    private CoreActionBeanContext context;

    private String submitString;

    private Map<Long, String> fullNameMap;

    @Inject
    private BuildInfoBean buildInfoBean;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    private DateRangeSelector dateRange;

    /**
     * @return the context
     */
    @Override
    public CoreActionBeanContext getContext() {
        return this.context;
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
            if (error != null)
                context.getValidationErrors().addGlobalError(error);
            Message message = (Message) context.getRequest().getAttribute(FLASH_MESSAGE);
            if (message != null)
                context.getMessages().add(message);
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
    @SuppressWarnings("unchecked")
    protected Resolution getSourcePageResolution() {
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
            return ((ForwardResolution) res).addParameters(getContext().getRequest().getParameterMap());
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
    protected void addMessage(String message) {
        this.getContext().getMessages().add(new SimpleMessage(message));
    }

    /**
     * Convenience method for adding a SimpleError to the context.
     *
     * @param field        The form field
     * @param errorMessage The message to put into a SimpleError
     */
    protected void addValidationError(String field, String errorMessage) {
        this.getContext().getValidationErrors().add(field, new SimpleError(errorMessage));
    }


    /**
     * Convenience method for adding a SimpleError to the context.
     *
     * @param errorMessage The message to put into a SimpleError
     */
    public void addGlobalValidationError(String errorMessage) {
        this.getContext().getValidationErrors().add(ValidationErrors.GLOBAL_ERROR, new SimpleError(errorMessage));
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

    public void setDateRange(DateRangeSelector dateRange) {
        this.dateRange = dateRange;
    }

    public DateRangeSelector getDateRange() {
        return dateRange;
    }

    public String getSubmitString() {
        return submitString;
    }

    public void setSubmitString(String submitString) {
        this.submitString = submitString;
    }

    public boolean isCreating() {
        return submitString.startsWith(CREATE);
    }

    public Map<Long, String> getFullNameMap() {
        if (fullNameMap == null) {
            fullNameMap = bspUserList.getFullNameMap();
        }

        return fullNameMap;
    }

    /**
     * Set HTTP response headers appropiately for a file download
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
    public void setFileDownloadHeaders(String contentType,
        String fileName) {
        // Some applications also muck with the character encoding
        // and cache control headers on file download, but it
        // doesn't seem that we need this.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        if (fileName == null) {
            getContext().getResponse().setContentType(contentType);
        } else {
            String doubleQuotedFileName =
                    "\"" + fileName + "\"";
            getContext().getResponse().setContentType(contentType + "; file=" + doubleQuotedFileName);
            getContext().getResponse().setHeader("Content-Disposition", "attachment; filename=" + doubleQuotedFileName);
        }
    }
}