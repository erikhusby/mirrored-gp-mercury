package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter forces all unsecured traffic to the application's secure (ssl) port.
 */
public class SecurityFilter implements Filter {

    private int securePort;

    private static final Log log = LogFactory.getLog(AuthorizationFilter.class);


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        try {
            securePort = Integer.parseInt(filterConfig.getInitParameter("securePort"));
        } catch (NumberFormatException | NullPointerException e) {
            log.error("Could not use secure port defined in web.xml.  Using default value");
            securePort = 443;
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) servletRequest;

        String servletPath = httpReq.getServletPath();
        if (excludeFromFilter(servletPath)) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            String currentURI = httpReq.getRequestURI();

            if (!httpReq.isSecure()) {
                // if required to be secure, then redirect user to proper location
                log.warn(httpReq.getRemoteAddr() + " trying to access " + currentURI + " insecurely.");

                // force user to secure login page (redirect)
                String newURL = "https://" + httpReq.getServerName() + ":" + securePort
                                + //httpReq.getContextPath() + "/" +
                                currentURI;
                ((HttpServletResponse) servletResponse).sendRedirect(newURL);
            } else {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        }
    }

    /**
     * Determine whether to exclude a path from the filter.  Web Service calls from liquid handling decks are
     * currently excluded, so the decks are not forced to implement SSL.
     * @param path url after server
     * @return true if excluded
     */
    private static boolean excludeFromFilter(String path) {
        return path.startsWith("/rest/limsQuery") ||
                path.startsWith("/rest/bettalimsmessage") ||
                path.startsWith("/rest/vessel/registerTubes") ||
                path.startsWith("/rest/IlluminaRun/query") ||
                path.startsWith("/rest/solexarun");
    }

    @Override
    public void destroy() {

    }
}
