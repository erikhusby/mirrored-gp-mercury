package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStore;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationSetupEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.labevent.BettaLimsMessageUtils;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.annotation.PostConstruct;
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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Allows BettaLIMS messages to be submitted through JAX-RS.  In this context, BettaLIMS refers to the message format,
 * defined by bettalims.xsd.  There is a BettaLIMS server that is part of the Squid suite of applications.
 */
@Path("/bettalimsmessage")
@RequestScoped
@Stateful
public class BettaLimsMessageResource {

    private static final String WORKFLOW_MESSAGE = " error(s) processing workflows for ";
    private static final Log log = LogFactory.getLog(BettaLimsMessageResource.class);
    private static final boolean VALIDATE_SCHEMA = false;

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private LabEventHandler labEventHandler;

    @Inject
    private WsMessageStore wsMessageStore;

    @Inject
    private WorkflowValidator workflowValidator;

    @Inject
    private AppConfig appConfig;

    @Inject
    private EmailSender emailSender;

    @Inject
    private UserBean userBean;

    @Inject
    private WorkflowLoader workflowLoader;

    private WorkflowConfig workflowConfig;

    public BettaLimsMessageResource() {
    }

    /** Constructor used for test purposes. */
    public BettaLimsMessageResource(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
        postConstructor();
    }

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void postConstructor() {
        if (workflowLoader != null) {
            workflowConfig = workflowLoader.getWorkflowConfig();
        }
        // Does the one-time lab event setup that is needed when processing messages.
        LabEvent.setupEventTypesThatCanFollowBucket(workflowConfig);
    }

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
        boolean logStacktrace = true;
        //noinspection OverlyBroadCatchBlock
        try {
            wsMessageStore.store(WsMessageStore.BETTALIMS_RESOURCE_TYPE, message, now);

            BettaLIMSMessage bettaLIMSMessage = unmarshal(message);
            // This has the side effect of setting the user for the audit trail.
            LabEventType labEventType = getLabEventType(bettaLIMSMessage);
            if (labEventType == null) {
                throw new RuntimeException("Failed to find event type");
            }
            processMessage(bettaLIMSMessage);
        } catch (Exception e) {
            wsMessageStore.recordError(WsMessageStore.BETTALIMS_RESOURCE_TYPE, message, now, e);
            if (logStacktrace) {
                log.error(e.getMessage(), e);
            } else {
                log.error(e.getMessage());
            }
            emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(),
                    Collections.<String>emptyList(), "[Mercury] Failed to process message", e.getMessage(), false, true);
            throw e;
        }
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
                    throw new RuntimeException("XSD Validation failure: " +
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
     * Find the first event type in the message.  Has side effect of setting the user for the audit trail.
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
                login(plateCherryPickEvent);
                break;
            }
        }
        if (labEventType == null) {
            for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
                labEventType = LabEventType.getByName(plateEventType.getEventType());
                if (labEventType != null) {
                    login(plateEventType);
                    break;
                }
            }
        }
        if (labEventType == null) {
            for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
                labEventType = LabEventType.getByName(plateTransferEventType.getEventType());
                if (labEventType != null) {
                    login(plateTransferEventType);
                    break;
                }
            }
        }
        if (labEventType == null) {
            for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : bettaLIMSMessage
                    .getReceptaclePlateTransferEvent()) {
                labEventType = LabEventType.getByName(receptaclePlateTransferEvent.getEventType());
                if (labEventType != null) {
                    login(receptaclePlateTransferEvent);
                    break;
                }
            }
        }
        if (labEventType == null) {
            for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
                labEventType = LabEventType.getByName(receptacleEventType.getEventType());
                if (labEventType != null) {
                    login(receptacleEventType);
                    break;
                }
            }
        }
        if (labEventType == null) {
            StationSetupEvent stationSetupEvent = bettaLIMSMessage.getStationSetupEvent();
            if (stationSetupEvent != null) {
                labEventType = LabEventType.getByName(stationSetupEvent.getEventType());
                if (labEventType != null) {
                    userBean.login(LabEventFactory.ACTIVITY_USER_ID);
                }
            }
        }
        if (labEventType == null) {
            for (ReceptacleTransferEventType receptacleTransferEventType : bettaLIMSMessage.getReceptacleTransferEvent()) {
                labEventType = LabEventType.getByName(receptacleTransferEventType.getEventType());
                if (labEventType != null) {
                    login(receptacleTransferEventType);
                    break;
                }
            }
        }
        return labEventType;
    }

    /**
     * Login, so the audit trail shows the user from the message.
     */
    private void login(StationEventType stationEventType) {
        userBean.login(stationEventType.getOperator());
    }

    /**
     * Builds a collection of {@link LabVessel} barcodes from a JAXB message bean that contains one or more event
     * beans.
     *
     * @param bettaLIMSMessage JAXB bean
     * @return list of barcodes
     */
    private Collection<String> getRegisteredBarcodesFromMessage(BettaLIMSMessage bettaLIMSMessage) {

        Set<String> barcodes = new HashSet<>();

        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            barcodes.addAll(BettaLimsMessageUtils.getBarcodesForCherryPick(plateCherryPickEvent));
        }
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            barcodes.addAll(BettaLimsMessageUtils.getBarcodesForPlateEvent(plateEventType));
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            barcodes.addAll(BettaLimsMessageUtils.getBarcodesForPlateTransfer(plateTransferEventType));
        }
        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent :
                bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            barcodes.addAll(BettaLimsMessageUtils.getBarcodesForReceptaclePlateTransfer(receptaclePlateTransferEvent));
        }
        for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
            barcodes.addAll(BettaLimsMessageUtils.getBarcodesForReceptacleEvent(receptacleEventType));
        }
        for (ReceptacleTransferEventType receptacleTransferEventType : bettaLIMSMessage.getReceptacleTransferEvent()) {
            barcodes.addAll(BettaLimsMessageUtils.getBarcodesForReceptacleTransferEvent(receptacleTransferEventType));
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
            workflowValidator.validateWorkflow(message);
        } catch (Exception e) {
            log.error("Failed to validate workflow", e);
            // Don't rethrow, workflow must not stop persistence of the message
        }
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        for (LabEvent labEvent : labEvents) {
            if (!Objects.equals(message.getMode(), LabEventFactory.MODE_BACKFILL)) {
                labEventHandler.processEvent(labEvent);
            }
            if (labEvent.hasAmbiguousLcsetProblem()) {
                emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                        "[Mercury] Vessels have ambiguous LCSET", "After " + labEvent.getLabEventType().getName() +
                                                                  " (" + labEvent.getLabEventId() + ")", false, true);
            }
        }
    }

    public void setLabEventFactory(LabEventFactory labEventFactory) {
        this.labEventFactory = labEventFactory;
    }

    /**
     * Allows documents that don't include a namespace
     */
    private static class NamespaceFilter extends XMLFilterImpl {

        private String usedNamespaceUri;
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
}
