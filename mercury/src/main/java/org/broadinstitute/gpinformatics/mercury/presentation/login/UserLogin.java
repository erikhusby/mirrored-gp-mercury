package org.broadinstitute.gpinformatics.mercury.presentation.login;

/**
 * @author Scott Matthews
 *         Date: 4/2/12
 *         Time: 1:35 PM
 */

import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationFilter;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Named
@RequestScoped
public class UserLogin extends AbstractJsfBean {
    private String username;

    private String password;

    @Inject
    private Log logger;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private Deployment deployment;

    @Inject
    private FacesContext facesContext;

    public static final String PDM_WELCOME_PAGE = "/orders/list";
    public static final String PM_WELCOME_PAGE = "/projects/list";
    public static final String INDEX_PAGE = "/index.xhtml";
    public static final String HOME_PAGE = "/Mercury";
    public static final String INDEX_REDIRECT = "/index";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String authenticateUser() {
        String targetPage;
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            HttpServletRequest request = (HttpServletRequest)context.getExternalContext().getRequest();

            request.login(username, password);
            targetPage = checkTargetForRoleRedirect(request);
            BspUser bspUser = bspUserList.getByUsername(username);
            if (bspUser != null) {
                userBean.setBspUser(bspUser);
            } else {
// FIXME: Temporarily map unknown users to the BSP tester user. Will need to handle this in userBean.
                userBean.setBspUser(bspUserList.getByUsername("tester"));
                // reuse exception handling below
                //throw new ServletException("Login error: couldn't find BspUser: " + username);
            }
            addInfoMessage("Welcome back!", "Sign in successful");

            String previouslyTargetedPage = (String)request.getSession().getAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE);
            if (previouslyTargetedPage != null) {
                // Check for redirect to PM and PDMs landing page
                previouslyTargetedPage = checkTargetForRoleRedirect(previouslyTargetedPage, request);

                request.getSession().setAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE, null);
                try {
                    facesContext.getExternalContext().redirect(previouslyTargetedPage);
                    return null;
                } catch (IOException e) {
                    logger.warn("Could not redirect to: " + previouslyTargetedPage, e);
                }
            }

        } catch (ServletException le) {
            logger.error("ServletException Retrieved: ", le);
            addErrorMessage("The username and password you entered is incorrect.  Please try again.", "Authentication error");
            targetPage = AuthorizationFilter.LOGIN_PAGE;
        }

        return redirect(targetPage);
    }

    private String checkTargetForRoleRedirect(String targetPage, final HttpServletRequest request) {
        StringBuilder newTargetPageBuilder = new StringBuilder(targetPage);
        final boolean hasPMRole = request.isUserInRole( "Mercury-ProductManagers");
        final boolean hasPDMRole = request.isUserInRole( "Mercury-ProjectManagers");

        if (targetPage.endsWith(HOME_PAGE) || targetPage.endsWith(HOME_PAGE + "/")) {
            if ( hasPMRole ) {
                newTargetPageBuilder.append(PM_WELCOME_PAGE);
            }
            if ( hasPDMRole ) {
                newTargetPageBuilder.append(PDM_WELCOME_PAGE);
            }
        } else if ( targetPage.endsWith(INDEX_REDIRECT)  || targetPage.endsWith(INDEX_PAGE) ) {
            if ( hasPMRole ) {
                newTargetPageBuilder =   new StringBuilder(targetPage.replace(INDEX_REDIRECT, PM_WELCOME_PAGE));
            }
            if ( hasPDMRole ) {
                newTargetPageBuilder =    new StringBuilder(targetPage.replace(INDEX_REDIRECT, PDM_WELCOME_PAGE));
            }
        }
        return newTargetPageBuilder.toString();
    }

    private String checkTargetForRoleRedirect(final HttpServletRequest request) {
        String indexRedirect = INDEX_REDIRECT;
        final boolean hasPMRole = request.isUserInRole( "Mercury-ProductManagers");
        final boolean hasPDMRole = request.isUserInRole( "Mercury-ProjectManagers");

        if ( hasPMRole ) {
            indexRedirect =   PM_WELCOME_PAGE;
        }
        if ( hasPDMRole ) {
            indexRedirect =   PDM_WELCOME_PAGE;
        }
        return indexRedirect;
    }

}
