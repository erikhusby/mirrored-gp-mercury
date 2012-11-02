package org.broadinstitute.gpinformatics.mercury.presentation.logout;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Scott Matthews
 *         Date: 4/23/12
 *         Time: 11:47 AM
 */

@Named
@RequestScoped
public class LogoutHandler extends AbstractJsfBean {

    @Inject
    private Log logger;

    @Inject
    private UserBean userBean;

    public String logout() {
        // If logout is successful, the redirect location is irrelevant since our authorization filter
        // will force the user to the login page.
        String result = redirect("/index");

        FacesContext context = FacesContext.getCurrentInstance();

        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        try {
            logger.debug("Attempting to sign out");
            request.logout();
            userBean.setBspUser(null);
        } catch (ServletException ex) {
            logger.error("Sign out failed", ex);
            result = request.getRequestURI();
        }

        return result;
    }
}
