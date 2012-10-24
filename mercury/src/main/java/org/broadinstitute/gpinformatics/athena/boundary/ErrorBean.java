package org.broadinstitute.gpinformatics.athena.boundary;

import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * Error bean for managing problems.
 *
 * @author Michael Dinsmore
 */
@Named
@RequestScoped
public class ErrorBean {
    private static final String BR = "\n";

    /**
     * Get the full stack trace.
     *
     * @return The full stack trace
     */
    public String getStackTrace() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, Object> map = context.getExternalContext().getRequestMap();
        Throwable throwable = (Throwable) map.get("javax.servlet.error.exception");
        StringBuilder builder = new StringBuilder();
        builder.append(throwable.getMessage()).append(BR);

        for (StackTraceElement element : throwable.getStackTrace()) {
            builder.append(element).append(BR);
        }

        return builder.toString();
    }

    /**
     * Get the message for display.
     *
     * @return The exception message
     */
    public String getMessage() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, Object> map = context.getExternalContext().getRequestMap();
        Throwable throwable = (Throwable) map.get("javax.servlet.error.exception");
        return throwable.getMessage();
    }
}
