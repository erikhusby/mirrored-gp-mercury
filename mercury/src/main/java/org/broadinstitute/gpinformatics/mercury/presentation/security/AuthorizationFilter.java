package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.admin.PublicMessageActionBean;

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
        servletContext = filterConfig.getServletContext();
    }

    public static ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Check to see if a user is already authenticated. If not, we redirect to the login page preserving the
     * current page so we can navigate to it once the user has been authenticated.
     *
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
            log.debug("Checking authentication for: " + pageUri);

            if (request.getRemoteUser() == null) {
                log.debug("User is not authenticated, redirecting to login page");

                StringBuilder requestedUrl = new StringBuilder(request.getRequestURL());
                if (request.getQueryString() != null) {
                    requestedUrl.append("?").append(request.getQueryString());
                }
                request.getSession().setAttribute(TARGET_PAGE_ATTRIBUTE, requestedUrl.toString());
                servletContext.getRequestDispatcher(SecurityActionBean.LOGIN_PAGE).forward(request, servletResponse);
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
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
                path.endsWith("Mercury/") ||
                path.startsWith("/tableau/") ||
                path.startsWith(PublicMessageActionBean.URL_BINDING);
    }

    @Override
    public void destroy() {
    }
}
