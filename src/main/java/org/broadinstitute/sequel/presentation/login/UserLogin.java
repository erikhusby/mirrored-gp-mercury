package org.broadinstitute.sequel.presentation.login;

/**
 * @author Scott Matthews
 *         Date: 4/2/12
 *         Time: 1:35 PM
 */

//import com.atlassian.crowd.application.jaas.CrowdLoginModule;

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
public class UserLogin {

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
        } catch (LoginException le) {
             FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage("The username and password combination entered was not able to be authenticated."));
            targetPage = "/login/login";
        } catch (ServletException le) {
             FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage("The username and password combination entered was not able to be authenticated."));
            targetPage = "/login/login";
        }

        return targetPage;
    }


    public void authenticate() throws LoginException, ServletException {

        HttpServletRequest request = (HttpServletRequest )FacesContext.getCurrentInstance().getExternalContext().getRequest();

        request.login(userName, password);

//        LoginContext ctx = new LoginContext("sequel",null,new AuthenticationCallback(),new LoginConfig());
//        ctx.login();
//        return ctx.getSubject();
    }
//
//    public class AuthenticationCallback implements CallbackHandler {
//
//        public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
//            for (int i = 0; i < callbacks.length; i++) {
//                if (callbacks[i] instanceof NameCallback) {
//                    NameCallback nc = (NameCallback) callbacks[i];
//                    nc.setName(userName);
//                } else if (callbacks[i] instanceof PasswordCallback) {
//                    PasswordCallback pc = (PasswordCallback) callbacks[i];
//                    pc.setPassword(password.toCharArray());
//                } else {
//                    throw (new UnsupportedCallbackException(callbacks[i],
//                            "Callback handler not supported"));
//                }
//            }
//        }
//    }
//
//    public class LoginConfig extends Configuration {
//        @Override
//        public AppConfigurationEntry[] getAppConfigurationEntry(String s) {
//            AppConfigurationEntry[] entries = new AppConfigurationEntry[1];
//            Map<String, String> args = new HashMap<String, String>(10);
//            args.put("crowd.server.url", "https://crowd.broadinstitute.org:8443/crowd");
//            args.put("application.name", "sequel");
//            args.put("application.password", "fsR7n3Iq");
//            entries[0] = new AppConfigurationEntry(CrowdLoginModule.class.getName(),AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,args );
//            return entries;
//        }
//    }


}
