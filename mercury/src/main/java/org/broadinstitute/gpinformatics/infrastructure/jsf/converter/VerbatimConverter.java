package org.broadinstitute.gpinformatics.infrastructure.jsf.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * JSF converter that performs no actual conversion. Can be used to disable other default converters, e.g.
 * {@link org.broadinstitute.gpinformatics.infrastructure.jsf.converter.StringSanitizerConverter}.
 *
 * @author breilly
 */
@FacesConverter("verbatim")
public class VerbatimConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
        return value;
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        return (String) value;
    }
}
