package org.broadinstitute.sequel.presentation.tags.security;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.TagConfig;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 3:08 PM
 */
public class LoggedInTag extends AuthenticationTag {

    public LoggedInTag(TagConfig tagConfigIn) {
        super(tagConfigIn);
    }

    @Override
    protected boolean checkAuthentication() {

        return (getUser() != null);
    }
}
