package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;


/**
 * Dao for {@link ProductFamily}s
 */
@Stateful
@RequestScoped
public class ProductFamilyDao extends GenericDao {

    public List<ProductFamily> findAll() {
        return findAll(ProductFamily.class);
    }

    /**
     * Return the ProductFamily corresponding to the well known ProductFamily.Name.
     *
     * @param productFamilyName
     * @return
     */
    public ProductFamily find(String productFamilyName) {
        return findSingle(ProductFamily.class, ProductFamily_.name, productFamilyName);
    }

    /**
     * Find a ProductFamily by it's primary key identifier
     * @param productFamilyId
     * @return
     */
    public ProductFamily find(@Nonnull Long productFamilyId) {
        return findSingle(ProductFamily.class, ProductFamily_.productFamilyId, productFamilyId);
    }

}
