package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import java.io.IOException;
import java.util.Collection;

/**
 * Tag support for the application of security rules.
 *
 * @author Scott Matthews
 */
public abstract class AuthorizationTag extends SecureTagHandler {
    private Collection<String> groups;

    protected AuthorizationTag(TagConfig tagConfigIn) {
        super(tagConfigIn);
    }

    @Override
    public void apply(FaceletContext faceletContextIn, UIComponent uiComponentIn) throws IOException {
        if (isAuthorized(faceletContextIn)) {
            this.nextHandler.apply(faceletContextIn, uiComponentIn);
        } else {
            alternateOptions();
        }
    }

    protected abstract void alternateOptions();

    protected abstract boolean isAuthorized(FaceletContext faceletContextIn);
}
