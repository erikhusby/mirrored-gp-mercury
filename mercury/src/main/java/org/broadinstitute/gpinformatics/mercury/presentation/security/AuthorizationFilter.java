package org.broadinstitute.gpinformatics.mercury.presentation.security;

import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import com.atlassian.crowd.service.soap.client.SecurityServerClientFactory;
import org.apache.log4j.Logger;
import org.primefaces.util.ArrayUtils;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This filter is used to validate if a user is authorized to have access to a certain page.
 * The filter is executed based on the url-pattern filter defined in the web deployment descriptor
 *
 *
 * @author Scott Matthews
 *         Date: 5/2/12
 *         Time: 11:57 AM
 */
public class AuthorizationFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(AuthorizationFilter.class);
    public static final String TARGET_PAGE_ATTRIBUTE = "targeted_page";
    private FilterConfig filterConfig;

    public static final String LOGIN_PAGE = "/security/login.xhtml";

    // All assets required by the Login page, minus JSF support files.
    private static final String[] LOGIN_ASSETS = { LOGIN_PAGE, "/images/broad_logo.png", "/images/bridge.jpeg"};

    private static final String CROWD_TOKEN_KEY_COOKIE_NAME = "crowd.token_key";

    /**
     * init is the default initialization method for this filter.  It grabs the filter config (defined in the
     * web deployment descriptor) as well as the error page if authorization fails
     * @param filterConfigIn Contains all values defined in the deployment descriptor
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfigIn) throws ServletException {
        filterConfig = filterConfigIn;
    }

    /**
     * Gets the crowd token key from the cookie.
     *
     * @param request
     *                the request.
     *
     * @return the crowd token key.
     */
    private static String getCrowdTokenKey(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(CROWD_TOKEN_KEY_COOKIE_NAME)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
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
     * @param servletRequestIn
     * @param servletResponseIn
     * @param filterChainIn
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequestIn,
                         ServletResponse servletResponseIn,
                         FilterChain filterChainIn)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest)servletRequestIn;
        String pageUri = request.getServletPath();

        if (!excludeFromFilter(pageUri)) {
            LOG.info("Checking authentication for: " + pageUri);
            String user = request.getRemoteUser();
            if (user == null) {
                LOG.info("User is not authenticated, checking for SSO token");
                String token = getCrowdTokenKey(request);
                if (token != null) {
                    try {
                        SOAPPrincipal principal =
                                SecurityServerClientFactory.getSecurityServerClient().findPrincipalByToken(token);
                        if (principal != null) {
                            // ???
                            LOG.info("Found principal " + principal.getName());
                        }
                    } catch (Exception e) {
                        LOG.error("Error while validating SSO token", e);

                    }
                }

                LOG.info("User is not authenticated, redirecting to login page");
                if (!pageUri.equals(LOGIN_PAGE)) {
                    servletRequestIn.setAttribute(TARGET_PAGE_ATTRIBUTE, pageUri);
                }
                errorRedirect(servletRequestIn, servletResponseIn, LOGIN_PAGE);
                return;
            }

            // User is now logged in, check for user role vs page authorization.
            if (!request.isUserInRole("PMBAdmins")
                && !request.isUserInRole("PMBUsers")
                && !request.isUserInRole("PMBViewers")) {
                LOG.info("User isn't in PMBridge groups");
                // FIXME: need to report page access error back to user somehow.
                String errorMessage = "The user '" + user +  "' doesn't have permission to log into PMBridge.";
                errorRedirect(servletRequestIn, servletResponseIn, LOGIN_PAGE);
            }
        }
        filterChainIn.doFilter(servletRequestIn, servletResponseIn);
    }

    /**
     *
     * errorRedirect is a helper method that redirects to a page upon failure in the filter
     *
     * @param requestIn
     * @param responseIn
     * @param errorPageIn
     * @throws IOException
     * @throws ServletException
     */
    private void errorRedirect(ServletRequest requestIn, ServletResponse responseIn,
                               String errorPageIn) throws IOException, ServletException{
        filterConfig.getServletContext().getRequestDispatcher(errorPageIn).forward(requestIn, responseIn);
    }

    private static boolean excludeFromFilter(String path) {
        return path.startsWith("/javax.faces.resource") ||
               path.startsWith("/rest") ||
               path.startsWith("/ArquillianServletRunner") ||
               ArrayUtils.contains(LOGIN_ASSETS, path);
    }


    @Override
    public void destroy() {
    }
}
