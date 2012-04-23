package org.broadinstitute.sequel.presentation.logout;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Scott Matthews
 *         Date: 4/20/12
 *         Time: 4:43 PM
 */

@WebServlet(urlPatterns = {"/logout"})
public class LogoutServlet extends HttpServlet {

    Logger logoutLogger = Logger.getLogger(this.getClass().getName());

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

    private void performLogout(HttpServletRequest requestIn, HttpServletResponse responseIn)
            throws ServletException, IOException {

        logoutLogger.log(Level.SEVERE, "contextPath is: " + requestIn.getContextPath());

        logoutLogger.log(Level.SEVERE,"Attempting to logout user");
        requestIn.logout();

        responseIn.sendRedirect(requestIn.getContextPath() + "index");
    }
}
