package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
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
    protected boolean isSingleLineMenuEntry() {
        return true;
    }

    @Override
    protected String getTokenId(Product product) {
        return product.getBusinessKey();
    }

    @Override
    protected String getTokenName(Product product) {
        return product.getProductName();
    }

    @Override
    protected String[] getMenuLines(Product product) {
        String[] lines = new String[1];
        lines[0] = product.getProductName() + " [" + product.getBusinessKey() + "]";
        return lines;
    }

    public String getAddOnsJsonString(Product editProduct, String query) throws JSONException {
        List<Product> addOns = productDao.searchProductsForAddonsInProductEdit(editProduct, query);

        JSONArray itemList = new JSONArray();
        for (Product addOn : addOns) {
            createAutocomplete(itemList, addOn);
        }

        return itemList.toString();
    }

    public String getTokenObject() {
        List<Product> projects = getTokenObjects();

        if ((projects == null) || projects.isEmpty()) {
            return "";
        }

        return projects.get(0).getBusinessKey();
    }
}
