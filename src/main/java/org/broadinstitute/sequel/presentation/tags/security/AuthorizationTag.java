package org.broadinstitute.sequel.presentation.tags.security;

import org.broadinstitute.sequel.presentation.logout.SecurityBackingBean;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Scott Matthews
 *         Date: 5/2/12
 *         Time: 10:16 AM
 */
public abstract class AuthorizationTag extends SecureTagHandler{

    private Collection<String> groups;

    protected AuthorizationTag(TagConfig tagConfigIn) {
        super(tagConfigIn);
    }

    @Override
    public void apply(FaceletContext faceletContextIn, UIComponent uiComponentIn) throws IOException {
        if(isAuthorized()) {
            this.nextHandler.apply(faceletContextIn, uiComponentIn);
        } else {
            alternateOptions();
        }
    }

    protected abstract void alternateOptions() ;
    protected abstract boolean isAuthorized();

}
