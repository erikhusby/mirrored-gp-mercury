package org.broadinstitute.gpinformatics.athena.presentation.security;

import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.athena.presentation.AbstractJsfBean;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@RequestScoped
public class UserLogin extends AbstractJsfBean {
    private static final Logger LOG = Logger.getLogger(UserLogin.class);

    private static final long serialVersionUID = 8696721679504370838L;

    private String username;
    private String password;

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
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

            request.login(username, password);
            context.addMessage(null,new FacesMessage(FacesMessage.SEVERITY_INFO, "Welcome back!", "Sign in successful"));

            String previouslyTargetedPage = (String)request.getAttribute("targeted_page");
            if (previouslyTargetedPage != null) {
                targetPage = previouslyTargetedPage;
            }
        } catch (ServletException le) {
            LOG.warn("Couldn't authenticate user '" + username + "'", le);
            context.addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR, "The username and password you entered is incorrect.  Please try again.", "Authentication error"));
            targetPage = org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationFilter.LOGIN_PAGE;
        }

        return targetPage;
    }
}
