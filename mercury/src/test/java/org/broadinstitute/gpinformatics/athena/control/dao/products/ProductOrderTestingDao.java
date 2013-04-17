package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

@Stateful
@RequestScoped
/**
 * DAO with methods called only by tests, so as to not to increase the complexity of regular DAOs unnecessarily.
 */
public class ProductOrderTestingDao extends GenericDao {

    public List<ProductOrder> findByProductName(@Nonnull final String productName) {
        return findAll(ProductOrder.class, new GenericDao.GenericDaoCallback<ProductOrder>() {
            @Override
            public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> root) {
                CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
                Join<ProductOrder, Product> productJoin = root.join(ProductOrder_.product);

                criteriaQuery.where(criteriaBuilder.equal(productJoin.get(Product_.productName), productName));
            }
        });
    }

}
