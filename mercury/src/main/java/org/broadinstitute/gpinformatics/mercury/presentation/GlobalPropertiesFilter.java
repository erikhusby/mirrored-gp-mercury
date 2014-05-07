package org.broadinstitute.gpinformatics.mercury.presentation;

import org.broadinstitute.gpinformatics.athena.boundary.BuildInfoBean;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * This filter installs properties that all JSP pages can access.
 */
public class GlobalPropertiesFilter implements Filter {

    @Inject
    private BuildInfoBean buildInfoBean;

    @Inject
    private UserBean userBean;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // This supports using CDI objects in JSP.
        servletRequest.setAttribute("buildInfoBean", buildInfoBean);
        servletRequest.setAttribute("userBean", userBean);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
