package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

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
 * SecurityFilter is a Servlet filter whose intentions are to assist the Mercury application with the job of forcing
 * all unsecured traffic to the applications secure (ssl) port.
 */
public class SecurityFilter implements Filter {

    private boolean isSecure;
    private int securePort;

    private static final Log log = LogFactory.getLog(AuthorizationFilter.class);


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        isSecure = Deployment.isCRSP;
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

        String currentURI = httpReq.getRequestURI();

        if (isSecure && (!httpReq.isSecure())) {
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

    @Override
    public void destroy() {

    }
}
