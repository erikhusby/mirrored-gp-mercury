package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.application.NavigationHandler;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

/**
 * A phase listener based authorization system.  This works better than using the filter since we have
 * access to JSF APIs when this code executes.
 *
 * @author pshapiro
 */
@RequestScoped
public class AuthorizationListener extends AbstractJsfBean implements PhaseListener {
    private static final Logger logger = Logger.getLogger(AuthorizationListener.class);

    public static final String HOME_PAGE = "/index.jsp";

    @Override
    public void afterPhase(PhaseEvent phaseEvent) {
        FacesContext context = phaseEvent.getFacesContext();
        HttpServletRequest request = (HttpServletRequest)context.getExternalContext().getRequest();
        String pageUri = request.getServletPath();

        AuthorizationManager authorizationManager = ServiceAccessUtility.getBean(AuthorizationManager.class);
        boolean authorized = authorizationManager.isUserAuthorized(pageUri, request);

        if (!authorized) {
            String errorMessage = request.getRemoteUser() + " doesn't have permission to access the page '" + pageUri + "'";
            logger.warn(errorMessage);

            addErrorMessage("You do not have permission to access the page '" + pageUri + "'.");
            NavigationHandler nh = context.getApplication().getNavigationHandler();
            nh.handleNavigation(context, null, HOME_PAGE);
        }
    }

    @Override
    public void beforePhase(PhaseEvent phaseEvent) {
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }
}
