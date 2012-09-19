package org.broadinstitute.gpinformatics.athena.presentation.security;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;

/**
 * This filter is used to validate if a user is authorized to have access to a certain page.
 * The filter is executed based on the url-pattern filter defined in the web deployment descriptor
 */
public class AuthorizationFilter implements Filter {

    private final Logger LOG = Logger.getLogger(AuthorizationFilter.class);
    private FilterConfig filterConfig;

    public static final String LOGIN_PAGE = "/security/login.xhtml";
    private static final String PMB_LOGO = "/images/bridge.jpeg";

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

        HttpServletRequest request =(HttpServletRequest)servletRequestIn;
        String pageUri = request.getServletPath();

        LOG.info("Checking authentication for: " + pageUri);

        if (!excludeFromFilter(pageUri)) {
            Principal user = request.getUserPrincipal();
            if (user == null) {
                LOG.info("User is not authenticated, redirecting to login page");
                if (!pageUri.equals(LOGIN_PAGE)) {
                    servletRequestIn.setAttribute("targeted_page", pageUri);
                }
                errorRedirect(servletRequestIn,servletResponseIn, LOGIN_PAGE);
                return;
            }

            // User is now logged in, could also check for user role vs page authorization here.
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

    private boolean excludeFromFilter(String path) {
        return path.startsWith("/javax.faces.resource") ||
               path.startsWith("/rest") ||
               path.startsWith("/ArquillianServletRunner") ||
               path.equals(LOGIN_PAGE) ||
               path.equals(PMB_LOGO);
    }


    @Override
    public void destroy() {
    }
}
