package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * This class is the user implementation of the token object
 *
 * @author hrafal
 */
@Named
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

        JSONArray itemList = new JSONArray();
        for (Product product : products) {
            createAutocomplete(itemList, product);
        }

        return itemList.toString();
    }

    @Override
    protected String generateCompleteData() throws JSONException {
        JSONArray itemList = new JSONArray();
        for (Product product : getTokenObjects()) {
            createAutocomplete(itemList, product);
        }

        return itemList.toString();
    }

    private static void createAutocomplete(JSONArray itemList, Product product) throws JSONException {
        JSONObject item = getJSONObject(product.getBusinessKey(), product.getProductName(), false);
        itemList.put(item);
    }

    public String getAddOnsJsonString(Product editProduct, String query) throws JSONException {
        List<Product> addOns = productDao.searchProductsForAddonsInProductEdit(editProduct, query);

        JSONArray itemList = new JSONArray();
        for (Product addOn : addOns) {
            JSONObject item = getJSONObject(addOn.getBusinessKey(), addOn.getDisplayName(), false);
            itemList.put(item);
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
