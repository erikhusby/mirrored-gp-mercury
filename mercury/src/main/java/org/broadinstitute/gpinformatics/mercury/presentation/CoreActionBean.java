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

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
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

    public boolean hasFlashError;

    public static final String FLASH_ERROR = "flash_error";

    public static final String FLASH_MESSAGE = "flash_message";

    private CoreActionBeanContext context;

    @Inject
    private BuildInfoBean buildInfoBean;

    @Inject
    private UserBean userBean;

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
    protected void addGlobalValidationError(String errorMessage) {
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
}