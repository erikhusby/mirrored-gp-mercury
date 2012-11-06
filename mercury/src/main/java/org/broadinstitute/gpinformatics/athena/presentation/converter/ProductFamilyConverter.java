package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.apache.commons.lang.StringUtils;
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
    public Object getAsObject(FacesContext context, UIComponent component, String productFamilyName) {
        ProductFamily productFamily = null;
        if (StringUtils.isNotBlank(productFamilyName)) {
            productFamily = productFamilyDao.find(productFamilyName);
        }
        return productFamily;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        if ((object != null) && (object instanceof ProductFamily)) {
            return ((ProductFamily) object).getName();
        }
        return "";
    }
}
