package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class ProductForm extends AbstractJsfBean {

    @Inject
    private ProductDetail detail;

    @Inject
    private ProductDao productDao;


    public String create() {
        Product product = detail.getProduct();

        //TODO hmc under construction
        if (product != null) {

        }

//        product.setDefaultPriceItem();

        productDao.persist(product);
        //TODO hmc add more info in the details param
        addInfoMessage("Product created.", "Product has been created.");
        return redirect("list");
    }


}
