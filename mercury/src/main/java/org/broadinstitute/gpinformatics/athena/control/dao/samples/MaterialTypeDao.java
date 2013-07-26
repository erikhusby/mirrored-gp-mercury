package org.broadinstitute.gpinformatics.athena.control.dao.samples;

import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Stateful
@RequestScoped
/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType}s
 */
public class MaterialTypeDao extends GenericDao {

    /**
     * The Product Details CRUD UI will want to enumerate "allowable"
     * {@link org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType}s for attachment
     * to {@link org.broadinstitute.gpinformatics.athena.entity.products.Product}s
     * 
     * @return
     */
    public List<MaterialType> findAll() {
        return findAll(MaterialType.class);
    }

    /**
     * Find a {@link org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType}s by its unique category and name
     *
     * @param categoryName
     * @param materialTypeName
     * @return
     */
    public MaterialType find( String categoryName, String materialTypeName) {

        if (categoryName == null) {
            throw new NullPointerException("Null material category!");
        }

        if (materialTypeName == null) {
            throw new NullPointerException("Null material name!");
        }

        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<MaterialType> cq = cb.createQuery(MaterialType.class);
        List<Predicate> predicateList = new ArrayList<>();

        Root<MaterialType> MaterialType = cq.from(MaterialType.class);

        predicateList.add(cb.equal(MaterialType.get(MaterialType_.category), categoryName));
        predicateList.add(cb.equal(MaterialType.get(MaterialType_.name), materialTypeName));

        Predicate[] predicates = new Predicate[predicateList.size()];
        cq.where(predicateList.toArray(predicates));

        try {
            return getEntityManager().createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
