package org.broadinstitute.gpinformatics.athena.presentation.orders;

import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestInput;
import edu.mit.broad.prodinfo.bean.generated.AutoWorkRequestOutput;
import edu.mit.broad.prodinfo.bean.generated.CreateProjectOptions;
import edu.mit.broad.prodinfo.bean.generated.CreateWorkRequestOptions;
import edu.mit.broad.prodinfo.bean.generated.ExecutionTypes;
import edu.mit.broad.prodinfo.bean.generated.OligioGroups;
import edu.mit.broad.prodinfo.bean.generated.SampleReceptacleGroup;
import edu.mit.broad.prodinfo.bean.generated.SampleReceptacleInfo;
import edu.mit.broad.prodinfo.bean.generated.SelectionOption;
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Action bean to support the Mercury page to assist customers to initiate the creation of a Project and/or Work
 * request in Squid
 */
@SuppressWarnings("unused")
@UrlBinding(SquidComponentActionBean.ACTIONBEAN_URL_BINDING)
public class SquidComponentActionBean extends CoreActionBean {

    private static Log logger = LogFactory.getLog(SquidComponentActionBean.class);
    public static final String BUILD_SQUID_COMPONENT_ACTION = "buildSquidComponents";

    public static final String ENTER_COMPONENTS_ACTION = "enterComponents";
    public static final String CANCEL_ACTION = "cancelComponents";

    public static final String WORKREQUEST_TYPE_FOR_BAITS = "SEQ ONLY HYB SEL";

    public static final String ACTIONBEAN_URL_BINDING = "/orders/squid_component.action";
    private static final String BUILD_SQUID_COMPONENTS = "Build Squid Components";

    private static final String CREATE_SQUID_COMPONENT_PAGE = "/orders/squidcomponent/create_squid_components.jsp";

    private static final String SQUID_PROJECT_OPTIONS_INSERT = "/orders/squidcomponent/squid_project_options.jsp";
    private static final String BAIT_OPTIONS_INSERT = "/orders/squidcomponent/squid_bait_options.jsp";
    private static final String SQUID_WORK_REQUEST_OPTIONS_INSERT = "/orders/squidcomponent/squid_work_request_options.jsp";
    private static final String BAINT_RECEPTACLES_INSERT = "/orders/squidcomponent/squid_bait_receptacles.jsp";

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private SquidConnector squidConnector;

    private ProductOrder sourceOrder;

    private String productOrderKey;

    private List<String> selectedProductOrderSampleIds;

    @ValidateNestedProperties({
            @Validate(field = "projectType", required = true,
                    label = "A project type is required", expression = "this != '-1'",
                    on = BUILD_SQUID_COMPONENT_ACTION),
            @Validate(field = "initiative", required = true, expression = "this != '-1'",
                    on = BUILD_SQUID_COMPONENT_ACTION, label = "An initiative is required"),
            @Validate(field = "fundSource", required = true, expression = "this != '-1'",
                    on = BUILD_SQUID_COMPONENT_ACTION, label = "A funding source is required"),
            @Validate(field = "executionType", required = true, expression = "this != '-1'",
                    on = BUILD_SQUID_COMPONENT_ACTION, label = "A project execution type is required"),
            @Validate(field = "workRequestType", required = true, expression = "this != '-1'",
                    on = BUILD_SQUID_COMPONENT_ACTION, label = "A work request type is required"),
            @Validate(field = "analysisType", required = true, expression = "this != '-1'",
                    on = BUILD_SQUID_COMPONENT_ACTION, label = "An analysis type is required"),
            @Validate(field = "referenceSequence", required = true, expression = "this != '-1'",
                    on = BUILD_SQUID_COMPONENT_ACTION, label = "A reference sequence is required")
    })
    private AutoWorkRequestInput autoSquidDto = new AutoWorkRequestInput();

    @Validate(required = true, on = BUILD_SQUID_COMPONENT_ACTION, label = "A value for paired sequencing is required")
    private String pairedSequence;
    private final CompletionStatusFetcher progressFetcher = new CompletionStatusFetcher();

    private CreateProjectOptions squidProjectOptions;
    private CreateWorkRequestOptions workRequestOptions;
    private ExecutionTypes squidProjectExecutionTypes;

    private Map<String, Set<SampleReceptacleInfo>> baitsByGroupName = new HashMap<>();
    private SampleReceptacleGroup selectedBaits;
    private String[] selectedBaitReceptacles;

    public SquidComponentActionBean() {
        super("", "", ProductOrderActionBean.PRODUCT_ORDER_PARAMETER);
    }

    @HandlesEvent(CANCEL_ACTION)
    public Resolution cancelComponents() {
        return new ForwardResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(
                ProductOrderActionBean.PRODUCT_ORDER_PARAMETER, productOrderKey);
    }

    @Before(stages = LifecycleStage.BindingAndValidation,
            on = {ENTER_COMPONENTS_ACTION, BUILD_SQUID_COMPONENT_ACTION,CANCEL_ACTION})
    public void init() {

        productOrderKey = getContext().getRequest().getParameter(ProductOrderActionBean.PRODUCT_ORDER_PARAMETER);
        if (StringUtils.isNotBlank(productOrderKey)) {
            sourceOrder = productOrderDao.findByBusinessKey(productOrderKey);
            if (sourceOrder != null) {
                progressFetcher.loadProgress(productOrderDao, Collections.singletonList(
                        sourceOrder.getProductOrderId()));
            }
            autoSquidDto.setProductOrderKey(productOrderKey);

            for (ProductOrderSample sample : sourceOrder.getSamples()) {
                SelectionOption sampleDto = new SelectionOption();
                sampleDto.setId(sample.getSampleKey());
                sampleDto.setName(sample.getSampleKey());
                autoSquidDto.getSeqContent().add(sampleDto);
            }
            autoSquidDto.setQuote(sourceOrder.getQuoteId());
            autoSquidDto.setUserName(getUserBean().getBspUser().getUsername());
        } else {
            addGlobalValidationError("Cannot create squid components without a Product Order to reference");
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {BUILD_SQUID_COMPONENT_ACTION})
    public void postInit() {
        squidProjectOptions = squidConnector.getProjectCreationOptions();
        workRequestOptions = squidConnector.getWorkRequestOptions(autoSquidDto.getExecutionType());
    }

    @HandlesEvent(ENTER_COMPONENTS_ACTION)
    public Resolution enterComponents() {

        setSubmitString(BUILD_SQUID_COMPONENTS);
        return new ForwardResolution(CREATE_SQUID_COMPONENT_PAGE);
    }

    @HandlesEvent(BUILD_SQUID_COMPONENT_ACTION)
    public Resolution buildSquidComponents() {

        autoSquidDto.setPairedSequencing((pairedSequence.equals("YES")));

        if(selectedBaits != null) {
            for(String receptacle:selectedBaitReceptacles) {
                SampleReceptacleInfo selectedInfo = new SampleReceptacleInfo();
                selectedInfo.setBarcode(receptacle);
                selectedInfo.setReceptacleId(receptacle);
                selectedBaits.getGroupReceptacles().add(selectedInfo);
            }

            autoSquidDto.setBaits(selectedBaits);
        }

        AutoWorkRequestOutput output = productOrderEjb.createSquidWorkRequest(productOrderKey, autoSquidDto);

        addMessage("A project with an ID of {0} and a work request with an ID of {1} has been created in Squid for " +
                   "this product order", output.getProjectId(), output.getWorkRequestId());

        return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION)
                .addParameter(ProductOrderActionBean.PRODUCT_ORDER_PARAMETER, productOrderKey);
    }


    @HandlesEvent("ajaxSquidProjectOptions")
    public Resolution ajaxSquidProjectOptions() throws Exception {

        squidProjectOptions = squidConnector.getProjectCreationOptions();
        return new ForwardResolution(SQUID_PROJECT_OPTIONS_INSERT);
    }

    @HandlesEvent("ajaxSquidWorkRequestOptions")
    public Resolution ajaxSquidWorkRequestOptions() throws Exception {

        workRequestOptions = squidConnector.getWorkRequestOptions(autoSquidDto.getExecutionType());
        return new ForwardResolution(SQUID_WORK_REQUEST_OPTIONS_INSERT);
    }

    @HandlesEvent("ajaxSquidBaitOptions")
    public Resolution ajaxSquidBaitOptions() throws Exception {
        OligioGroups oligioGroups = squidConnector.getOligioGroups();

        for(SampleReceptacleGroup baitDetails:oligioGroups.getGroups()) {
                baitsByGroupName.put(baitDetails.getGroupName(),
                        new HashSet<>(baitDetails.getGroupReceptacles()));
        }

        return  new ForwardResolution(BAIT_OPTIONS_INSERT);
    }

    @HandlesEvent("ajaxSquidBaitReceptacles")
    public Resolution ajaxSquidBaitReceptacles() throws Exception {

        SampleReceptacleGroup groupReceptacles = squidConnector.getGroupReceptacles(selectedBaits.getGroupName());

        selectedBaits.getGroupReceptacles().addAll(groupReceptacles.getGroupReceptacles());

        return new ForwardResolution(BAINT_RECEPTACLES_INSERT);

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

    public AutoWorkRequestInput getAutoSquidDto() {
        return autoSquidDto;
    }

    public void setAutoSquidDto(AutoWorkRequestInput autoSquidDto) {
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

    public String getPairedSequence() {
        return pairedSequence;
    }

    public void setPairedSequence(String pairedSequence) {
        this.pairedSequence = pairedSequence;
    }

    public boolean isProjectOptionsRetrieved() {
        return squidProjectOptions != null && (!squidProjectOptions.getFundingSources().isEmpty() ||
                                               !squidProjectOptions.getInitiatives().isEmpty() ||
                                               !squidProjectOptions.getProjectTypes().isEmpty());
    }

    public boolean isWorkRequestOptionsRetrieved() {
        return workRequestOptions != null && (!workRequestOptions.getAnalysisTypes().isEmpty() ||
                                              !workRequestOptions.getReferenceSequences().isEmpty() ||
                                              !workRequestOptions.getWorkRequestTypes().isEmpty());
    }

    public Set<String> getBaitGroupNames() {
        return baitsByGroupName.keySet();
    }

    public SampleReceptacleGroup getSelectedBaits() {
        return selectedBaits;
    }

    public void setSelectedBaitReceptacles(String[] selectedBaitReceptacles) {
        this.selectedBaitReceptacles = selectedBaitReceptacles;
    }

    public void setSelectedBaits(SampleReceptacleGroup selectedBaits) {
        this.selectedBaits = selectedBaits;
    }
}
