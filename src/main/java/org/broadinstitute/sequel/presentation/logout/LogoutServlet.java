package org.broadinstitute.sequel.presentation.logout;

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

//TODO:  Correct Base context root to be for any setting
@WebServlet(urlPatterns = {"/logout"})
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest requestIn, HttpServletResponse responseIn)
            throws ServletException, IOException {
        requestIn.getSession().invalidate();
        responseIn.sendRedirect("index.xhtml");
    }
}
