package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Convert the product order automatically with JSF.
 *
 * @author Michael Dinsmore
 */
@Named
public class ProductOrderConverter implements Converter {
    @Inject
    private ProductOrderDao productOrderDao;

    @Override
    public ProductOrder getAsObject(FacesContext context, UIComponent component, String value) {
        return productOrderDao.findByBusinessKey(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        // check for null because the converter might be passed null due to an ajax partial update
        return object == null ? "" : ((ProductOrder) object).getBusinessKey();
    }
}