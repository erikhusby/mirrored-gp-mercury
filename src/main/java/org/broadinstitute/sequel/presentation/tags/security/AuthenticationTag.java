package org.broadinstitute.sequel.presentation.tags.security;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import java.io.IOException;

/**
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

    protected abstract boolean checkAuthentication();

    protected boolean showTagBody() {

       if(checkAuthentication()) {
           return true;
       } else {
           return false;
       }

    }
}
