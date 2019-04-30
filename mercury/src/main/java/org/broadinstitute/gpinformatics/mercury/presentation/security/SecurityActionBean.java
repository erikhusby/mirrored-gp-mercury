package org.broadinstitute.gpinformatics.mercury.presentation.security;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is for managing security.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@UrlBinding("/security/security.action")
public class SecurityActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(SecurityActionBean.class);

    public static final String LOGIN_ACTION = "/security/security.action";

    public static final String HOME_PAGE = "/index.jsp";

    public static final String LOGIN_PAGE = "/security/login.jsp";

    private static final Set<String> STRIPES_IGNORE_PARAMS = new HashSet<String>() {{
        add("__fp");
        add("__fsk");
    }};

    @Validate(required = true, on = {"signIn"})
    private String username;

    @Validate(required = true, on = {"signIn"})
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
     * @return login page on error, or user's start page
     */
    @DefaultHandler
    public Resolution welcome() {
        HttpServletRequest request = getContext().getRequest();
        if (request.getUserPrincipal() == null || request.getUserPrincipal().getName() == null) {
            // User not logged in

            // If AJAX session timeout, do not forward to targeted page after sign in.
            if( request.getParameter("ajax") != null ) {
                request.getSession().removeAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE);
            }

            return new RedirectResolution(LOGIN_PAGE);
        }

        return new ForwardResolution(HOME_PAGE);
    }

    /**
     * Sign the user in and figure out where they should go.
     *
     * @return
     */
    public Resolution signIn() {
        String targetPage;
        HttpServletRequest request = getContext().getRequest();

        if (request.getUserPrincipal() != null && username.equalsIgnoreCase(request.getUserPrincipal().getName())) {
            // User is already logged in, don't try to login again.
            return new RedirectResolution(UserRole.fromUserBean(userBean).landingPage);
        }

        try {
            request.login(username, password);
            userBean.login(request);
            UserRole role = UserRole.fromUserBean(userBean);
            targetPage = role.landingPage;

            if (!userBean.isValidBspUser() && !userBean.isViewer()) {
                logger.error(userBean.getBspStatus() + ": " + username);

                addGlobalValidationError(userBean.getBspMessage());
            }
            if (!userBean.isValidJiraUser() && !userBean.isViewer()) {
                logger.error(userBean.getJiraStatus() + ": " + username);
                addGlobalValidationError(userBean.getJiraMessage());
            }

            String previouslyTargetedPage = (String) request.getSession().getAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE);
            if (previouslyTargetedPage != null) {
                // Check for redirect to PM and PDMs landing page.
                previouslyTargetedPage = role.checkUrlForRoleRedirect(previouslyTargetedPage);

                request.getSession().setAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE, null);
                Map<String, String[]> parameters = (Map<String, String[]>) request.getSession().getAttribute(
                        AuthorizationFilter.TARGET_PARAMETERS);

                RedirectResolution redirectResolution = new RedirectResolution(previouslyTargetedPage, false);
                for (Map.Entry<String, String[]> mapEntry : parameters.entrySet()) {
                    if (STRIPES_IGNORE_PARAMS.contains(mapEntry.getKey())) {
                        continue;
                    }
                    redirectResolution.addParameter(mapEntry.getKey(), mapEntry.getValue());
                }
                return redirectResolution;
            }
        } catch (ServletException le) {
            logger.error("ServletException Retrieved: ", le);
            addGlobalValidationError("The username and password you entered is incorrect.  Please try again.");
            targetPage = LOGIN_PAGE;
        }

        return new ForwardResolution(targetPage);
    }

    /**
     * Logout and invalidate the HTTP session.
     */
    public Resolution signOut() {
        try {
            getContext().invalidateSession();
            getContext().getRequest().logout();
        } catch (ServletException se) {
            logger.error("Problem logging out", se);
        }

        return new RedirectResolution("/");
    }

    public enum UserRole {
        // Order of roles is important, if user is both PDM and PM we want to go to PDM's page.
        PDM("/orders/order.action?list", Role.PDM),
        PM(ResearchProjectActionBean.PROJECT_LIST_PAGE, Role.PM),
        GPPM(ResearchProjectActionBean.PROJECT_LIST_PAGE, Role.GPProjectManager),
        OTHER("/index.jsp", null);

        private static final String APP_CONTEXT = "/Mercury"; // getContext().getRequest().getContextPath();

        public static UserRole fromUserBean(UserBean userBean) {
            for (UserRole userRole : values()) {
                if (userBean.getRoles().contains(userRole.role)) {
                    return userRole;
                }
            }
            return OTHER;
        }

        public final String landingPage;
        public final Role role;

        private UserRole(String landingPage, Role role) {
            this.landingPage = landingPage;
            this.role = role;
        }

        private String checkUrlForRoleRedirect(String targetPage) {
            StringBuilder newUrlBuilder = new StringBuilder(targetPage);
            if (this != OTHER) {
                if (targetPage.endsWith(APP_CONTEXT) || targetPage.endsWith(APP_CONTEXT + "/")) {
                    if (targetPage.endsWith("/")) {
                        newUrlBuilder.deleteCharAt(targetPage.lastIndexOf("/"));
                    }
                    newUrlBuilder.append(landingPage).append(".jsp");
                } else if (targetPage.endsWith(HOME_PAGE)) {
                    newUrlBuilder = new StringBuilder(targetPage.replace(HOME_PAGE, landingPage));
                }
            }
            return newUrlBuilder.toString();
        }
    }
}
