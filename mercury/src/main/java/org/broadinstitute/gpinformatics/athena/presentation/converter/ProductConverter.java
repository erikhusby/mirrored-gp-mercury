package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ProductConverter implements Converter {

    @Inject
    private ProductDao productDao;

    public Product getAsObject(String businessKey) {
        Product product = productDao.findByBusinessKey(businessKey);
        return product;
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return getAsObject(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        if (object != null) {
            return ((Product) object).getBusinessKey();
        }
        return "";
    }
}
