package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.presentation.links.BspLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/orders/order.action")
public class ProductOrderActionBean extends CoreActionBean {

    private static final String CREATE_ORDER = CoreActionBean.CREATE + "New Product Order";
    private static final String EDIT_ORDER = CoreActionBean.EDIT + "Product Order: ";

    private static final String ORDER_CREATE_PAGE = "/orders/create.jsp";
    private static final String ORDER_LIST_PAGE = "/orders/list.jsp";
    private static final String ORDER_VIEW_PAGE = "/orders/view.jsp";

    @Inject
    private ProductOrderListEntryDao orderListEntryDao;

    @Inject
    private JiraLink jiraLink;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private BspLink bspLink;

    @Inject
    private ProductOrderDao productOrderDao;

    private List<ProductOrderListEntry> allProductOrders;

    @Validate(required = true, on = {"view", "edit"})
    private String orderKey;

    ProductOrder editOrder;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view", "edit", "save", "addOnsAutocomplete"})
    public void init() {
        orderKey = getContext().getRequest().getParameter("orderKey");
        if (orderKey != null) {
            editOrder = productOrderDao.findByBusinessKey(orderKey);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allProductOrders = orderListEntryDao.findProductOrderListEntries();
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(ORDER_LIST_PAGE);
    }

    @HandlesEvent("view")
    public Resolution view() {
        return new ForwardResolution(ORDER_VIEW_PAGE);
    }

    @HandlesEvent("create")
    public Resolution create() {
        setSubmitString(CREATE_ORDER);
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    @HandlesEvent("edit")
    public Resolution edit() {
        setSubmitString(EDIT_ORDER);
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    @HandlesEvent(value = "save")
    public Resolution save() {
        try {
            productOrderDao.persist(editOrder);
        } catch (Exception e ) {
            addGlobalValidationError(e.getMessage());
            return null;
        }

        addMessage("Product Order \"" + editOrder.getTitle() + "\" has been created");
        return new RedirectResolution(ORDER_VIEW_PAGE).addParameter("order", editOrder.getBusinessKey());
    }

    public List<ProductOrderListEntry> getAllProductOrders() {
        return allProductOrders;
    }

    public String getJiraUrl() {
        return jiraLink.browseUrl();
    }

    public ProductOrder getEditOrder() {
        return editOrder;
    }

    public void setEditOrder(ProductOrder order) {
        this.editOrder = order;
    }

    public String getProductKey() {
        return orderKey;
    }

    public void setProductKey(String orderKey) {
        this.orderKey = orderKey;
    }

    public String getQuoteUrl() {
        return quoteLink.quoteUrl(editOrder.getQuoteId());
    }

    public String getEditOrderSampleSearchUrl() {
        return bspLink.sampleSearchUrl();
    }
}
