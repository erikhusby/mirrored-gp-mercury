package org.broadinstitute.gpinformatics.athena.presentation.billing;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/billing/session.action")
public class BillingSessionActionBean extends CoreActionBean {

    private static final String SESSION_LIST_PAGE = "/billing/sessions.jsp";
    private static final String SESSION_VIEW_PAGE = "/billing/view.jsp";

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private PriceListCache priceListCache;

    private List<BillingSession> billingSessions;

    private String sessionKey;

    private BillingSession editSession;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view"})
    public void init() {
        sessionKey = getContext().getRequest().getParameter("sessionKey");
        if (sessionKey != null) {
            editSession = billingSessionDao.findByBusinessKey(sessionKey);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        billingSessions = billingSessionDao.findAll();
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent("view")
    public Resolution view() {
        return new ForwardResolution(SESSION_VIEW_PAGE);
    }

    public List<BillingSession> getBillingSessions() {
        return billingSessions;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public BillingSession getEditSession() {
        return editSession;
    }

    public void setEditSession(BillingSession editSession) {
        this.editSession = editSession;
    }

    private final Map<String, String> priceItemNameMap = new HashMap<String, String>();
    public Map<String, String> getQuotePriceItemNameMap() {
        Collection<PriceItem> priceItems = priceListCache.getPriceItems ();
        if (priceItemNameMap.isEmpty() && !priceItems.isEmpty()) {
            for (PriceItem priceItem : priceListCache.getPriceItems()) {
                priceItemNameMap.put(priceItem.getId(), priceItem.getName());
            }
        }

        return priceItemNameMap;
    }
}
