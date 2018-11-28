package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.PublicMessageActionBean;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
    public static final String TARGET_PARAMETERS = "targeted_params";

    /**
     * This the default initialization method for this filter.  It grabs the filter config (defined in the
     * web deployment descriptor).
     *
     * @param filterConfig Contains all values defined in the deployment descriptor
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
     */
    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String pageUri = request.getServletPath();

        // External web services require BASIC authentication.
        if (request.getRequestURI().startsWith(request.getContextPath() + "/rest/external")) {
            basicAuth(servletRequest, servletResponse, filterChain);
            return;
        }
        if (!excludeFromFilter(pageUri)) {
            // Everything else is FORM authentication
            log.debug("Checking authentication for: " + pageUri);

            if (request.getRemoteUser() == null) {
                log.debug("User is not authenticated, redirecting to login page");

                StringBuilder requestedUrl = new StringBuilder(request.getRequestURL());
                if (request.getQueryString() != null) {
                    requestedUrl.append("?").append(request.getQueryString());
                }
                request.getSession().setAttribute(TARGET_PAGE_ATTRIBUTE, requestedUrl.toString());

                Map<String, String[]> parameterMap = new HashMap<>();
                parameterMap.putAll(request.getParameterMap());
                request.getSession().setAttribute(TARGET_PARAMETERS, parameterMap);
                if (ServletFileUpload.isMultipartContent(request)) {
                    ServletFileUpload sfu = new ServletFileUpload();
                    sfu.setFileItemFactory(new DiskFileItemFactory());
                    try {
                        List<FileItem> fileItems = sfu.parseRequest(request);

                        for (FileItem item : fileItems) {
                            if (item.isFormField()) {
                                parameterMap.put(item.getFieldName(), new String[]{item.getString()});
                            }
                        }
                    } catch (FileUploadException e) {
                        log.error(e.getMessage(), e);
                    }
                }

                servletContext.getRequestDispatcher(SecurityActionBean.LOGIN_PAGE).forward(request, servletResponse);
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Decode request headers that include username and password.
     */
    private void basicAuth(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();

                if (basic.equalsIgnoreCase("Basic")) {
                    try {
                        String credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");
                        int p = credentials.indexOf(':');
                        if (p == -1) {
                            unauthorized(response, "Invalid authentication token");
                        } else {
                            String username = credentials.substring(0, p).trim();
                            String password = credentials.substring(p + 1).trim();
                            if (request.getRemoteUser() == null || !request.getRemoteUser().equals(username)) {
                                request.login(username, password);
                            }
                            filterChain.doFilter(servletRequest, servletResponse);
                        }
                    } catch (ServletException ignored) {
                        unauthorized(response, "Bad credentials");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Couldn't decode authentication", e);
                    }
                }
            }
        } else {
            unauthorized(response, "Unauthorized");
        }
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + "REST" + "\"");
        response.sendError(401, message);
    }

    /**
     * Pages to ignore from the authorization filter.
     *
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
