package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.BettalimsMessageUtils;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates messages against workflow definitions
 */
@Stateful
@RequestScoped
public class WorkflowValidator {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private EmailSender emailSender;

    @Inject
    private AthenaClientService athenaClientService;

    @Inject
    private AppConfig appConfig;

    /**
     * Validate workflow for all events in a message.  Starts a new transaction; without it, exceptions could cause
     * rollbacks, which would cause knock on transactions in the code to persist messages.
     * @param bettaLIMSMessage JAXB from deck
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void validateWorkflow(BettaLIMSMessage bettaLIMSMessage) {
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            validateWorkflow(plateCherryPickEvent, new ArrayList<String>(
                    BettalimsMessageUtils.getBarcodesForCherryPick(plateCherryPickEvent)));
        }

        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            validateWorkflow(plateEventType, new ArrayList<String>(
                    BettalimsMessageUtils.getBarcodesForPlateEvent(plateEventType)));
        }

        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            validateWorkflow(plateTransferEventType, new ArrayList<String>(
                    BettalimsMessageUtils.getBarcodesForPlateTransfer(plateTransferEventType)));
        }

        for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
            validateWorkflow(receptacleEventType, new ArrayList<String>(
                    BettalimsMessageUtils.getBarcodesForReceptacleEvent(receptacleEventType)));
        }

        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            validateWorkflow(receptaclePlateTransferEvent, new ArrayList<String>(
                    BettalimsMessageUtils.getBarcodesForReceptaclePlateTransfer(receptaclePlateTransferEvent)));
        }
    }

    /**
     * Validate workflow for barcodes in an event.
     * @param stationEventType JAXB from deck
     * @param barcodes plastic to be validated
     */
    private void validateWorkflow(StationEventType stationEventType, List<String> barcodes) {
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        validateWorkflow(mapBarcodeToVessel.values(), stationEventType);
    }

    /**
     * Parameter to email template.
     */
    public static class WorkflowValidationError {
        private SampleInstance sampleInstance;
        private List<ProductWorkflowDefVersion.ValidationError> errors;
        private ProductOrder productOrder;
        private AppConfig appConfig;

        public WorkflowValidationError(SampleInstance sampleInstance,
                List<ProductWorkflowDefVersion.ValidationError> errors, ProductOrder productOrder,
                AppConfig appConfig) {
            this.sampleInstance = sampleInstance;
            this.errors = errors;
            this.productOrder = productOrder;
            this.appConfig = appConfig;
        }

        public SampleInstance getSampleInstance() {
            return sampleInstance;
        }

        public List<ProductWorkflowDefVersion.ValidationError> getErrors() {
            return errors;
        }

        public ProductOrder getProductOrder() {
            return productOrder;
        }

        public String getLinkToProductOrder() {
            return appConfig.getUrl() + ProductOrderActionBean.ACTIONBEAN_URL_BINDING + "?" +
                    ProductOrderActionBean.VIEW_ACTION + "&" + ProductOrderActionBean.PRODUCT_ORDER_PARAMETER +
                    "=" + productOrder.getBusinessKey();
        }

        public String getLinkToResearchProject() {
            return appConfig.getUrl() + ResearchProjectActionBean.ACTIONBEAN_URL_BINDING + "?" +
                    ResearchProjectActionBean.VIEW_ACTION + "&" + ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER +
                    "=" + productOrder.getResearchProject().getBusinessKey();
        }
    }

    /**
     * Validate workflow for the LabVessels in an event
     * @param labVessels entities
     * @param stationEventType JAXB from deck
     */
    private void validateWorkflow(Collection<LabVessel> labVessels, StationEventType stationEventType) {

        List<WorkflowValidationError> validationErrors = validateWorkflow(labVessels, stationEventType.getEventType());

        if (!validationErrors.isEmpty()) {
            Map<String, Object> rootMap = new HashMap<String, Object>();
            String linkToPlastic = appConfig.getUrl() + SearchActionBean.ACTIONBEAN_URL_BINDING + "?" +
                    SearchActionBean.SEARCH_ACTION + "=&searchKey=" + labVessels.iterator().next().getLabel();
            rootMap.put("linkToPlastic", linkToPlastic);
            rootMap.put("stationEvent", stationEventType);
            rootMap.put("bspUser", bspUserList.getByUsername(stationEventType.getOperator()));
            rootMap.put("validationErrors", validationErrors);
            StringWriter stringWriter = new StringWriter();
            templateEngine.processTemplate("WorkflowValidation.ftl", rootMap, stringWriter);
            emailSender.sendHtmlEmail(appConfig.getWorkflowValidationEmail(), "Workflow validation failure for " +
                    stationEventType.getEventType(), stringWriter.toString());
        }
    }

    /**
     * Validate the next action for a collection of lab vessels.
     * @param labVessels entities
     * @param eventType name
     * @return list of errors
     */
    public List<WorkflowValidationError> validateWorkflow(Collection<LabVessel> labVessels, String eventType) {
        List<SampleInstance> allSampleInstances = new ArrayList<SampleInstance>();
        List<WorkflowValidationError> validationErrors = new ArrayList<WorkflowValidationError>();
        for (LabVessel labVessel : labVessels) {
            Set<SampleInstance> sampleInstances = labVessel.getSampleInstances(LabVessel.SampleType.WITH_PDO,
                    LabBatch.LabBatchType.WORKFLOW);
            allSampleInstances.addAll(sampleInstances);
            for (SampleInstance sampleInstance : sampleInstances) {
                ProductWorkflowDefVersion workflowVersion = getWorkflowVersion(sampleInstance.getProductOrderKey());
                if (workflowVersion != null) {
                    List<ProductWorkflowDefVersion.ValidationError> errors = workflowVersion.validate(labVessel, eventType);
                    if (!errors.isEmpty()) {
                        validationErrors.add(new WorkflowValidationError(sampleInstance, errors,
                                athenaClientService.retrieveProductOrderDetails(
                                        sampleInstance.getProductOrderKey()), appConfig));
                    }
                }
            }
        }
        return validationErrors;
    }

    // todo jmt copy/pasted from LabEventHandler, decide where to put it so it can be shared
    /**
     * Based on the BusinessKey of a product order, find the defined Workflow Version.  Query to the "Athena" side of
     * Mercury for the ProductOrder Definition and look up the workflow definition based on the workflow name defined
     * on the ProductOrder
     *
     * @param productOrderKey Business Key for a previously defined product order
     * @return Workflow Definition for the defined workflow for the product order represented by productOrderKey
     */
    public ProductWorkflowDefVersion getWorkflowVersion(String productOrderKey) {
        WorkflowConfig workflowConfig = new WorkflowLoader().load();
        ProductWorkflowDefVersion versionResult = null;
        ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(productOrderKey);

        if (StringUtils.isNotBlank(productOrder.getProduct().getWorkflowName())) {
            ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(
                    productOrder.getProduct().getWorkflowName());
            versionResult = productWorkflowDef.getEffectiveVersion();
        }
        return versionResult;
    }

    public void setAthenaClientService(AthenaClientService athenaClientService) {
        this.athenaClientService = athenaClientService;
    }
}
