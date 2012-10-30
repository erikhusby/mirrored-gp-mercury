package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.*;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/10/12
 * Time: 2:34 PM
 */
@Stateful
@RequestScoped
public class BillableItemDao extends GenericDao {


    //TODO hmc not tested yet
    public List<BillableItem> findByProductOrderSample(ProductOrderSample productOrderSample ) {

        EntityManager em = getEntityManager();
        CriteriaQuery<BillableItem> criteriaQuery =
                getEntityManager().getCriteriaBuilder().createQuery(BillableItem.class);

        Root<BillableItem> billableItemRoot = criteriaQuery.from(BillableItem.class);
        criteriaQuery.where(em.getCriteriaBuilder().equal(billableItemRoot.get(BillableItem_.productOrderSample), productOrderSample));
        try {
            return em.createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    //TODO hmc not tested yet
    public List<BillableItem> findByProductOrder(ProductOrder productOrder) {

        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BillableItem> criteriaQuery = cb.createQuery(BillableItem.class);
        Root<BillableItem> billableItemRoot = criteriaQuery.from(BillableItem.class);

        criteriaQuery.where(em.getCriteriaBuilder().equal(
            billableItemRoot.join(BillableItem_.productOrderSample).join(ProductOrderSample_.productOrder), productOrder));
        try {
            return em.createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    public BillableItem findByBillableItemId(Long billableItemId) {
        return findSingle(BillableItem.class, BillableItem_.billableItemId, billableItemId);
    }

}
