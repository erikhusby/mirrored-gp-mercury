package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * JSF converter for Product instances.
 */
@Named
public class ProductConverter extends AbstractConverter {

    @Inject
    private ProductDao productDao;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return productDao.findByBusinessKey(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        // check for null because the converter might be passed null during an AJAX request
        if (object != null) {
            return ((Product) object).getBusinessKey();
        }
        return "";
    }
}
