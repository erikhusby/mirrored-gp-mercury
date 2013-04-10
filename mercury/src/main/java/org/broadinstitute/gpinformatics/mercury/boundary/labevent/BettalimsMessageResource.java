package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStore;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.*;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * Allows BettaLIMS messages to be submitted through JAX-RS.  In this context, BettaLIMS refers to the message format,
 * defined by bettalims.xsd.  There is a BettaLIMS server that is part of the Squid suite of applications.
 */
@Path("/bettalimsmessage")
@Stateful
@RequestScoped
public class BettalimsMessageResource {

    /**
     * workflow error message from Squid
     */
    private static final String WORKFLOW_MESSAGE = " error(s) processing workflows for ";

    private static final Log LOG = LogFactory.getLog(BettalimsMessageResource.class);
    private static final boolean VALIDATE_SCHEMA = false;

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private LabEventHandler labEventHandler;

    @Inject
    private WsMessageStore wsMessageStore;

    @Inject
    private BettalimsConnector bettalimsConnector;

    @Inject
    private ThriftService thriftService;

    @Inject
    private MercuryOrSquidRouter mercuryOrSquidRouter;

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
     * Accepts a message from (typically) a liquid handling deck.  We unmarshal ourselves, rather than letting JAX-RS
     * do it, because we need to write the text to the file system.
     *
     * @param message the text of the message
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML})
    public Response processMessage(String message) throws ResourceException {
        try {
            storeAndProcess(message);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains(WORKFLOW_MESSAGE)) {
                throw new ResourceException(e.getMessage(), Response.Status.CREATED, e);
            } else {
                throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
            }
        }
        // The VWorks client seems to prefer 200 to 204
        return Response.status(Response.Status.OK).entity("Message persisted").type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    /**
     * Shared between JAX-RS and JMS.
     * Transaction is REQUIRED because this method is called by an MDB with transaction NOT_SUPPORTED.
     *
     * @param message from deck
     *
     * @throws Exception
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public void storeAndProcess(String message) throws Exception {
        Date now = new Date();
        //noinspection OverlyBroadCatchBlock
        try {
            wsMessageStore.store(WsMessageStore.BETTALIMS_RESOURCE_TYPE, message, now);

            BettaLIMSMessage bettaLIMSMessage = unmarshal(message);

            boolean processInMercury = false;
            boolean processInSquid = false;
            if (bettaLIMSMessage.getMode() != null && bettaLIMSMessage.getMode().equals(LabEventFactory.MODE_MERCURY)) {
                // Don't route Mercury test messages to BettalIMS/Squid
                processInMercury = true;
                processInSquid = false;
            } else {
                LabEventType labEventType = getLabEventType(bettaLIMSMessage);
                if (labEventType == null) {
                    throw new RuntimeException("Failed to find event type");
                }
                switch (labEventType.getSystemOfRecord()) {
                    case MERCURY:
                        processInMercury = true;
                        processInSquid = false;
                        break;
                    case SQUID:
                        processInMercury = false;
                        processInSquid = true;
                        break;
                    case PRODUCT_DEPENDENT:

                        Collection<String> barcodesToBeVerified = getRegisteredBarcodesFromMessage(bettaLIMSMessage);

                        // todo jmt revisit performance of this - fetch all barcodes at once, and fetch unique PDOs from Athena
                        for (String testEvent : barcodesToBeVerified) {
                            if (MercuryOrSquidRouter.MercuryOrSquid.MERCURY == mercuryOrSquidRouter
                                                                                           .routeForVessel(testEvent)) {
                                processInMercury = true;
                            } else {
                                processInSquid = true;
                            }
                        }
                        if (processInMercury && processInSquid) {
                            throw new InformaticsServiceException(
                                    "For Product Dependent processing, we cannot process in both Mercury and Squid");
                        }
                        break;
                    case BOTH:
                        processInMercury = true;
                        processInSquid = true;
                        break;
                    default:
                        throw new RuntimeException("Unexpected enum value " + labEventType.getSystemOfRecord());
                }
            }

            BettalimsConnector.BettalimsResponse bettalimsResponse = null;
            if (processInSquid) {
                bettalimsResponse = bettalimsConnector.sendMessage(message);
            }
            if (processInMercury) {
                processMessage(bettaLIMSMessage);
            }
            if (bettalimsResponse != null && bettalimsResponse.getCode() != 200) {
                throw new RuntimeException(bettalimsResponse.getMessage());
            }
        } catch (Exception e) {
            wsMessageStore.recordError(WsMessageStore.BETTALIMS_RESOURCE_TYPE, message, now, e);
            LOG.error("Failed to process run", e);
            //            notifySupport(e);
            throw e;
        }
    }

    // todo jmt move workflow validation to a Control class, this Boundary class is getting too big

    /**
     * Validate workflow for all events in a message.
     * @param bettaLIMSMessage JAXB from deck
     */
    private void validateWorkflow(BettaLIMSMessage bettaLIMSMessage) {
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            validateWorkflow(plateCherryPickEvent, new ArrayList<String>(getBarcodesForCherryPick(plateCherryPickEvent)));
        }

        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            validateWorkflow(plateEventType, new ArrayList<String>(getBarcodesForPlateEvent(plateEventType)));
        }

        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            validateWorkflow(plateTransferEventType, new ArrayList<String>(getBarcodesForPlateTransfer(plateTransferEventType)));
        }

        for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
            validateWorkflow(receptacleEventType, new ArrayList<String>(getBarcodesForReceptacleEvent(receptacleEventType)));
        }

        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            validateWorkflow(receptaclePlateTransferEvent,
                    new ArrayList<String>(getBarcodesForReceptaclePlateTransfer(receptaclePlateTransferEvent)));
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
            Set<SampleInstance> sampleInstances = labVessel.getSampleInstances();
            allSampleInstances.addAll(sampleInstances);
            for (SampleInstance sampleInstance : sampleInstances) {
                ProductWorkflowDefVersion workflowVersion = getWorkflowVersion(sampleInstance.getStartingSample().getProductOrderKey());
                if (workflowVersion != null) {
                    List<ProductWorkflowDefVersion.ValidationError> errors = workflowVersion.validate(labVessel, eventType);
                    if (!errors.isEmpty()) {
                        validationErrors.add(new WorkflowValidationError(sampleInstance, errors,
                                athenaClientService.retrieveProductOrderDetails(
                                        sampleInstance.getStartingSample().getProductOrderKey()), appConfig));
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

    /**
     * Convert from String to object
     *
     * @param message from deck
     *
     * @return JAXB bean
     *
     * @throws JAXBException
     * @throws SAXException
     */
    BettaLIMSMessage unmarshal(String message) throws JAXBException, SAXException {

        JAXBContext jc = JAXBContext.newInstance(BettaLIMSMessage.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        if (VALIDATE_SCHEMA) {
            // todo jmt move so done only once
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(new File("e:/java/mercury/mercury/src/main/xsd/bettalims.xsd"));
            unmarshaller.setSchema(schema);
            unmarshaller.setEventHandler(new ValidationEventHandler() {
                @Override
                public boolean handleEvent(ValidationEvent event) {
                    throw new RuntimeException("XSD Validation failure: "+
                            "SEVERITY: " + event.getSeverity() + "MESSAGE: " + event.getMessage() +
                            "LINKED EXCEPTION:  " + event.getLinkedException() +
                            "LINE NUMBER:  " + event.getLocator().getLineNumber() +
                            "COLUMN NUMBER:  " + event.getLocator().getColumnNumber() +
                            "OFFSET:  " + event.getLocator().getOffset() +
                            "OBJECT:  " + event.getLocator().getObject() +
                            "NODE:  " + event.getLocator().getNode() +
                            "URL:  " + event.getLocator().getURL());
                }
            });
        }

        //Create an XMLReader to use with our filter
        XMLReader reader = XMLReaderFactory.createXMLReader();

        //Create the filter (to remove namespace) and set the xmlReader as its parent.
        NamespaceFilter inFilter = new NamespaceFilter(null, false);
        inFilter.setParent(reader);

        //Prepare the input
        InputSource is = new InputSource(new StringReader(message));

        //Create a SAXSource specifying the filter
        SAXSource source = new SAXSource(inFilter, is);

        return (BettaLIMSMessage) unmarshaller.unmarshal(source);
    }

    /**
     * Find the first event type in the message
     *
     * @param bettaLIMSMessage from deck
     *
     * @return enum value, or null if not found
     */
    private LabEventType getLabEventType(BettaLIMSMessage bettaLIMSMessage) {
        LabEventType labEventType = null;
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            labEventType = LabEventType.getByName(plateCherryPickEvent.getEventType());
            if (labEventType != null) {
                break;
            }
        }
        if (labEventType == null) {
            for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
                labEventType = LabEventType.getByName(plateEventType.getEventType());
                if (labEventType != null) {
                    break;
                }
            }
        }
        if (labEventType == null) {
            for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
                labEventType = LabEventType.getByName(plateTransferEventType.getEventType());
                if (labEventType != null) {
                    break;
                }
            }
        }
        if (labEventType == null) {
            for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : bettaLIMSMessage
                                                                                     .getReceptaclePlateTransferEvent()) {
                labEventType = LabEventType.getByName(receptaclePlateTransferEvent.getEventType());
                if (labEventType != null) {
                    break;
                }
            }
        }
        if (labEventType == null) {
            for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
                labEventType = LabEventType.getByName(receptacleEventType.getEventType());
                if (labEventType != null) {
                    break;
                }
            }
        }
        return labEventType;
    }

    /**
     * Builds a collection of {@link LabVessel} barcodes from a JAXB message bean that contains one or more event
     * beans.
     * Since this method is used to validate the system of record for existing vessels, the vessels returned will be
     * Source vessles, In place vessels, and some target vessels If the setting for the associated
     * {@link LabEventType#expectExistingTarget} determines it is possible.
     *
     * @param bettaLIMSMessage JAXB bean
     *
     * @return list of barcodes
     */
    private Collection<String> getRegisteredBarcodesFromMessage(BettaLIMSMessage bettaLIMSMessage) {

        Set<String> barcodes = new HashSet<String>();

        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            barcodes.addAll(getBarcodesForCherryPick(plateCherryPickEvent));
        }
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            barcodes.addAll(getBarcodesForPlateEvent(plateEventType));
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            barcodes.addAll(getBarcodesForPlateTransfer(plateTransferEventType));
        }
        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent :
                bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            barcodes.addAll(getBarcodesForReceptaclePlateTransfer(receptaclePlateTransferEvent));
        }
        for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
            barcodes.addAll(getBarcodesForReceptacleEvent(receptacleEventType));
        }

        return barcodes;
    }

    private Set<String> getBarcodesForCherryPick(PlateCherryPickEvent plateCherryPickEvent) {
        Set<String> barcodes = new HashSet<String>();
        switch (LabEventType.getByName(plateCherryPickEvent.getEventType()).getPlasticToValidate()) {
            case SOURCE:
                for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
                    for (ReceptacleType receptacle : positionMapType.getReceptacle()) {
                        barcodes.add(receptacle.getBarcode());
                    }
                }
                break;
        }
        return barcodes;
    }

    private Set<String> getBarcodesForPlateEvent(PlateEventType plateEventType) {
        Set<String> barcodes = new HashSet<String>();
        switch (LabEventType.getByName(plateEventType.getEventType()).getPlasticToValidate()) {
            case SOURCE:
                if (plateEventType.getPositionMap() == null) {
                    barcodes.add(plateEventType.getPlate().getBarcode());
                } else {
                    for (ReceptacleType position : plateEventType.getPositionMap().getReceptacle()) {
                        barcodes.add(position.getBarcode());
                    }
                }
                break;
        }
        return barcodes;
    }

    private Set<String> getBarcodesForPlateTransfer(PlateTransferEventType plateTransferEventType) {
        Set<String> barcodes = new HashSet<String>();
        switch (LabEventType.getByName(plateTransferEventType.getEventType()).getPlasticToValidate()) {
            case SOURCE:
                if (plateTransferEventType.getSourcePositionMap() == null) {
                    barcodes.add(plateTransferEventType.getSourcePlate().getBarcode());
                } else {
                    for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap()
                            .getReceptacle()) {
                        barcodes.add(receptacleType.getBarcode());
                    }
                }
                break;
            case TARGET:

                if (plateTransferEventType.getPositionMap() == null) {
                    barcodes.add(plateTransferEventType.getPlate().getBarcode());
                } else {
                    for (ReceptacleType targetReceptacle : plateTransferEventType.getPositionMap()
                            .getReceptacle()) {
                        barcodes.add(targetReceptacle.getBarcode());
                    }
                }
                break;
        }
        return barcodes;
    }

    private Set<String> getBarcodesForReceptaclePlateTransfer(ReceptaclePlateTransferEvent receptaclePlateTransferEvent) {
        Set<String> barcodes = new HashSet<String>();
        switch (LabEventType.getByName(receptaclePlateTransferEvent.getEventType()).getPlasticToValidate()) {
            case SOURCE:
                barcodes.add(receptaclePlateTransferEvent.getSourceReceptacle().getBarcode());
                break;

            case TARGET:
                barcodes.add(receptaclePlateTransferEvent.getDestinationPlate().getBarcode());
                break;
        }
        return barcodes;
    }

    private Set<String> getBarcodesForReceptacleEvent(ReceptacleEventType receptacleEventType) {
        Set<String> barcodes = new HashSet<String>();
        switch (LabEventType.getByName(receptacleEventType.getEventType()).getPlasticToValidate()) {
            case SOURCE:
                barcodes.add(receptacleEventType.getReceptacle().getBarcode());
                break;
        }
        return barcodes;
    }

    /**
     * Process JAXB message bean after it is converted from string
     *
     * @param message JAXB
     */
    public void processMessage(BettaLIMSMessage message) {
        try {
            validateWorkflow(message);
        } catch (Exception e) {
            LOG.error("Failed to validate workflow", e);
            // Don't rethrow, workflow must not stop persistence of the message
        }
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        for (LabEvent labEvent : labEvents) {
            labEventHandler.processEvent(labEvent);
        }
    }

    /**
     * Allows documents that don't include a namespace
     */
    private static class NamespaceFilter extends XMLFilterImpl {

        private String  usedNamespaceUri;
        private boolean addNamespace;

        private boolean addedNamespace;

        NamespaceFilter(String namespaceUri, boolean addNamespace) {
            if (addNamespace) {
                usedNamespaceUri = namespaceUri;
            } else {
                usedNamespaceUri = "";
            }
            this.addNamespace = addNamespace;
        }

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            if (addNamespace) {
                startControlledPrefixMapping();
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            super.startElement(usedNamespaceUri, localName, qName, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(usedNamespaceUri, localName, qName);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (addNamespace) {
                startControlledPrefixMapping();
            } else {
                //Remove the namespace, i.e. don't call startPrefixMapping for parent
            }
        }

        private void startControlledPrefixMapping() throws SAXException {
            if (addNamespace && !addedNamespace) {
                //We should add namespace since it is set and has not yet been done.
                super.startPrefixMapping("", usedNamespaceUri);

                //Make sure we dont do it twice
                addedNamespace = true;
            }
        }
    }

    public void setBettalimsConnector(BettalimsConnector connector) {
        this.bettalimsConnector = connector;
    }

    public void setAthenaClientService(AthenaClientService athenaClientService) {
        this.athenaClientService = athenaClientService;
    }
}
