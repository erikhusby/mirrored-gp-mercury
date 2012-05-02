package org.broadinstitute.sequel.presentation.tags.security;

import org.broadinstitute.sequel.boundary.authentication.AuthenticationService;

import javax.faces.view.facelets.TagConfig;
import javax.inject.Inject;

/**
 * @author Scott Matthews
 *         Date: 5/2/12
 *         Time: 10:23 AM
 */
public class AuthorizeBlockTag extends AuthorizationTag {

    public AuthorizeBlockTag(TagConfig tagConfigIn) {
        super(tagConfigIn);
    }

    @Override
    protected void alternateOptions() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected boolean isAuthorized() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
