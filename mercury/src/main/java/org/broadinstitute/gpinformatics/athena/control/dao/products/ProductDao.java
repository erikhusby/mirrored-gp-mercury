package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.control.dao.AthenaGenericDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

@Stateful
@RequestScoped
/**
 *
 * Dao for {@link Product}s, supporting the browse and CRUD UIs.
 *
 */
public class ProductDao extends AthenaGenericDao {


    /**
     * Current 2012-10-04 UI Product Details mockup shows a three column table with Part#, Name, and Description, all
     * simple properties not requiring any fetches.
     *
     * @return
     */
    public List<Product> findTopLevelProducts() {

        EntityManager em = em();

        CriteriaQuery<Product> criteriaQuery =
                em.getCriteriaBuilder().createQuery(Product.class);

        Root<Product> root = criteriaQuery.from(Product.class);
        criteriaQuery.where(em.getCriteriaBuilder().equal(root.get(Product_.topLevelProduct), true));

        return em.createQuery(criteriaQuery).getResultList();

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

        EntityManager em = em();

        CriteriaQuery<Product> criteriaQuery =
                em.getCriteriaBuilder().createQuery(Product.class);

        Root<Product> root = criteriaQuery.from(Product.class);
        criteriaQuery.where(em.getCriteriaBuilder().equal(root.get(Product_.partNumber), partNumber));

        return em.createQuery(criteriaQuery).getSingleResult();

    }
}
