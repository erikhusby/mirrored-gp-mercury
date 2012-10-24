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
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        ProductFamily productFamily = null;
        if (StringUtils.isNotBlank(value)) {
            ProductFamily.ProductFamilyName productFamilyName = ProductFamily.ProductFamilyName.valueOf(value);
            productFamily = productFamilyDao.find(productFamilyName);
        }
        return productFamily;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        //TODO hmc should not need this extra check but cannot yet figure where the Long object is coming from
        if ((object != null) && (object instanceof ProductFamily)) {
            return ((ProductFamily) object).getName();
        }
        return "";
    }
}
