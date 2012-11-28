package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;


/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample}s
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/5/12
 * Time: 6:17 PM
 */
@Stateful
@RequestScoped
public class ProductOrderSampleDao extends GenericDao {

    /**
     * Find ProductOrderSamples by ProductOrder
     * @return
     */
    public List<ProductOrderSample> findByProductOrder(ProductOrder productOrder) {
        return findList(ProductOrderSample.class, ProductOrderSample_.productOrder, productOrder);
    }


//    public ProductOrderSample findByOrderAndName(@Nonnull ProductOrder productOrder, @Nonnull String sampleName) {
//        if (productOrder == null) {
//            throw new NullPointerException("Null Product Order.");
//        }
//
//        if (sampleName == null) {
//            throw new NullPointerException("Null Sample Name.");
//        }
//
//        EntityManager entityManager = getEntityManager();
//        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
//
//        CriteriaQuery<ProductOrderSample> criteriaQuery =
//                criteriaBuilder.createQuery(ProductOrderSample.class);
//
//        List<Predicate> predicateList = new ArrayList<Predicate>();
//
//        Root<ProductOrderSample> productOrderSampleRoot = criteriaQuery.from(ProductOrderSample.class);
//        predicateList.add(criteriaBuilder.equal(productOrderSampleRoot.get(ProductOrderSample_.productOrder ), productOrder));
//        predicateList.add(criteriaBuilder.equal(productOrderSampleRoot.get(ProductOrderSample_.sampleName), sampleName));
//
//        Predicate[] predicates = new Predicate[predicateList.size()];
//        criteriaQuery.where(predicateList.toArray(predicates));
//
//        try {
//            return entityManager.createQuery(criteriaQuery).getSingleResult();
//        } catch (NoResultException ignored) {
//            return null;
//        }
//    }

}
