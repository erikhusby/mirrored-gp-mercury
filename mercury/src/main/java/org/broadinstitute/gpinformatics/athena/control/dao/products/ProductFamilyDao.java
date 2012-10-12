package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;


/**
 * Dao for {@link ProductFamily}s
 */
@Stateful
@RequestScoped
public class ProductFamilyDao extends GenericDao {


    public List<ProductFamily> findAll() {

        EntityManager em = getEntityManager();

        CriteriaQuery<ProductFamily> criteriaQuery =
                em.getCriteriaBuilder().createQuery(ProductFamily.class);

        Root<ProductFamily> root = criteriaQuery.from(ProductFamily.class);

        return em.createQuery(criteriaQuery).getResultList();
    }


    /**
     * Return the ProductFamily corresponding to the well known ProductFamily.Name.
     *
     * @param productFamilyName
     * @return
     */
    public ProductFamily find(ProductFamily.ProductFamilyName productFamilyName) {

        return findSingle(ProductFamily.class, ProductFamily_.name, productFamilyName.name());

    }
}
