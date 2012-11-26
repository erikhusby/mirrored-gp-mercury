package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class BillingSessionConverter implements Converter {
    @Inject
    private BillingSessionDao billingSessionDao;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return billingSessionDao.findByBusinessKey(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        // check for null because the converter might be passed null due to an ajax partial update
        return object == null ? "" : ((BillingSession) object).getBusinessKey();
    }
}
