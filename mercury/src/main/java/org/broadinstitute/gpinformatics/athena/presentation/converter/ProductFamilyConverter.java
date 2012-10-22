package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ProductFamilyConverter implements Converter {

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        ProductFamily.ProductFamilyName productFamilyName = ProductFamily.ProductFamilyName.valueOf(value);
        return productFamilyDao.find(productFamilyName);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        if (object != null) {
            return ((ProductFamily.ProductFamilyName) object).getDisplayName();
        }
        return "";
    }
}
