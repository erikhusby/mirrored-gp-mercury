package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

/**
 * Token Input support for Products.
 *
 * @author hrafal
 */
public class ProductTokenInput extends TokenInput<Product> {

    @Inject
    private ProductDao productDao;

    public ProductTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected Product getById(String key) {
        return productDao.findByBusinessKey(key);
    }

    public String getJsonString(String query) throws JSONException {
        List<Product> products = productDao.searchProducts(query);
        return createItemListString(products);
    }

    @Override
    protected String getTokenId(Product product) {
        return product.getBusinessKey();
    }

    @Override
    protected String getTokenName(Product product) {
        return product.getProductName() + " [" + product.getBusinessKey() + "]";
    }

    @Override
    protected String formatMessage(String messageString, Product product) {
        return MessageFormat.format(messageString, product.getProductName() + " [" + product.getBusinessKey() + "]");
    }

    public String getAddOnsJsonString(Product editProduct, String query) throws JSONException {
        List<Product> addOns = productDao.searchProductsForAddonsInProductEdit(editProduct, query);
        return createItemListString(addOns);
    }

    public String getTokenObject() {
        List<Product> projects = getTokenObjects();

        if ((projects == null) || projects.isEmpty()) {
            return "";
        }

        return projects.get(0).getBusinessKey();
    }
}
