package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.broadinstitute.gpinformatics.mercury.boundary.authentication.AuthorizationService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * AuthorizationManager provides the {@link AuthorizationFilter AuthorizationFilter} with a way to access the
 * {@link AuthorizationService} in order to perform authorization and authentication logic on the pages accessed
 *
 * @author Scott Matthews
 */

@RequestScoped
public class AuthorizationManager {
    @Inject private AuthorizationService authSvc;

    /**
     * isUserAuthorized will determine if the user that us currently logged into the application is authorized to
     * access the current page.
     *
     * @param pageUri page to check
     * @param requestIn contains user data with roles
     * @return true if user is authorized for the page
     */
    public boolean isUserAuthorized(String pageUri, HttpServletRequest requestIn) {
        boolean authorized = false;

        if (authSvc.isPageProtected(pageUri)) {
            Collection<String> authorizationGroups = authSvc.retrieveAuthorizedRoles(pageUri);
            for (String currentGroup : authorizationGroups) {
                if (requestIn.isUserInRole(currentGroup) || currentGroup.equals("All")) {
                    authorized = true;
                    break;
                }
            }
        } else {
            authorized = true;
        }

        return authorized;
    }

    /**
     *
     * @param pageUri page to check
     * @return true if the current page needs authentication
     */
    public boolean isPageProtected(String pageUri) {
        return authSvc.isPageProtected(pageUri);
    }
}
