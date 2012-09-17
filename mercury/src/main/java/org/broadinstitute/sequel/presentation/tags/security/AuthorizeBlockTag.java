package org.broadinstitute.sequel.presentation.tags.security;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 5/2/12
 *         Time: 10:23 AM
 */
public class AuthorizeBlockTag extends AuthorizationTag {

    private final TagAttribute roles;


    public AuthorizeBlockTag(TagConfig tagConfigIn) {
        super(tagConfigIn);
        this.roles = this.getRequiredAttribute("roles");

    }

    private List<String> getAttrValues(FaceletContext ctx, TagAttribute attr) {
        List<String> values = new LinkedList<String>();
        String value;
        if (attr.isLiteral()) {
            value = attr.getValue(ctx);
        } else {
            ValueExpression expression = attr.getValueExpression(ctx, String.class);
            value = (String) expression.getValue(ctx);
        }

        if(null != value) {
            values.addAll(Arrays.asList(value.split(",")));
        }
        return values;
    }


    @Override
    protected void alternateOptions() {

    }

    @Override
    protected boolean isAuthorized(FaceletContext faceletContextIn) {

        boolean authorized = false;

        FacesContext currContext = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest)currContext.getExternalContext().getRequest();


        for(String currGroup:getAttrValues(faceletContextIn, roles)){
            if(request.isUserInRole(currGroup) || currGroup.equals("All")){
                authorized = true;
                break;
            }
        }

        return authorized;
    }
}
