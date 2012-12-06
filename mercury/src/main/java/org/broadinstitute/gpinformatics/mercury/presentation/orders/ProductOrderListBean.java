package org.broadinstitute.gpinformatics.mercury.presentation.orders;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@ViewScoped
public class ProductOrderListBean implements Serializable {
    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BSPUserList bspUserList;

    private List<ProductOrder> pdos;

    public void updatePDOs(List<ProductOrder> pdos) {
        this.pdos = new ArrayList<ProductOrder>();
        for (ProductOrder pdo : pdos) {
            this.pdos.add(productOrderDao.findById(pdo.getProductOrderId()));
        }
    }

    public List<ProductOrder> getPdos() {
        return pdos;
    }

    public void setPdos(List<ProductOrder> pdos) {
        this.pdos = pdos;
    }

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }
}
