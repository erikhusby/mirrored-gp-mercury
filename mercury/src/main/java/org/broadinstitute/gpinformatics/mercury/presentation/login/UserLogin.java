package org.broadinstitute.gpinformatics.mercury.presentation.login;

/**
 * @author Scott Matthews
 *         Date: 4/2/12
 *         Time: 1:35 PM
 */

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
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
import java.text.MessageFormat;

@Named
@RequestScoped
public class UserLogin extends AbstractJsfBean {

    public static final String PRODUCT_MANAGER_ROLE = "Mercury-ProductManagers";

    public static final String PROJECT_MANAGER_ROLE = "Mercury-ProjectManagers";

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

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private JiraService jiraService;

    @Inject
    private JiraConfig jiraConfig;

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

    private void warnIfBspUserInvalid() {
        if (userBean.getBspUser() == null) {
            // The user is not a valid JIRA User.  Warn, but allow login.
            String message;
            if (bspUserList.getUsers().isEmpty()) {
                // JIRA Server is unresponsive, can't log in to verify user.
                message = "Can't connect to JIRA server.";
            } else {
                message = MessageFormat.format("User ''{0}'' is not a valid BSP user, using server '{1}'.", username,
                        bspConfig.getHost());
            }
            logger.error(message);
            addFlashErrorMessage(message, message);
        }
    }

    private void warnIfJiraUserInvalid() {
        if (!jiraService.isUser(username)) {
            // The user is not a valid JIRA User.  Warn, but allow login.
            String message = MessageFormat.format("User ''{0}'' is not a valid JIRA user, using server '{1}'.",
                        username, jiraConfig.getHost());
            logger.error(message);
            addFlashErrorMessage(message, message);
        }
    }

    public String authenticateUser() {
        String targetPage;
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            HttpServletRequest request = (HttpServletRequest)context.getExternalContext().getRequest();

            request.login(username, password);
            UserRole role = UserRole.fromRequest(request);
            targetPage = role.landingPage;
            userBean.setBspUser(bspUserList.getByUsername(username));
            warnIfBspUserInvalid();
            warnIfJiraUserInvalid();
            addFlashMessage("Sign in successful. Welcome back!");

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
            addFlashErrorMessage("The username and password you entered is incorrect.  Please try again.",
                    "Authentication error");
            targetPage = AuthorizationFilter.LOGIN_PAGE;
        }
        return redirect(targetPage);
    }

    public enum UserRole {
        // Order of roles is important, if user is both PDM and PM we want to go to PDM's page.
        PDM("/orders/list", PRODUCT_MANAGER_ROLE),
        PM("/projects/list", PROJECT_MANAGER_ROLE),
        OTHER("", "");

        private static final String INDEX = "/index";
        private static final String HOME_PAGE = "/Mercury";

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
                if (targetPage.endsWith(HOME_PAGE) || targetPage.endsWith(HOME_PAGE + "/")) {
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
