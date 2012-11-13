package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
     * Find the order using the business key (the jira ticket number).
     *
     * @param key The business key to look up
     *
     * @return The matching order
     */
    public ProductOrder findByBusinessKey(String key) {
        return findSingle(ProductOrder.class, ProductOrder_.jiraTicketKey, key);
    }

    /**
     * Find ProductOrders by Research Project
     *
     * @param researchProject The project
     *
     * @return The matching list of orders
     */
    public List<ProductOrder> findByResearchProject(ResearchProject researchProject) {
        return findList(ProductOrder.class, ProductOrder_.researchProject, researchProject);
    }


    /**
     * Find a productOrder by its containing ResearchProject and title
     *
     * @param orderTitle The title to look up
     * @param researchProject The project
     *
     * @return The order that matches the project and title
     */
    public ProductOrder findByResearchProjectAndTitle(@Nonnull ResearchProject researchProject, @Nonnull String orderTitle) {
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
     * Find all ProductOrders. Not sure if we need this but have put it in here just in case.
     *
     * @return All the orders
     */
    public List<ProductOrder> findAll() {
        return findAll(ProductOrder.class);
    }


    /**
     * Find all the ProductOrders for a person who is
     * associated ( as the creator ) with the ProductOrders
     *
     * @param personId The person to filter on
     *
     * @return The products for this person
     */
    public List<ProductOrder> findByCreatedPersonId(@Nonnull Long personId) {
        if (personId == null) {
            throw new NullPointerException("Null Person Id.");
        }

        return findList(ProductOrder.class, ProductOrder_.createdBy, personId);
    }

    /**
     * Find all the ProductOrders for a person who is
     * associated ( as the modifier ) with the ProductOrders
     *
     * @param personId The person to filter on
     *
     * @return The products for this person
     */
    public List<ProductOrder> findByModifiedPersonId(@Nonnull Long personId) {
        if (personId == null) {
            throw new NullPointerException("Null Person Id.");
        }

        return findList(ProductOrder.class, ProductOrder_.modifiedBy, personId);
    }

    /**
     * Find a ProductOrder by it's primary key identifier
     *
     * @param orderId The order id to look up
     *
     * @return the order that matches
     */
    public ProductOrder findById(@Nonnull Long orderId) {
        if (orderId == null) {
            throw new NullPointerException("Null ProductOrder Id.");
        }

        return findSingle(ProductOrder.class, ProductOrder_.productOrderId, orderId);
    }
}
