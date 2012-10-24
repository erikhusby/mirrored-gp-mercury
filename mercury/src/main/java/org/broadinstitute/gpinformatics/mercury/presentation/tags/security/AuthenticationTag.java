package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import java.io.IOException;

/**
 *
 * AuthenticationTag is the base class for all custom tags that deal specifically with authentication
 *
 * @author Scott Matthews
 *         Date: 5/1/12
 *         Time: 3:03 PM
 */
public abstract class AuthenticationTag extends SecureTagHandler {
    protected AuthenticationTag(TagConfig tagConfigIn) {
        super(tagConfigIn);
    }

    @Override
    public void apply(FaceletContext faceletContextIn, UIComponent uiComponentIn) throws IOException {
        if(showTagBody()) {
            this.nextHandler.apply(faceletContextIn, uiComponentIn);
        }
    }

    /**
     * implemented by the child classes,checkAuthentication assists in determining if the user should have
     * access to the resource that this tag surrounds
     * @return
     */
    protected abstract boolean checkAuthentication();

    /**
     *
     * showTagBody is a helper method to determine if the resource that this tag surrounds should be allowed to be
     * shown based on the users authentication status.
     *
     * @return
     */
    protected boolean showTagBody() {
       if(checkAuthentication()) {
           return true;
       } else {
           return false;
       }
    }
}
