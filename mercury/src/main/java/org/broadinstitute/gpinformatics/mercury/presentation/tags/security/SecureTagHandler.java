package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 *
 * SecureTagHandler is the base tag class for all Custom tags that deal with Authentication and authorization
 *
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 2:56 PM
 */
public abstract class SecureTagHandler extends TagHandler{

    protected SecureTagHandler(TagConfig tagConfigIn) {
        super(tagConfigIn);
    }

    /**
     * getUser retrieves the {@link Principal Principal} associated with the currently logged in user from
     * the request in order to support the authentication tags
     * @return
     */
    protected Principal getUser () {
        FacesContext currContext = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest)currContext.getExternalContext().getRequest();
        return request.getUserPrincipal();
    }

}
