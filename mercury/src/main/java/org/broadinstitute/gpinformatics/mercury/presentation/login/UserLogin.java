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
            addInfoMessage("Welcome to Mercury!", "Sign in successful");

            String previouslyTargetedPage = (String)request.getSession().getAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE);
            if (previouslyTargetedPage != null) {
                // Check for redirect to PM and PDMs landing page
                previouslyTargetedPage = checkUrlForRoleRedirect(previouslyTargetedPage, request);

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

    private String checkUrlForRoleRedirect(String targetPage, final HttpServletRequest request) {
        final String INDEX = "/index";
        final String HOME_PAGE = "/Mercury";
        StringBuilder newUrlBuilder = new StringBuilder(targetPage);

        final boolean hasPMRole = request.isUserInRole( RolePage.PM.getRoleName() );
        final boolean hasPDMRole = request.isUserInRole( RolePage.PDM.getRoleName());

        if (targetPage.endsWith(HOME_PAGE) || targetPage.endsWith(HOME_PAGE + "/")) {
            if ( targetPage.endsWith("/") ) {
                newUrlBuilder.deleteCharAt( targetPage.lastIndexOf("/") );
            }
            if ( hasPMRole ) {
                newUrlBuilder.append(RolePage.PM.getLandingPage() + ".xhtml");
            }
            if ( hasPDMRole ) {
                newUrlBuilder.append(RolePage.PDM.getLandingPage() + ".xhtml" );
            }
        } else if ( targetPage.endsWith(INDEX)  || targetPage.endsWith(INDEX + ".xhtml") ) {
            if ( hasPMRole ) {
                newUrlBuilder = new StringBuilder(targetPage.replace(INDEX, RolePage.PM.getLandingPage()));
            }
            if ( hasPDMRole ) {
                newUrlBuilder = new StringBuilder(targetPage.replace(INDEX, RolePage.PDM.getLandingPage()));
            }
        }
        return newUrlBuilder.toString();
    }

    private String checkTargetForRoleRedirect(final HttpServletRequest request) {
        String newTarget = "/index";
        final boolean hasPMRole = request.isUserInRole( RolePage.PM.getRoleName() );
        final boolean hasPDMRole = request.isUserInRole( RolePage.PDM.getRoleName());

        if ( hasPMRole ) {
            newTarget =   RolePage.PM.getLandingPage();
        }
        if ( hasPDMRole ) {
            newTarget =   RolePage.PDM.getLandingPage();
        }
        return newTarget;
    }

    private enum RolePage {
        PDM ("/orders/list", "Mercury-ProductManagers"),
        PM ("/projects/list", "Mercury-ProjectManagers");

        private String landingPage;
        private String roleName;

        private RolePage(final String landingPage, final String roleName) {
            this.landingPage = landingPage;
            this.roleName = roleName;
        }

        public String getLandingPage() {
            return landingPage;
        }

        public String getRoleName() {
            return roleName;
        }
    }

}
