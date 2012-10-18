package org.broadinstitute.gpinformatics.mercury.presentation.login;

/**
 * @author Scott Matthews
 *         Date: 4/2/12
 *         Time: 1:35 PM
 */

import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationFilter;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

//@javax.faces.bean.ManagedBean
//@javax.faces.bean.RequestScoped
@Named
@RequestScoped
public class UserLogin extends AbstractJsfBean {
    private String username;

    private String password;

    @Inject
    private Log logger;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String authenticateUser() {
        String targetPage = "/index";
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            HttpServletRequest request = (HttpServletRequest)context.getExternalContext().getRequest();

            request.login(username, password);
            BspUser bspUser = bspUserList.getByUsername(username);
            if (bspUser != null) {
                userBean.setBspUser(bspUser);
            } else {
                // FIXME: Temporarily map unknown users to the BSP tester user. Will need to handle this in userBean.
                userBean.setBspUser(bspUserList.getByUsername("tester"));
                // reuse exception handling below
                //throw new ServletException("Login error: couldn't find BspUser: " + username);
            }
            addInfoMessage("Welcome back!", "Sign in successful");

            String previouslyTargetedPage = (String)request.getAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE);

            if (previouslyTargetedPage != null) {
                targetPage = previouslyTargetedPage;
            }
        } catch (ServletException le) {
            logger.error("ServletException Retrieved: ", le);
            addErrorMessage("The username and password you entered is incorrect.  Please try again.", "Authentication error");
            targetPage = AuthorizationFilter.LOGIN_PAGE;
        }

        return targetPage;
    }
}
