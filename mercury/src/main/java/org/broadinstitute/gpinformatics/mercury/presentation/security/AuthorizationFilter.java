package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.login.SecurityActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.login.UserLogin;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * AuthorizationFilter is a ServletFilter used to assist the Mercury application with validating whether a users
 * is authorized to have access to a certain page.  The filter is executed based on the url-pattern filter
 * defined in the web deployment descriptor.
 *
 * @author Scott Matthews
 */
public class AuthorizationFilter implements Filter {
    private static Log logger = LogFactory.getLog(AuthorizationFilter.class);

    private FilterConfig filterConfig;

    @Inject
    AuthorizationManager authManager;

    public static final String TARGET_PAGE_ATTRIBUTE = "targeted_page";

    /**
     * This the default initialization method for this filter.  It grabs the filter config (defined in the
     * web deployment descriptor).
     *
     * @param filterConfig Contains all values defined in the deployment descriptor
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    /**
     * This method contains the logic for authorizing a given page.
     * <p/>
     * The logic within this method will determine if
     * <ul>
     * <li>A user is currently logged in</li>
     * <li>If the page they are trying to access needs authorization</li>
     * <li>If the user is authorized to gain access to the page</li>
     * </ul>
     * <p/>
     * A failure on any of these items will result in the user being redirected to an appropriate page.
     *
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String pageUri = request.getServletPath();

        if (!excludeFromFilter(pageUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Checking authentication for: " + pageUri);
            }

            if (request.getRemoteUser() == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("User is not authenticated, redirecting to login page");
                }

                StringBuilder requestedUrl = new StringBuilder(request.getRequestURL());
                if (request.getQueryString() != null) {
                    requestedUrl.append("?").append(request.getQueryString());
                }
                request.getSession().setAttribute(TARGET_PAGE_ATTRIBUTE, requestedUrl.toString());
                redirectTo(request, servletResponse, SecurityActionBean.LOGIN_PAGE);
                return;
            }
        }

        // FIXME: With this code enabled, the URLs don't get updated in the browser after
        // the redirect.  Need to debug and then re-enable.  This is bug GPLIM-100.
        if (false && pageUri.equals(SecurityActionBean.LOGIN_PAGE) && request.getRemoteUser() != null) {
            // Already logged in user is trying to view the login page.  Redirect to the role default page.
            UserLogin.UserRole role = UserLogin.UserRole.fromRequest(request);
            redirectTo(request, servletResponse, role.landingPage);

            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * This is a helper method that redirects to a page instead of chaining to the next in the filter.
     *
     * @param request
     * @param response
     * @param errorPage
     * @throws IOException
     * @throws ServletException
     */
    private void redirectTo(ServletRequest request, ServletResponse response, String errorPage)
            throws IOException, ServletException {
        filterConfig.getServletContext().getRequestDispatcher(errorPage).forward(request, response);
    }

    /**
     * Pages to ignore from the authorization filter.
     *
     * @param path
     * @return
     */
    private static boolean excludeFromFilter(String path) {
        return path.startsWith("/rest") ||
                path.startsWith("/ArquillianServletRunner") ||
                path.startsWith(SecurityActionBean.LOGIN_ACTION) ||
                path.endsWith(SecurityActionBean.LOGIN_PAGE) ||
                path.endsWith("Mercury/");
    }

    @Override
    public void destroy() {
    }
}
