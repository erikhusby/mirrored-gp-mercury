package org.broadinstitute.sequel.presentation.security;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

/**
 *
 * AuthorizationFilter is a ServletFilter used to assist the SequeL application with validating whether a users
 * is authorized to have access to a certain page.  The filter is executed based on the url-pattern filter
 * defined in the web deployment descriptor
 *
 *
 * @author Scott Matthews
 *         Date: 5/2/12
 *         Time: 11:57 AM
 */
public class AuthorizationFilter implements Filter {

    private FilterConfig filterConfig;

    private String errorPage;
    @Inject AuthorizationManager manager;

    private static final String LOGIN_PAGE = "/login/login.xhtml";

    /**
     * init is the default initialization method for this filter.  It grabs the filter config (defined in the
     * web deployment descriptor) as well as the error page if authorization fails
     * @param filterConfigIn Contains all values defined in the deployment descriptor
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfigIn) throws ServletException {
        filterConfig = filterConfigIn;
        if (filterConfigIn != null) {
            errorPage = filterConfigIn.getInitParameter("error_page");
        }

    }

    /**
     *
     * doFilter Defines the logic for Authorizing a given page.
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

        debug("Checking authentication for: " + pageUri);

        if(manager.isPageProtected(pageUri, request) && !excludeFromFilter(pageUri)) {

            Principal user = request.getUserPrincipal();
            if(null == user ) {
                debug("User is not authenticated, redirecting to login page");
                errorRedirect(servletRequestIn,servletResponseIn,LOGIN_PAGE);
                return;
            }

            boolean authorized = manager.isUserAuthorized(pageUri, request);

            if(authorized) {
                debug("User is authorized for this resource, continuing on");
                continueProcessing(servletRequestIn, servletResponseIn, filterChainIn);
                return;
            } else  {
                debug("User is not authorized for this resource, redirecting to authorization error page");
                errorRedirect(servletRequestIn,servletResponseIn, errorPage);
                return;
            }
        } else{
            debug("Current page: "+pageUri+" is not protected or excluded from filter.  Continuing on");
            continueProcessing(servletRequestIn, servletResponseIn, filterChainIn);
            return;
        }
    }

    /**
     *
     * continueProcessing is a simple helper method who's intention is to encapsulate the execution of the call
     * to the doFilter method of the filter chain.
     *
     * It is intended to be called upon success.
     *
     * @param servletRequestIn
     * @param servletResponseIn
     * @param filterChainIn
     * @throws IOException
     * @throws ServletException
     */
    private void continueProcessing(ServletRequest servletRequestIn,
                             ServletResponse servletResponseIn,
                             FilterChain filterChainIn)
            throws IOException, ServletException {
        filterChainIn.doFilter(servletRequestIn, servletResponseIn);
    }


    /**
     *
     * errorRedirect is a simple helper method who's intention is to encapsulate the execution of redirecting
     * the page upon a failure in the filter logic.
     *
     * @param requestIn
     * @param responseIn
     * @param errorPageIn
     * @throws IOException
     * @throws ServletException
     */
    private void errorRedirect(ServletRequest requestIn, ServletResponse responseIn,
                               String errorPageIn) throws IOException, ServletException{
        filterConfig.getServletContext().getRequestDispatcher(errorPageIn).forward(requestIn,responseIn);
    }

    private boolean excludeFromFilter(String path) {
        if(path.startsWith("/javax.faces.resource")){
            return true;
        } else {
            return false;
        }
    }

    private void debug(String debugStmtIn) {
        System.out.println(debugStmtIn);
    }


    @Override
    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}
