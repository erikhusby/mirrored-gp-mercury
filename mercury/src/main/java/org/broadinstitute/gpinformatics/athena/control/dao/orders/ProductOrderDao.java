package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s.
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/5/12
 * Time: 6:17 PM
 */

@Stateful
@RequestScoped
public class ProductOrderDao extends GenericDao {

    /**
     *
     * @param key
     * @return
     */
    public ProductOrder findByBusinessKey(String key) {
        return findSingle(ProductOrder.class, ProductOrder_.jiraTicketKey, key);
    }

    /**
     * Find ProductOrders by Research Project
     * @param researchProject
     * @return
     */
    public List<ProductOrder> findByResearchProject( ResearchProject researchProject ) {
        return findList(ProductOrder.class, ProductOrder_.researchProject, researchProject);

    }


    /**
     * Find a productOrder by its containing ResearchProject and title
     * @param orderTitle
     * @param researchProject
     * @return
     */
    public ProductOrder findByResearchProjectAndTitle(@NotNull ResearchProject researchProject, @NotNull String orderTitle) {
        ProductOrder productOrder = null;

        if (researchProject == null) {
            throw new NullPointerException("Null Research Project.");
        }

        if (orderTitle == null) {
            throw new NullPointerException("Null Product Order Title.");
        }

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductOrder> criteriaQuery =
                criteriaBuilder.createQuery(ProductOrder.class);

        List<Predicate> predicateList = new ArrayList<Predicate>();

        Root<ProductOrder> productOrderRoot = criteriaQuery.from(ProductOrder.class);
        predicateList.add(criteriaBuilder.equal(productOrderRoot.get(ProductOrder_.researchProject), researchProject));
        predicateList.add(criteriaBuilder.equal(productOrderRoot.get(ProductOrder_.title), orderTitle));

        Predicate[] predicates = new Predicate[predicateList.size()];
        criteriaQuery.where(predicateList.toArray(predicates));

        try {
            return entityManager.createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }

    }

    /**
     * Find all ProductOrders.
     * Not sure if we need this but have put it in here just in case.
     * @return
     */
    public List<ProductOrder> findAll() {
        return findAll(ProductOrder.class);
    }


    //TODO hmc find Order by (PM) Person Id.  This needs a person Id on ProductOrder
    /**
     * Find all the ProductOrders for a person who is
     * associated ( as the creator ) with the ProductOrders
     *
     * @param personId
     * @return
     */
    /*
    public ProductOrder findByPersonId(Long personId) {
        findSingle(ProductOrder.class, ProductOrder_.personId, personId);
    }
    */

    /**
     * Find a ProductOrder by it's primary key identifier
     * @param orderId
     * @return
     */
    public ProductOrder findById(Long orderId) {
        return findSingle(ProductOrder.class, ProductOrder_.productOrderId, orderId);
    }


}
