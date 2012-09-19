package org.broadinstitute.gpinformatics.athena.presentation.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@RequestScoped
public class LogoutHandler extends AbstractJsfBean {
    private static final long serialVersionUID = -5344292141461478023L;

    private static final Log log = LogFactory.getLog(LogoutHandler.class);

    public String logout() {

        String result = "/index?faces-redirect=true";

        FacesContext context = FacesContext.getCurrentInstance();

        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        try {
            log.info("Attempting Logout");
            request.logout();
        } catch (ServletException ex) {
            log.error("Logout Failed", ex);
            result = request.getRequestURI();
        }
        return result;
    }
}
