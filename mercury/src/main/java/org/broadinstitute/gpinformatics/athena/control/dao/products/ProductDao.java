package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

@Stateful
@RequestScoped
/**
 *
 * Dao for {@link Product}s, supporting the browse and CRUD UIs.
 *
 */
public class ProductDao extends GenericDao {

    /**
     * Current 2012-10-04 UI Product Details mockup shows a three column table with Part#, Name, and Description, all
     * simple properties not requiring any fetches.
     *
     * @return
     */
    public List<Product> findTopLevelProducts() {
        return findList(Product.class, Product_.topLevelProduct, true);
    }

    /**
     * Find a Product by the specified part number.  Currently not fetching through to addOns or PriceItems since
     * there probably aren't enough of them to really bog things down, but that decision can be revisited if needed.
     *
     * @param partNumber
     *
     * @return
     */
    public Product findByPartNumber(String partNumber) {
        return findSingle(Product.class, Product_.partNumber, partNumber);
    }
}
