package org.broadinstitute.gpinformatics.mercury.presentation.admin;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@UrlBinding(SAPAccessControlActionBean.ACTIONBEAN_URL_BINDING)
public class SAPAccessControlActionBean extends CoreActionBean {
    private static Log logger = LogFactory.getLog(SAPAccessControlActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/admin/sap_access_control.action";
    public static final String SAP_ACCESS_CONTROL_PAGE = "/admin/sap_access_controller.jsp";
    public static final String SET_ACCESS_ACTION = "setAccess";
    public static final String RESET_ACCESS_ACTION = "resetAccess";

    @Inject
    private SAPAccessControlEjb accessControlEjb;

    @Inject
    private PriceListCache priceListCache;

    private SAPAccessControl accessController;

    private Set<String> priceListOptions = new HashSet<>();

    private Set<String> selectedPriceItems = new HashSet<>();

    private String enabledAccess;

    @Before(stages = LifecycleStage.BindingAndValidation)
    private void initValues() {
        accessController = accessControlEjb.getCurrentControlDefinitions();
        for (String selectedFeature : accessController.getDisabledFeatures()) {
            selectedPriceItems.add(selectedFeature);
        }
        enabledAccess = accessController.getAccessStatus().name();
        for (QuotePriceItem quotePriceItem : priceListCache.getQuotePriceItems()) {
            priceListOptions.add(quotePriceItem.getName());
        }
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SAP_ACCESS_CONTROL_PAGE);
    }

    @HandlesEvent(SET_ACCESS_ACTION)
    public Resolution setAccess() {
        AccessStatus status = AccessStatus.valueOf(enabledAccess);
        accessControlEjb.setDefinitions(status, selectedPriceItems);
        return new ForwardResolution(SAP_ACCESS_CONTROL_PAGE);
    }

    @HandlesEvent(RESET_ACCESS_ACTION)
    public Resolution resetAccess() {
        accessControlEjb.setDefinitions(AccessStatus.ENABLED, new HashSet<String>());
        return new ForwardResolution(SAP_ACCESS_CONTROL_PAGE);
    }

    public SAPAccessControl getAccessController() {
        return accessController;
    }
    public Set<String> getPriceListOptions() {
        return priceListOptions;
    }

    public void setSelectedPriceItems(Set<String> selectedPriceItems) {
        this.selectedPriceItems = selectedPriceItems;
    }

    public Set<String> getSelectedPriceItems() {
        return selectedPriceItems;
    }

    public String getSelectedOptionsString() {
        return StringUtils.join(accessController.getDisabledFeatures(), ",");
    }

    public void setEnabledAccess(String enabledAccess) {
        this.enabledAccess = enabledAccess;
    }

    public String getEnabledAccess() {
        return enabledAccess;
    }
}