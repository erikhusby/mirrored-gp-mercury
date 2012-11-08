package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import javax.faces.view.facelets.TagConfig;

/**
 * @author Scott Matthews
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
