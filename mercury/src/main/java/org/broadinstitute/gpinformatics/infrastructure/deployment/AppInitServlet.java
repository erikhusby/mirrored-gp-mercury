package org.broadinstitute.gpinformatics.infrastructure.deployment;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

/**
 * Makes objects related to web container available to other classes.
 * (moved from CacheFilter because WildFly undertow web container does not initialize servlet filters)
 */
@WebServlet(loadOnStartup = 1)
public class AppInitServlet extends HttpServlet {

    private static ServletContext servletcontext = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servletcontext = config.getServletContext();
    }

    public static ServletContext getInitServletContext(){
        return servletcontext;
    }
}
