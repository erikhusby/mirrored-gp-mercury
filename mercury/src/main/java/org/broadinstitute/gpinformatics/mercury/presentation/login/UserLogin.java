package org.broadinstitute.gpinformatics.mercury.presentation.login;

/**
 * @author Scott Matthews
 *         Date: 4/2/12
 *         Time: 1:35 PM
 */

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.entity.DB;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationFilter;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationListener;

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
            UserRole role = UserRole.fromRequest(request);
            targetPage = role.landingPage;
            // HACK needed by Arquillian, see FIXME in UserBean.
            userBean.setBspUserList(bspUserList);
            userBean.login(request);

            if (!userBean.isValidBspUser()) {
                logger.error(userBean.getBspStatus() + ": " + username);
                addErrorMessage(userBean.getBspMessage(), null);
            }
            if (!userBean.isValidJiraUser()) {
                logger.error(userBean.getJiraStatus() + ": " + username);
                addErrorMessage(userBean.getJiraMessage(), null);
            }

            String previouslyTargetedPage = (String)request.getSession().getAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE);
            if (previouslyTargetedPage != null) {
                // Check for redirect to PM and PDMs landing page.
                previouslyTargetedPage = role.checkUrlForRoleRedirect(previouslyTargetedPage);

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
            addErrorMessage("The username and password you entered is incorrect.  Please try again.",
                    "Authentication error");
            targetPage = AuthorizationFilter.LOGIN_PAGE;
        }
        return redirect(targetPage);
    }

    public enum UserRole {
        // Order of roles is important, if user is both PDM and PM we want to go to PDM's page.
        PDM("/orders/list", DB.Role.PDM.name),
        PM("/projects/list", DB.Role.PM.name),
        OTHER("index", "");

        private static final String INDEX = AuthorizationListener.HOME_PAGE;
        private static final String MERCURY_PAGE = "/Mercury";

        public static UserRole fromRequest(HttpServletRequest request) {
            for (UserRole role : values()) {
                if (request.isUserInRole(role.roleName)) {
                    return role;
                }
            }
            return OTHER;
        }

        public final String landingPage;
        public final String roleName;

        private UserRole(String landingPage, String roleName) {
            this.landingPage = landingPage;
            this.roleName = roleName;
        }

        private String checkUrlForRoleRedirect(String targetPage) {
            StringBuilder newUrlBuilder = new StringBuilder(targetPage);
            if (this != OTHER) {
                if (targetPage.endsWith(MERCURY_PAGE) || targetPage.endsWith(MERCURY_PAGE + "/")) {
                    if (targetPage.endsWith("/")) {
                        newUrlBuilder.deleteCharAt(targetPage.lastIndexOf("/"));
                    }
                    newUrlBuilder.append(landingPage).append(".xhtml");
                } else if (targetPage.endsWith(INDEX) || targetPage.endsWith(INDEX + ".xhtml")) {
                    newUrlBuilder = new StringBuilder(targetPage.replace(INDEX, landingPage));
                }
            }
            return newUrlBuilder.toString();
        }
    }
}
