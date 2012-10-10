package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 * @author breilly
 */
@RequestScoped
@FacesConverter(value = "bspUserConverter", forClass = BspUser.class)
public class BspUserConverter implements Converter {

    // TODO: add seam-faces dependency to allow for injection into converters
    @Inject
    private BSPUserList userList;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return userList.getById(Long.parseLong(value));
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        return ((BspUser) object).getUserId().toString();
    }
}
