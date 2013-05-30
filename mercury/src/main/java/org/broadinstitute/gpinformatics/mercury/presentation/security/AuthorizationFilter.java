package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * AuthorizationFilter is a ServletFilter used to assist the Mercury application with validating whether a users
 * is authorized to have access to a certain page.  The filter is executed based on the url-pattern filter
 * defined in the web deployment descriptor.
 *
 * @author Scott Matthews
 */
public class AuthorizationFilter implements Filter {
    private static final Log log = LogFactory.getLog(AuthorizationFilter.class);

    private FilterConfig filterConfig;

    private static ServletContext servletContext;

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
        servletContext = filterConfig.getServletContext();
    }

    public static ServletContext getServletContext() {
        return servletContext;
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
            if (log.isDebugEnabled()) {
                log.debug("Checking authentication for: " + pageUri);
            }

            if (request.getRemoteUser() == null) {
                if (log.isDebugEnabled()) {
                    log.debug("User is not authenticated, redirecting to login page");
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
