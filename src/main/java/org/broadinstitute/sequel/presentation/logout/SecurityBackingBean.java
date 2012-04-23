package org.broadinstitute.sequel.presentation.logout;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Scott Matthews
 *         Date: 4/23/12
 *         Time: 11:47 AM
 */

@ManagedBean
@RequestScoped
public class SecurityBackingBean {


    Logger securityLogger = Logger.getLogger(this.getClass().getName());


    public String logout() {

        String result = "/index?faces-redirect=true";

        FacesContext context = FacesContext.getCurrentInstance();

        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        try {
            securityLogger.log(Level.WARNING, "Attempting Logout");
            request.logout();
        } catch (ServletException ex) {
            securityLogger.log(Level.SEVERE, "Logout Failed");
            result = request.getRequestURI();
        }

        return result;

    }

}
