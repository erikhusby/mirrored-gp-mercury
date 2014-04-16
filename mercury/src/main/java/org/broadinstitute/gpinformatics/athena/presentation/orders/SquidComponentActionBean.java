package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collections;

/**
 * TODO scottmat fill in javadoc!!!
 */
@SuppressWarnings("unused")
@UrlBinding(SquidComponentActionBean.ACTIONBEAN_URL_BINDING)
public class SquidComponentActionBean extends CoreActionBean {

    private static final String BUILD_SQUID_COMPONENT_ACTION = "buildSquidComponents";
    public static final String BUILD_SQUID_COMPONENTS = "Build Squid Components";
    private static Log logger = LogFactory.getLog(SquidComponentActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/orders/squid_component.action";
    public static final String CREATE_SQUID_COMPONENT_PAGE = "/orders/createSquidComponenets.jsp";

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private UserTokenInput owner;

    private ProductOrder sourceOrder;

    private String productOrderKey;

    private final CompletionStatusFetcher progressFetcher = new CompletionStatusFetcher();

    public SquidComponentActionBean() {
        super(BUILD_SQUID_COMPONENTS, BUILD_SQUID_COMPONENTS, ProductOrderActionBean.PRODUCT_ORDER_PARAMETER);
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on={CREATE_ACTION, BUILD_SQUID_COMPONENT_ACTION})
    public void init() {
        productOrderKey = getContext().getRequest().getParameter(ProductOrderActionBean.PRODUCT_ORDER_PARAMETER);
        if (StringUtils.isNotBlank(productOrderKey)) {
            sourceOrder = productOrderDao.findByBusinessKey(productOrderKey);
            if (sourceOrder != null) {
                progressFetcher.loadProgress(productOrderDao, Collections.singletonList(sourceOrder.getProductOrderId()));
            }
        } else {
            addGlobalValidationError("Cannot create ");
        }
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {

        setSubmitString(BUILD_SQUID_COMPONENTS);
        owner.setup(userBean.getBspUser().getUserId());
        return new ForwardResolution(CREATE_SQUID_COMPONENT_PAGE);
    }


    public ProductOrder getSourceOrder() {
        return sourceOrder;
    }
}
