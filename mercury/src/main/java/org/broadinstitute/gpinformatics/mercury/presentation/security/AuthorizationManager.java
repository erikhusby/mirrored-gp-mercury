package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.broadinstitute.gpinformatics.mercury.boundary.authentication.AuthenticationService;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 *
 * AuthorizationManager provides the {@link AuthorizationFilter AuthorizationFilter} with a way to access the
 * {@link AuthenticationService} in order to perform authorization and authentication logic on the pages accessed
 *
 * @author Scott Matthews
 *         Date: 5/2/12
 *         Time: 12:03 PM
 */

@ManagedBean
@RequestScoped
public class AuthorizationManager {

    @Inject private AuthenticationService authSvc;

    /**
     *
     * isUserAuthorized will determine if the user that us currently logged into the application is authorized to
     * access the current page.
     *
     * @param pageUri
     * @param requestIn
     * @return
     */
    public boolean isUserAuthorized(String pageUri, HttpServletRequest requestIn) {

        boolean authorized = false;

        HttpServletRequest request =requestIn;
        if(authSvc.isPageProtected(pageUri)) {

            Collection<String> authorizationGrps = authSvc.retrieveAuthorizedRoles(pageUri);
            for(String currGrp:authorizationGrps) {
                if(request.isUserInRole(currGrp) || currGrp.equals("All")) {
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
     * isPageProtected determines if the current page needs authentication
     *
     * @param pageUri
     * @param requestIn
     * @return
     */
    public boolean isPageProtected(String pageUri, HttpServletRequest requestIn) {
        return authSvc.isPageProtected(pageUri);
    }
}
