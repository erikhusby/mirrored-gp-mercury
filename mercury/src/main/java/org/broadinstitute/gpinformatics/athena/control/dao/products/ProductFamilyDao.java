package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily_;
import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;


/**
 * DAO for {@link ProductFamily}s
 */
public class ProductFamilyDao extends GenericDao {


    /**
     * Return the ProductFamily corresponding to the well known ProductFamily.Name.
     *
     * @param productFamilyName
     * @return
     */
    public ProductFamily find(ProductFamily.Name productFamilyName) {

        EntityManager em = getThreadEntityManager().getEntityManager();


        CriteriaQuery<ProductFamily> criteriaQuery =
                em.getCriteriaBuilder().createQuery(ProductFamily.class);

        Root<ProductFamily> root = criteriaQuery.from(ProductFamily.class);
        criteriaQuery.where(em.getCriteriaBuilder().equal(root.get(ProductFamily_.name), productFamilyName.name()));


        return em.createQuery(criteriaQuery).getSingleResult();

    }
}
