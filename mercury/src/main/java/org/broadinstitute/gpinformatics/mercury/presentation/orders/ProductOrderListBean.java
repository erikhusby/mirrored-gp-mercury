package org.broadinstitute.gpinformatics.mercury.presentation.orders;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;

@ManagedBean
@RequestScoped
public class ProductOrderListBean implements Serializable {
    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BSPUserList bspUserList;

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }

    public ProductOrder safeLoad(ProductOrder order, String property) {
        if (!productOrderDao.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(order, property)) {
            order = productOrderDao.findById(order.getProductOrderId());
        }
        return order;
    }
}
