package org.broadinstitute.gpinformatics.mercury.presentation.login;

import net.sourceforge.stripes.action.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.mercury.entity.DB;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationFilter;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationListener;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * This class is for managing security.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@UrlBinding("/security/security.action")
public class SecurityActionBean extends CoreActionBean {
    private Log logger = LogFactory.getLog(SecurityActionBean.class);

    private String username;

    private String password;

    @Inject
    private UserBean userBean;

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

    /**
     * Sign the user in and figure out where they should go.
     *
     * @return
     */
    @DefaultHandler
    public Resolution welcome() {
        return new ForwardResolution(UserRole.INDEX);
    }

    /**
     * Sign the user in and figure out where they should go.
     *
     * @return
     */
    public Resolution signIn() {
        String targetPage;
        HttpServletRequest request = (HttpServletRequest) getContext().getRequest();

        if (request.getUserPrincipal() != null && username.equalsIgnoreCase(request.getUserPrincipal().getName())) {
            // User is already logged in, don't try to login again.
            return new RedirectResolution(UserRole.fromRequest(request).landingPage);
        }

        try {
            request.login(username, password);
            UserRole role = UserRole.fromRequest(request);
            targetPage = role.landingPage;
            userBean.login(request);

            if (!userBean.isValidBspUser()) {
                logger.error(userBean.getBspStatus() + ": " + username);

                addGlobalValidationError(userBean.getBspMessage());
            }
            if (!userBean.isValidJiraUser()) {
                logger.error(userBean.getJiraStatus() + ": " + username);
                addGlobalValidationError(userBean.getJiraMessage());
            }

            String previouslyTargetedPage = (String) request.getSession().getAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE);
            if (previouslyTargetedPage != null) {
                // Check for redirect to PM and PDMs landing page.
                previouslyTargetedPage = role.checkUrlForRoleRedirect(previouslyTargetedPage);

                request.getSession().setAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE, null);
                new RedirectResolution(previouslyTargetedPage);
            }
        } catch (ServletException le) {
            logger.error("ServletException Retrieved: ", le);
            addGlobalValidationError("The username and password you entered is incorrect.  Please try again.");
            targetPage = AuthorizationFilter.LOGIN_PAGE;
        }

        return new ForwardResolution(targetPage);
    }

    public Resolution signOut() {
        getContext().invalidateSession();
        return new RedirectResolution(UserRole.APP_CONTEXT);
    }

    public enum UserRole {
        // Order of roles is important, if user is both PDM and PM we want to go to PDM's page.
        PDM("/orders/list", DB.Role.PDM.name),
        PM(ResearchProjectActionBean.PROJECT_LIST_PAGE, DB.Role.PM.name),
        OTHER("/index.jsp", "");

        private static final String INDEX = AuthorizationListener.HOME_PAGE;
        private static final String APP_CONTEXT = "/Mercury"; // getContext().getRequest().getContextPath();

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
                if (targetPage.endsWith(APP_CONTEXT) || targetPage.endsWith(APP_CONTEXT + "/")) {
                    if (targetPage.endsWith("/")) {
                        newUrlBuilder.deleteCharAt(targetPage.lastIndexOf("/"));
                    }
                    newUrlBuilder.append(landingPage).append(".jsp");
                } else if (targetPage.endsWith(INDEX) || targetPage.endsWith(INDEX + ".jsp")) {
                    newUrlBuilder = new StringBuilder(targetPage.replace(INDEX, landingPage));
                }
            }
            return newUrlBuilder.toString();
        }
    }
}
