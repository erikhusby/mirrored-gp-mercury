package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Product converter that will return an empty dummy product if no product can be found with the specified part number
 */
@Named
public class ProductCreateEditConverter extends ProductConverter {

    @Inject
    private ProductDao productDao;


    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String partNumber) {

        Product product = productDao.findByPartNumberEagerProductFamily(partNumber);

        if (product != null) {
            return product;
        }

        return Product.makeEmptyProduct();
    }

}
