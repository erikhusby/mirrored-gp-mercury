package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
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

    public ProductOrder findByBusinessKey(String key) {
        return findSingle(ProductOrder.class, ProductOrder_.jiraTicketKey, key);
    }

    /**
     *
     * @param orderTitle
     * @return
     */
    public ProductOrder findByTitle(String orderTitle) {
        return findSingle(ProductOrder.class, ProductOrder_.title, orderTitle);
    }

    /**
     *
     * @return
     */
    public List<ProductOrder> findAllOrders() {
        return findAll(ProductOrder.class);
    }

    /**
     *
     * @param orderId
     * @return
     */
    public ProductOrder findById(Long orderId) {
        return findSingle(ProductOrder.class, ProductOrder_.productOrderId, orderId);
    }
}
