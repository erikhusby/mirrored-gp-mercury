package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ProductConverter extends AbstractConverter {

    @Inject
    private ProductDao productDao;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        Product product = productDao.findByBusinessKey(value);
        return product;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        if (object != null) {
            return ((Product) object).getBusinessKey();
        }
        return "";
    }
}
