package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Class for displaying details about a product order.
 */
@Named
@RequestScoped
public class ProductOrderDetail extends AbstractJsfBean {
    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    BSPUserList userList;

    /** Key used to look up this product order. */
    private String productOrderKey;

    /** If non null, the key for the default research project for a new product order */
    private String researchProjectKey;

    /** The product order we're currently displaying */
    private ProductOrder productOrder;

    public void initEmpty() {
        if (productOrder == null) {
            ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
            String username = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
            BspUser user = userList.getByUsername(username);
            long userId = 0;
            if (user != null) {
                // FIXME: how to handle unknown user?? Should disallow login for user.
                userId = userList.getByUsername(username).getUserId();
            }
            productOrder = new ProductOrder(userId, researchProject);
        }
    }

    public void load() {
        if ((productOrder == null) && !StringUtils.isBlank(productOrderKey)) {
            productOrder = productOrderDao.findByBusinessKey(productOrderKey);
        }
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }

    public String getResearchProjectKey() {
        return researchProjectKey;
    }

    public void setResearchProjectKey(String researchProjectKey) {
        this.researchProjectKey = researchProjectKey;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }
}
