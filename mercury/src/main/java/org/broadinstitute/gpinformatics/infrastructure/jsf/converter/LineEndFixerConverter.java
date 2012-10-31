package org.broadinstitute.gpinformatics.infrastructure.jsf.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * JSF converter that trims converts \r\n to \n for string values.
 *
 * @author breilly
 */
@FacesConverter("lineEndFixer")
public class LineEndFixerConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
        return value == null ? null : value.replaceAll("\\r\\n", "\n");
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        return (String) value;
    }
}
