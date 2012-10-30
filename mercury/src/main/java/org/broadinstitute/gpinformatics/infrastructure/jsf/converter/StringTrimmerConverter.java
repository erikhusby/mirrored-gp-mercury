package org.broadinstitute.gpinformatics.infrastructure.jsf.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * JSF converter that trims whitespace from string values.
 *
 * @author breilly
 */
@FacesConverter("stringTrimmer")
public class StringTrimmerConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
        return value == null ? null : value.trim();
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        return (String) value;
    }
}
