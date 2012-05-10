package org.broadinstitute.sequel.presentation.tags.security;

import javax.faces.view.facelets.TagConfig;

/**
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 3:23 PM
 */
public class LoggedOutTag extends LoggedInTag {


    public LoggedOutTag(TagConfig tagConfigIn) {
        super(tagConfigIn);
    }

    @Override
    protected boolean checkAuthentication() {
        return !super.checkAuthentication();
    }
}
