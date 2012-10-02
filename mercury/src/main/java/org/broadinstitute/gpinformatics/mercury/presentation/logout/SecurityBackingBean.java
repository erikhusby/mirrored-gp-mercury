package org.broadinstitute.gpinformatics.mercury.presentation.logout;

import org.apache.commons.logging.Log;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Scott Matthews
 *         Date: 4/23/12
 *         Time: 11:47 AM
 */

@ManagedBean
@RequestScoped
public class SecurityBackingBean {

    @Inject
    private Log logger;

    public String logout() {

        String result = "/index?faces-redirect=true";

        FacesContext context = FacesContext.getCurrentInstance();

        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        try {
            logger.info("Attempting Logout");
            request.logout();
        } catch (ServletException ex) {
            logger.error("Logout Failed");
            result = request.getRequestURI();
        }

        return result;
    }
}
