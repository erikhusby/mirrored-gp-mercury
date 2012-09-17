package org.broadinstitute.pmbridge.presentation.security;

import org.apache.log4j.Logger;
import org.broadinstitute.pmbridge.presentation.AbstractJsfBean;

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

        FacesContext context = FacesContext.getCurrentInstance();
        try {
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

            request.login(userName, password);

            String previouslyTargetedPage = (String)request.getAttribute("targeted_page");
            if (previouslyTargetedPage != null) {
                targetPage = previouslyTargetedPage;
            }
        } catch (ServletException le) {
            LOG.warn("Couldn't authenticate user '" + userName + "'", le);
            context.addMessage(null, new FacesMessage("The username and password combination entered couldn't be authenticated."));
            targetPage = AuthorizationFilter.LOGIN_PAGE;
        }

        return targetPage;
    }
}
