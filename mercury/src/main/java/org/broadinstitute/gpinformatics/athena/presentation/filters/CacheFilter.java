package org.broadinstitute.gpinformatics.athena.presentation.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This filter allows caching of things for any configured timeout period. It uses a default of twenty-four hours if no
 * servlet filter configuration is supplied.  Typically any dynamic content should not be cached, but this is highly
 * effective for speeding up page load speeds containing more static content, like images, scripts and the like.
 */
@WebFilter
public class CacheFilter implements Filter {
    /**
     * Number of seconds for the content to expire.
     */
    private int expirationTime;

    /**
     * Should the filter set to cache or NOT cache the content?  Useful for local debugging of various issues if set to
     * not cache when one is changing around static content.
     */
    private boolean shouldCache;

    private static final String SHOULD_CACHE = "shouldCache";
    private static final String EXPIRATION_TIME = "expirationTime";

    /**
     * Default expiration time is twenty-four hours.
     */
    private static final int DEFAULT_EXPIRE = 24 * 60 * 60;

    /**
     * DateFormat wrapped in a ThreadLocal to make thread-safe.
     */
    private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            DateFormat localDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            localDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            return localDateFormat;
        }
    };

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        try {
            expirationTime = Integer.valueOf(filterConfig.getInitParameter(EXPIRATION_TIME));
        } catch (Exception e) {
            // No override timeout period specified, use application server settings.
            expirationTime = DEFAULT_EXPIRE;
        }

        try {
            shouldCache = Boolean.valueOf(filterConfig.getInitParameter(SHOULD_CACHE));
        } catch (Exception e) {
            // If not specified, the default is to cache.
            shouldCache = true;
        }
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }

    /**
     * Set the proper HTTP headers.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Set the provided HTTP response parameters.
        if (shouldCache) {
            setCacheExpireDate(httpResponse, expirationTime);
        } else {
            setNoCache(httpResponse);
        }

        chain.doFilter(request, response);
    }

    /**
     * Proper HTML header stuff for expiry junk.
     *
     * @param response          The servlet response
     * @param expirationSeconds The number of seconds before the response expires
     */
    private void setCacheExpireDate(HttpServletResponse response, int expirationSeconds) {
        if (response != null) {
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.SECOND, expirationSeconds);
            response.setHeader("Cache-Control", "public" + ", max-age=" + expirationSeconds
                                                + ", public");
            response.setHeader("Expires", DATE_FORMAT.get().format(cal.getTime()));
            response.setHeader("ExpiresDefault", "access plus " + expirationSeconds + " seconds");
        }
    }

    /**
     * Don't allow caching on the HTTP response.
     *
     * @param response The servlet response
     */
    private void setNoCache(HttpServletResponse response) {
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setDateHeader("Expires", -1);
    }

}