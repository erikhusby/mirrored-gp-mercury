package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/orders/order.action")
public class ProductOrderActionBean extends CoreActionBean {

    private static final String CREATE = "Create New Product Order";
    private static final String EDIT = "Edit Product Order: ";

    private static final String ORDER_CREATE_PAGE = "/orders/create.jsp";
    private static final String ORDER_LIST_PAGE = "/orders/list.jsp";
    private static final String ORDER_VIEW_PAGE = "/orders/view.jsp";

    @Inject
    private ProductOrderListEntryDao orderListEntryDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private JiraLink jiraLink;

    private List<ProductOrderListEntry> allProductOrders;

    private Map<Long, String> fullNameMap;

    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allProductOrders = orderListEntryDao.findProductOrderListEntries();
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(ORDER_LIST_PAGE);
    }

    public void setAllProductOrders(List<ProductOrderListEntry> allProductOrders) {
        this.allProductOrders = allProductOrders;
    }

    public List<ProductOrderListEntry> getAllProductOrders() {
        return allProductOrders;
    }

    public String getJiraUrl() {
        return jiraLink.browseUrl();
    }

    public Map<Long, String> getFullNameMap() {
        if (fullNameMap == null) {
            fullNameMap = bspUserList.getFullNameMap();
        }

        return fullNameMap;
    }
}
