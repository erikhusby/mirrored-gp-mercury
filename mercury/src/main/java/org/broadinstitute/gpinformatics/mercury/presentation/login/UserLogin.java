package org.broadinstitute.gpinformatics.mercury.presentation.login;

/**
 * @author Scott Matthews
 *         Date: 4/2/12
 *         Time: 1:35 PM
 */

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationFilter;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@RequestScoped
public class UserLogin {
    private String username;
    private String password;

    @Inject
    private Log logger;

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
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Welcome back!", "Sign in successful"));

            String previouslyTargetedPage = (String)request.getAttribute(AuthorizationFilter.TARGET_PAGE_ATTRIBUTE);

            if (previouslyTargetedPage != null) {
                targetPage = previouslyTargetedPage;
            }
        } catch (ServletException le) {
            logger.error("ServletException Retrieved: ", le);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "The username and password you entered is incorrect.  Please try again.", "Authentication error"));
            targetPage = AuthorizationFilter.LOGIN_PAGE;
        }

        return targetPage;
    }
}
