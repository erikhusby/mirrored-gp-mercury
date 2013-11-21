package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
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
        return product.getDisplayName();
    }

    @Override
    protected String formatMessage(String messageString, Product product) {
        return MessageFormat.format(messageString, product.getDisplayName());
    }

    public String getAddOnsJsonString(Product editProduct, String query) throws JSONException {
        List<Product> addOns = productDao.searchProductsForAddonsInProductEdit(editProduct, query);
        return createItemListString(addOns);
    }

    public List<String> getBusinessKeyList() {
        List<Product> products = getTokenObjects();

        List<String> businessKeyList = new ArrayList<>();
        for (Product product : products) {
            businessKeyList.add(product.getBusinessKey());
        }

        return businessKeyList;
    }
}
