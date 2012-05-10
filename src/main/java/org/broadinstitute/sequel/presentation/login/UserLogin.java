package org.broadinstitute.sequel.presentation.login;

/**
 * @author Scott Matthews
 *         Date: 4/2/12
 *         Time: 1:35 PM
 */

//import com.atlassian.crowd.application.jaas.CrowdLoginModule;

import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@ManagedBean
@RequestScoped
public class UserLogin extends AbstractJsfBean {

    private String userName;
    private String password;


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userNameIn) {
        userName = userNameIn;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String passwordIn) {
        password = passwordIn;
    }

    public String authenticateUser() {

        String targetPage = "/index";

        try {

            authenticate();
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest)context.getExternalContext().getRequest();
            String previouslyTargettedPage = (String)request.getAttribute("targetted_page");
            if(null != previouslyTargettedPage ) {
                targetPage =previouslyTargettedPage;
            }
        } catch (LoginException le) {
             FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage("The username and password combination entered was not able to be authenticated."));
            targetPage = "/security/login";
        } catch (ServletException le) {
             FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage("The username and password combination entered was not able to be authenticated."));
            targetPage = "/security/login";
        }

        return targetPage;
    }


    private void authenticate() throws LoginException, ServletException {

        HttpServletRequest request = (HttpServletRequest )FacesContext.getCurrentInstance().getExternalContext().getRequest();

        request.login(userName, password);

    }

}
