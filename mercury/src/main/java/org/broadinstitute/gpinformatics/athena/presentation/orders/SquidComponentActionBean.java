package org.broadinstitute.gpinformatics.athena.presentation.orders;

import edu.mit.broad.prodinfo.bean.generated.CreateProjectOptions;
import edu.mit.broad.prodinfo.bean.generated.CreateWorkRequestOptions;
import edu.mit.broad.prodinfo.bean.generated.ExecutionTypes;
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
import org.broadinstitute.gpinformatics.athena.boundary.orders.SquidComponentDto;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * Action bean to support the Mercury page to assist customers to initiate the creation of a Project and/or Work
 * request in Squid
 */
@SuppressWarnings("unused")
@UrlBinding(SquidComponentActionBean.ACTIONBEAN_URL_BINDING)
public class SquidComponentActionBean extends CoreActionBean {

    private static final String BUILD_SQUID_COMPONENT_ACTION = "buildSquidComponents";
    public static final String BUILD_SQUID_COMPONENTS = "Build Squid Components";
    private static Log logger = LogFactory.getLog(SquidComponentActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/orders/squid_component.action";
    public static final String CREATE_SQUID_COMPONENT_PAGE = "/orders/create_squid_components.jsp";
    public static final String SQUID_PROJECT_OPTIONS_INSERT = "/orders/squid_project_options.jsp";
    public static final String SQUID_PROJECT_EXECUTION_TYPE_INSERT = "/orders/squid_execution_types.jsp";
    public static final String SQUID_WORK_REQUEST_OPTIONS_INSERT = "/orders/squid_work_request_options.jsp";

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private SquidConnector squidConnector;

    @Inject
    private UserTokenInput owner;

    private ProductOrder sourceOrder;

    private String productOrderKey;

    private List<String> selectedProductOrderSampleIds;

    private SquidComponentDto autoSquidDto = new SquidComponentDto();

    private final CompletionStatusFetcher progressFetcher = new CompletionStatusFetcher();

    private CreateProjectOptions squidProjectOptions;
    private CreateWorkRequestOptions workRequestOptions;
    private ExecutionTypes squidProjectExecutionTypes;

    public SquidComponentActionBean() {
        super("", "", ProductOrderActionBean.PRODUCT_ORDER_PARAMETER);
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {CREATE_ACTION, BUILD_SQUID_COMPONENT_ACTION})
    public void init() {
        productOrderKey = getContext().getRequest().getParameter(ProductOrderActionBean.PRODUCT_ORDER_PARAMETER);
        if (StringUtils.isNotBlank(productOrderKey)) {
            sourceOrder = productOrderDao.findByBusinessKey(productOrderKey);
            if (sourceOrder != null) {
                progressFetcher.loadProgress(productOrderDao, Collections.singletonList(
                        sourceOrder.getProductOrderId()));
            }
        } else {
            addGlobalValidationError("Cannot create squid components without a Product Order to reference");
        }
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {

        setSubmitString(BUILD_SQUID_COMPONENTS);
        owner.setup(userBean.getBspUser().getUserId());
        return new ForwardResolution(CREATE_SQUID_COMPONENT_PAGE);
    }

    @HandlesEvent("ajaxSquidProjectOptions")
    public Resolution ajaxSquidProjectOptions() throws Exception {

        squidProjectOptions = squidConnector.getProjectCreationOptions();
        return new ForwardResolution(SQUID_PROJECT_OPTIONS_INSERT);
    }

    @HandlesEvent("ajaxSquidExecutionTypes")
    public Resolution ajaxSquidExecutionTypes() throws Exception {

        squidProjectExecutionTypes = squidConnector.getProjectExecutionTypes();
        return new ForwardResolution(SQUID_PROJECT_EXECUTION_TYPE_INSERT);
    }

    @HandlesEvent("ajaxSquidWorkRequestOptions")
    public Resolution ajaxSquidWorkRequestOptions() throws Exception {

        workRequestOptions = squidConnector.getWorkRequestOptions(autoSquidDto.getExecutionType());
        return new ForwardResolution(SQUID_WORK_REQUEST_OPTIONS_INSERT);
    }

    public ProductOrder getSourceOrder() {
        return sourceOrder;
    }

    public List<String> getSelectedProductOrderSampleIds() {
        return selectedProductOrderSampleIds;
    }

    public void setSelectedProductOrderSampleIds(List<String> selectedProductOrderSampleIds) {
        this.selectedProductOrderSampleIds = selectedProductOrderSampleIds;
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }

    public SquidComponentDto getAutoSquidDto() {
        return autoSquidDto;
    }

    public void setAutoSquidDto(SquidComponentDto autoSquidDto) {
        this.autoSquidDto = autoSquidDto;
    }

    public CreateWorkRequestOptions getWorkRequestOptions() {
        return workRequestOptions;
    }

    public CreateProjectOptions getSquidProjectOptions() {
        return squidProjectOptions;
    }

    public ExecutionTypes getSquidProjectExecutionTypes() {
        return squidProjectExecutionTypes;
    }
}
