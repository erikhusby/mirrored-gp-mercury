package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author breilly
 */
@Named
public class BspUserConverter implements Converter {

    @Inject
    private BSPUserList userList;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return userList.getById(Long.parseLong(value));
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        Long userId = ((BspUser) object).getUserId();
        if (userId != null) {
            return userId.toString();
        } else {
            return null;
        }
    }
}
