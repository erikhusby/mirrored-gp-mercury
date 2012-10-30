package org.broadinstitute.gpinformatics.infrastructure.jsf.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * JSF converter that automatically trims whitespace from string values. If this behavior is not desired for a specific
 * component, use the {@link VerbatimConverter}.
 *
 * @author breilly
 */
@FacesConverter(forClass = String.class)
public class StringTrimmerConverter implements Converter {

    public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
        return value != null ? value.trim() : null;
    }

    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        return (String) value;
    }
}
