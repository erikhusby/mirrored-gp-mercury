package org.broadinstitute.gpinformatics.mercury.presentation.logout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Scott Matthews
 *         Date: 4/20/12
 *         Time: 4:43 PM
 */

@WebServlet(urlPatterns = {"/logout"})
public class LogoutServlet extends HttpServlet {

    private static final Log logoutLogger = LogFactory.getLog(LogoutServlet.class);

    @Override
    protected void doGet(HttpServletRequest requestIn, HttpServletResponse responseIn)
            throws ServletException, IOException {
        performLogout(requestIn, responseIn);
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequestIn, HttpServletResponse httpServletResponseIn)
            throws ServletException, IOException {
        performLogout(httpServletRequestIn, httpServletResponseIn);
    }

    private static void performLogout(HttpServletRequest requestIn, HttpServletResponse responseIn)
            throws ServletException, IOException {
        logoutLogger.info("contextPath is: " + requestIn.getContextPath());

        logoutLogger.info("Attempting to logout user");
        requestIn.logout();

        responseIn.sendRedirect(requestIn.getContextPath() + "index");
    }
}
