package org.broadinstitute.gpinformatics.mercury.presentation.orders;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * This is the bean class for the composite component that represents a list of PDOs.
 */
@ManagedBean
@RequestScoped
public class ProductOrderListComponentBean implements Serializable {
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private BSPUserList bspUserList;

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = user.getUsername();
        }
        return username;
    }

    /**
     * Used to load the entity back into the session if we have lost it. This is used to avoid lazy initialization
     * exceptions.
     *
     * @param order    the order that information is being loaded from
     * @param property the property of the order we are trying to access.
     * @return the order that is now loaded into the hibernate session.
     */
    public ProductOrder safeLoad(ProductOrder order, String property) {
        if (!productOrderDao.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(order, property)) {
            order = productOrderDao.findById(order.getProductOrderId());
        }
        return order;
    }
}
