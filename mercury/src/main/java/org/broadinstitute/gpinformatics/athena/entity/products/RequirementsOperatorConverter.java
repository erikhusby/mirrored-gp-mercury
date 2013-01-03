package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.presentation.converter.AbstractConverter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
* @author pshapiro
*/
@Named
public class RequirementsOperatorConverter extends AbstractConverter {

    public Object[] getAllItems() {
        return BillingRequirement.Operator.values();
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return BillingRequirement.Operator.fromLabel(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        // check for null because the converter might be passed null during an AJAX request
        if (object != null) {
            return ((BillingRequirement.Operator) object).label;
        }
        return "";
    }
}
