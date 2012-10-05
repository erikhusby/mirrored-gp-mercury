package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.control.dao.AthenaGenericDao;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;


/**
 * Dao for {@link ProductFamily}s
 */
@Stateful
@RequestScoped
public class ProductFamilyDao extends AthenaGenericDao {


    /**
     * Return the ProductFamily corresponding to the well known ProductFamily.Name.
     *
     * @param productFamilyName
     * @return
     */
    public ProductFamily find(ProductFamily.ProductFamilyName productFamilyName) {

        try {

            EntityManager em = em();

            CriteriaQuery<ProductFamily> criteriaQuery =
                    em.getCriteriaBuilder().createQuery(ProductFamily.class);

            Root<ProductFamily> root = criteriaQuery.from(ProductFamily.class);
            criteriaQuery.where(em.getCriteriaBuilder().equal(root.get(ProductFamily_.name), productFamilyName.name()));

            return em.createQuery(criteriaQuery).getSingleResult();
        }
        catch (NoResultException nrx) {
            return null;
        }

    }
}
