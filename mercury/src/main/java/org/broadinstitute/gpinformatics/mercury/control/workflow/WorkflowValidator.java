package org.broadinstitute.gpinformatics.mercury.control.workflow;

import clover.org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
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
import org.broadinstitute.gpinformatics.mercury.control.labevent.BettaLimsMessageUtils;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates messages against workflow definitions
 */
@SuppressWarnings("FeatureEnvy")
@Stateful
@RequestScoped
public class WorkflowValidator {

    private static final Log log = LogFactory.getLog(WorkflowValidator.class);
    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private TemplateEngine templateEngine;

    @Inject
    private EmailSender emailSender;

    private WorkflowConfig workflowConfig;

    private ProductOrderDao productOrderDao;

    @Inject
    private AppConfig appConfig;

    public static class WorkflowException extends Exception {
        public WorkflowException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Validate workflow for all events in a message.  Starts a new transaction; without it, exceptions could cause
     * rollbacks, which would cause knock on transactions in the code to persist messages.
     *
     * @param bettaLIMSMessage JAXB from deck
     */
    public void validateWorkflow(BettaLIMSMessage bettaLIMSMessage) throws WorkflowException {
        try {
            for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
                validateWorkflow(plateCherryPickEvent, new ArrayList<>(
                        BettaLimsMessageUtils.getBarcodesForCherryPick(plateCherryPickEvent)));
            }

            if (bettaLIMSMessage.getPlateEvent().size() > 0) {
                validateWorkflow(BettaLimsMessageUtils.getBarcodesForPlateEvent(bettaLIMSMessage.getPlateEvent()));
            }

            for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
                validateWorkflow(plateTransferEventType, new ArrayList<>(
                        BettaLimsMessageUtils.getBarcodesForPlateTransfer(plateTransferEventType)));
            }

            for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
                validateWorkflow(receptacleEventType, new ArrayList<>(
                        BettaLimsMessageUtils.getBarcodesForReceptacleEvent(receptacleEventType)));
            }

            for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : bettaLIMSMessage
                    .getReceptaclePlateTransferEvent()) {
                validateWorkflow(receptaclePlateTransferEvent, new ArrayList<>(
                        BettaLimsMessageUtils.getBarcodesForReceptaclePlateTransfer(receptaclePlateTransferEvent)));
            }
        } catch (RuntimeException e) {
            // convert runtime exceptions to checked, so the transaction is not rolled back
            log.error("Workflow exception", e);
            throw new WorkflowException(e);
        }
    }

    private void validateWorkflow(String barcode, Set<StationEventType> stationEventTypes) {
        LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
        validateWorkflow(labVessel, stationEventTypes);
    }

    private void validateWorkflow(LabVessel labVessel, Set<StationEventType> eventTypes) {
        Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();
        Set<String> eventNames = new LinkedHashSet<>();
        for (StationEventType stationEventType: eventTypes) {
            eventNames.add(stationEventType.getEventType());
        }
        List<WorkflowValidationError> validationErrors = validateSampleInstances(
                labVessel, sampleInstances, eventNames);

        if (!validationErrors.isEmpty()) {
            String eventsString = StringUtils.join(eventNames, ",");
            String operator = eventTypes.iterator().next().getOperator();
            String body = renderTemplate(labVessel.getLabel(), eventsString, operator, validationErrors);
            emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                    "Workflow validation failure for " + eventsString, body, false);
        }
    }

    private List<WorkflowValidationError> validateSampleInstances(LabVessel labVessel,
                                                                  Set<SampleInstanceV2> sampleInstances,
                                                                  Set<String> eventNames) {
        List<WorkflowValidationError> validationErrors = new ArrayList<>();
        for (SampleInstanceV2 sampleInstance : sampleInstances) {
            String workflowName = sampleInstance.getWorkflowName();
            LabBatch effectiveBatch = sampleInstance.getSingleBatch();

                /*
                    Not necessarily an ideal solution but validation should not necessarily cause a Null pointer which
                    is what will happen if the batch is not found (Sample exists in multiple batches)
                 */
            if (workflowName != null && effectiveBatch != null) {
                ProductWorkflowDefVersion workflowVersion = workflowConfig.getWorkflowVersionByName(
                        workflowName, effectiveBatch.getCreatedOn());
                if (workflowVersion != null) {
                    List<ProductWorkflowDefVersion.ValidationError> errors =
                            workflowVersion.validate(labVessel, eventNames);
                    if (!errors.isEmpty()) {
                        ProductOrder productOrder = null;
                        if (sampleInstance.getSingleBucketEntry() != null) {
                            productOrder = sampleInstance.getSingleBucketEntry().getProductOrder();
                        }
                        //if this is a rework and we are at the first step of the process it was reworked from ignore the error
                        boolean ignoreErrors = false;
                        for (LabBatch batch : labVessel.getReworkLabBatches()) {
                            if (batch.getReworks().contains(labVessel)) {
                                ignoreErrors = true;
                            }
                        }
                        if (!ignoreErrors) {
                            validationErrors.add(new WorkflowValidationError(sampleInstance, errors, productOrder,
                                    appConfig));
                        }

                    }
                }
            }
            /*
                    TO re-evaluate the usage later.
             */
/*
                else {
                    List<ProductWorkflowDefVersion.ValidationError> errors =
                            Collections.singletonList(new ProductWorkflowDefVersion.ValidationError(
                                    "Either the lab batch is missing or the Workflow is missing"));
                    validationErrors.add(new WorkflowValidationError(sampleInstance, errors,
                            athenaClientService.retrieveProductOrderDetails(
                                    sampleInstance.getProductOrderKey()), appConfig));
                }
*/
        }

        return validationErrors;
    }

    /**
     * Validate workflow for barcodes in an event.
     *
     * @param stationEventType JAXB from deck
     * @param barcodes         plastic to be validated
     */
    private void validateWorkflow(StationEventType stationEventType, List<String> barcodes) {
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        validateWorkflow(mapBarcodeToVessel.values(), stationEventType);
    }

    /**
     * Validate workflow for barcodes in an event.
     *
     * @param barcodeToEventTypes plastic to set of associated JAXB events from the deck
     */
    private void validateWorkflow(Map<String, Set<StationEventType>> barcodeToEventTypes) {
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(
                new ArrayList<>(barcodeToEventTypes.keySet()));
        for (Map.Entry<String, LabVessel> entry: mapBarcodeToVessel.entrySet()) {
            if (entry.getValue() != null) {
                validateWorkflow(entry.getValue(), barcodeToEventTypes.get(entry.getKey()));
            }
        }
    }

    /**
     * Parameter to email template.
     */
    public static class WorkflowValidationError {
        private final SampleInstanceV2 sampleInstance;
        private final List<ProductWorkflowDefVersion.ValidationError> errors;
        private final ProductOrder productOrder;
        private final AppConfig appConfig;

        public WorkflowValidationError(SampleInstanceV2 sampleInstance,
                                       List<ProductWorkflowDefVersion.ValidationError> errors,
                                       ProductOrder productOrder,
                                       AppConfig appConfig) {
            this.sampleInstance = sampleInstance;
            this.errors = errors;
            this.productOrder = productOrder;
            this.appConfig = appConfig;
        }

        public SampleInstanceV2 getSampleInstance() {
            return sampleInstance;
        }

        public List<ProductWorkflowDefVersion.ValidationError> getErrors() {
            return errors;
        }

        public ProductOrder getProductOrder() {
            return productOrder;
        }

        public String getLinkToProductOrder() {
            if (productOrder == null) {
                return "";
            } else {
                return appConfig.getUrl() + ProductOrderActionBean.ACTIONBEAN_URL_BINDING + "?" +
                       CoreActionBean.VIEW_ACTION + "&" + ProductOrderActionBean.PRODUCT_ORDER_PARAMETER +
                       "=" + productOrder.getBusinessKey();
            }
        }

        public String getLinkToResearchProject() {
            if (productOrder == null) {
                return "";
            } else {
                return appConfig.getUrl() + ResearchProjectActionBean.ACTIONBEAN_URL_BINDING + "?" +
                       CoreActionBean.VIEW_ACTION + "&" + ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER +
                       "=" + productOrder.getResearchProject().getBusinessKey();
            }
        }
    }

    /**
     * Validate workflow for the LabVessels in an event
     *
     * @param labVessels       entities
     * @param stationEventType JAXB from deck
     */
    private void validateWorkflow(Collection<LabVessel> labVessels, StationEventType stationEventType) {

        List<WorkflowValidationError> validationErrors = validateWorkflow(labVessels, stationEventType.getEventType());

        if (!validationErrors.isEmpty()) {
            String body = renderTemplate(labVessels, stationEventType, validationErrors);
            emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                    "Workflow validation failure for " + stationEventType.getEventType(), body, false);
        }
    }

    /**
     * Uses a template to render validation errors into an HTML email body.
     */
    public String renderTemplate(Collection<LabVessel> labVessels, StationEventType stationEventType,
            List<WorkflowValidationError> validationErrors) {
        Map<String, Object> rootMap = new HashMap<>();
        String linkToPlastic = appConfig.getUrl() + VesselSearchActionBean.ACTIONBEAN_URL_BINDING + "?" +
                               VesselSearchActionBean.VESSEL_SEARCH + "=&searchKey=" + labVessels.iterator().next()
                                                                                                 .getLabel();
        rootMap.put("linkToPlastic", linkToPlastic);
        rootMap.put("stationEvent", stationEventType);
        rootMap.put("bspUser", bspUserList.getByUsername(stationEventType.getOperator()));
        rootMap.put("validationErrors", validationErrors);
        StringWriter stringWriter = new StringWriter();
        templateEngine.processTemplate("WorkflowValidation.ftl", rootMap, stringWriter);
        return stringWriter.toString();
    }

    /**
     * Uses a template to render validation errors into an HTML email body.
     */
    public String renderTemplate(String vesselLabel, String eventTypes, String operator,
                                 List<WorkflowValidationError> validationErrors) {
        Map<String, Object> rootMap = new HashMap<>();
        String linkToPlastic = appConfig.getUrl() + VesselSearchActionBean.ACTIONBEAN_URL_BINDING + "?" +
                               VesselSearchActionBean.VESSEL_SEARCH + "=&searchKey=" + vesselLabel;
        rootMap.put("linkToPlastic", linkToPlastic);
        rootMap.put("stationEvent", eventTypes);
        rootMap.put("bspUser", bspUserList.getByUsername(operator));
        rootMap.put("validationErrors", validationErrors);
        StringWriter stringWriter = new StringWriter();
        templateEngine.processTemplate("WorkflowValidation.ftl", rootMap, stringWriter);
        return stringWriter.toString();
    }

    /**
     * Validate the next action for a collection of lab vessels.
     *
     * @param labVessels entities
     * @param eventType  name
     *
     * @return list of errors
     */
    public List<WorkflowValidationError> validateWorkflow(Collection<LabVessel> labVessels, String eventType) {
        List<WorkflowValidationError> validationErrors = new ArrayList<>();

        for (LabVessel labVessel : labVessels) { // todo jmt can this be null?
            Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();
            validationErrors.addAll(
                    validateSampleInstances(labVessel, sampleInstances, Collections.singleton(eventType)));
        }
        return validationErrors;
    }

    @Inject
    public void setProductOrderDao(ProductOrderDao productOrderDao) {
        this.productOrderDao = productOrderDao;
    }

    @Inject
    public void setWorkflowConfig(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
    }

    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }
}
