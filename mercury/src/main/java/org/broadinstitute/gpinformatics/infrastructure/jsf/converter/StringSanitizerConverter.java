package org.broadinstitute.gpinformatics.infrastructure.jsf.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * JSF converter that automatically trims whitespace and converts \r\n to \n for string values. If this behavior is not
 * desired for a specific component, use the
 * {@link org.broadinstitute.gpinformatics.infrastructure.jsf.converter.VerbatimConverter}.
 *
 * @author breilly
 */
@FacesConverter(forClass = String.class)
public class StringSanitizerConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
        return value == null ? null : value.trim().replaceAll("\\r\\n", "\n");
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        return (String) value;
    }
}
