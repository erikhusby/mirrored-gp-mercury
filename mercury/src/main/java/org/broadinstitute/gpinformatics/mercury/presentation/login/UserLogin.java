package org.broadinstitute.gpinformatics.mercury.presentation.login;

/**
 * @author Scott Matthews
 *         Date: 4/2/12
 *         Time: 1:35 PM
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@RequestScoped
public class UserLogin extends AbstractJsfBean {
    private String username;
    private String password;

    private Log loginLogger = LogFactory.getLog(UserLogin.class);


    public String getUserName() {
        return username;
    }

    public void setUserName(String usernameIn) {
        username = usernameIn;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String passwordIn) {
        password = passwordIn;
    }

    public String authenticateUser() {
        String targetPage = "/index";
        FacesContext context = FacesContext.getCurrentInstance();

        try {
            authenticate();
            context.addMessage(null,new FacesMessage(FacesMessage.SEVERITY_INFO, "Welcome back!", "Sign in successful"));

            HttpServletRequest request = (HttpServletRequest)context.getExternalContext().getRequest();
            String previouslyTargetedPage = (String)request.getAttribute("targetted_page");

            if(null != previouslyTargetedPage ) {
                targetPage = previouslyTargetedPage;
            }
        } catch (LoginException le) {
            loginLogger.error("LoginException Retrieved: ",le);
            context.addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR, "The username and password you entered is incorrect.  Please try again.", "Authentication error"));
            targetPage = "/security/login";
        } catch (ServletException le) {
            loginLogger.error("ServletException Retrieved: ",le);
            context.addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR, "The username and password you entered is incorrect.  Please try again.", "Authentication error"));
            targetPage = "/security/login";
        }

        return targetPage;
    }

    private void authenticate() throws LoginException, ServletException {
        HttpServletRequest request = (HttpServletRequest )FacesContext.getCurrentInstance().getExternalContext().getRequest();

        request.login(username, password);
    }
}
