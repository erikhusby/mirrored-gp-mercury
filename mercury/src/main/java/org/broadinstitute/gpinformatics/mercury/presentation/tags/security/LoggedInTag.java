package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import javax.faces.view.facelets.TagConfig;

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
