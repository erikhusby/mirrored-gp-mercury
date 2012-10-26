package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.commons.logging.Log;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * AuthorizationFilter is a ServletFilter used to assist the Mercury application with validating whether a users
 * is authorized to have access to a certain page.  The filter is executed based on the url-pattern filter
 * defined in the web deployment descriptor
 *
 *
 * @author Scott Matthews
 *         Date: 5/2/12
 *         Time: 11:57 AM
 */
public class AuthorizationFilter implements Filter {

    @Inject
    private Log logger;
    private FilterConfig filterConfig;

    @Inject AuthorizationManager manager;

    public static final String LOGIN_PAGE = "/security/login.xhtml";
    public static final String TARGET_PAGE_ATTRIBUTE = "targeted_page";

    /**
     * This the default initialization method for this filter.  It grabs the filter config (defined in the
     * web deployment descriptor).
     * @param filterConfigIn Contains all values defined in the deployment descriptor
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfigIn) throws ServletException {
        this.filterConfig = filterConfigIn;
    }

    /**
     * This method contains the logic for authorizing a given page.
     *
     * The logic within this method will determine if
     * <ul>
     *     <li>A user is currently logged in</li>
     *     <li>If the page they are trying to access needs authorization</li>
     *     <li>If the user is authorized to gain access to the page</li>
     * </ul>
     *
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

        HttpServletRequest request = (HttpServletRequest)servletRequest;
        String pageUri = request.getServletPath();

        if (!excludeFromFilter(pageUri)) {
            logger.debug("Checking authentication for: " + pageUri);
            String user = request.getRemoteUser();
            if (user == null) {
                logger.debug("User is not authenticated, redirecting to login page");
                if (!pageUri.equals(LOGIN_PAGE)) {
                    StringBuilder requestedUrl = new StringBuilder(request.getRequestURL());
                    if (request.getQueryString() != null) {
                        requestedUrl.append("?").append(request.getQueryString());
                    }
                    request.getSession().setAttribute(TARGET_PAGE_ATTRIBUTE, requestedUrl.toString());
                }
                errorRedirect(servletRequest, servletResponse, LOGIN_PAGE);
                return;
            }
            boolean authorized = manager.isUserAuthorized(pageUri, request);

            if (!authorized) {
                String errorMessage = "The user '" + user +  "' doesn't have permission to log in.";
                logger.warn(errorMessage);
                errorRedirect(servletRequest, servletResponse, LOGIN_PAGE);
                return;
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * errorRedirect is a helper method that redirects to a page upon failure in the filter
     *
     * @param request
     * @param response
     * @param errorPage
     * @throws IOException
     * @throws ServletException
     */
    private void errorRedirect(ServletRequest request, ServletResponse response, String errorPage)
            throws IOException, ServletException {
        filterConfig.getServletContext().getRequestDispatcher(errorPage).forward(request, response);
    }

    private static boolean excludeFromFilter(String path) {
        return path.startsWith("/javax.faces.resource") ||
               path.startsWith("/rest") ||
               path.startsWith("/ArquillianServletRunner") ||
               path.startsWith(LOGIN_PAGE);
    }


    @Override
    public void destroy() {
    }
}
