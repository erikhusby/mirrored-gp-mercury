package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Token Input support for Products.
 *
 * @author hrafal
 */
@Dependent
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
        Collection<String> searchTerms = extractSearchTerms(query);
        List<Product> products = productDao.findTopLevelProductsForProductOrder(searchTerms);
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
        Collection<String> searchTerms = extractSearchTerms(query);
        List<Product> addOns = productDao.searchProductsForAddOnsInProductEdit(editProduct, searchTerms);
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
