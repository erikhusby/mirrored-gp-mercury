package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStore;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
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
//import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
//import javax.xml.bind.ValidationEvent;
//import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.sax.SAXSource;
//import javax.xml.validation.Schema;
//import javax.xml.validation.SchemaFactory;
//import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Allows BettaLIMS messages to be submitted through JAX-RS.  In this context, BettaLIMS refers to the message format,
 * defined by bettalims.xsd.  There is a BettaLIMS server that is part of the Squid suite of applications.
 */
@Path("/bettalimsmessage")
@Stateful
@RequestScoped
public class BettalimsMessageResource {
    /** workflow error message from Squid */
    public static final String WORKFLOW_MESSAGE = " error(s) processing workflows for ";

    private static final Log LOG = LogFactory.getLog(BettalimsMessageResource.class);

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private LabEventHandler labEventHandler;

    @Inject
    private WsMessageStore wsMessageStore;

//    @Inject
//    private BucketDao bucketDao;
//
//    @Inject
//    private BucketBean bucketBean;
//
//    @Inject
//    private BSPUserList bspUserList;
//
//    @Inject
//    private WorkflowLoader workflowLoader;
//
//    @Inject
//    private AthenaClientService athenaClientService;

    @Inject
    private BettalimsConnector bettalimsConnector;

    @Inject
    private ThriftService thriftService;

    /**
     * Accepts a message from (typically) a liquid handling deck.  We unmarshal ourselves, rather than letting JAX-RS
     * do it, because we need to write the text to the file system.
     * @param message the text of the message
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML})
    public Response processMessage(String message) {
        try {
            storeAndProcess(message);
        } catch (Exception e) {
            if(e.getMessage().contains(WORKFLOW_MESSAGE)) {
                throw new ResourceException(e.getMessage(), Response.Status.CREATED);
            } else {
                throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        // The VWorks client seems to prefer 200 to 204
        return Response.status(Response.Status.OK).entity("Message persisted").type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    /**
     * Shared between JAX-RS and JMS.
     * Transaction is REQUIRED because this method is called by an MDB with transaction NOT_SUPPORTED.
     * @param message from deck
     * @throws Exception
     */
    @TransactionAttribute(value= TransactionAttributeType.REQUIRED)
    public void storeAndProcess(String message) throws Exception {
        Date now = new Date();
        //noinspection OverlyBroadCatchBlock
        try {
            wsMessageStore.store(message, now);

            BettaLIMSMessage bettaLIMSMessage = unmarshal(message);

            boolean processInMercury = false;
            boolean processInSquid = false;
            if(bettaLIMSMessage.getMode() != null && bettaLIMSMessage.getMode().equals(LabEventFactory.MODE_MERCURY)) {
                // Don't route Mercury test messages to BettalIMS/Squid
                processInMercury = true;
                processInSquid = false;
            } else {
                LabEventType labEventType = getLabEventType(bettaLIMSMessage);
                if(labEventType == null) {
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
                        // todo jmt for Mar 1, traverse plastic
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
            wsMessageStore.recordError(message, now, e);
            LOG.error("Failed to process run", e);
            throw e;
        }
    }

    /**
     * Convert from String to object
     * @param message from deck
     * @return JAXB bean
     * @throws JAXBException
     * @throws SAXException
     */
    BettaLIMSMessage unmarshal(String message) throws JAXBException, SAXException {
        // todo jmt move so done only once
//        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//        Schema schema = sf.newSchema(new File("e:/java/mercury/mercury/src/main/xsd/bettalims.xsd"));

        JAXBContext jc = JAXBContext.newInstance(BettaLIMSMessage.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
//        unmarshaller.setSchema(schema);
//        unmarshaller.setEventHandler(new ValidationEventHandler() {
//            @Override
//            public boolean handleEvent(ValidationEvent event) {
//                throw new RuntimeException("XSD Validation failure: "+
//                        "SEVERITY: " + event.getSeverity() + "MESSAGE: " + event.getMessage() +
//                        "LINKED EXCEPTION:  " + event.getLinkedException() +
//                        "LINE NUMBER:  " + event.getLocator().getLineNumber() +
//                        "COLUMN NUMBER:  " + event.getLocator().getColumnNumber() +
//                        "OFFSET:  " + event.getLocator().getOffset() +
//                        "OBJECT:  " + event.getLocator().getObject() +
//                        "NODE:  " + event.getLocator().getNode() +
//                        "URL:  " + event.getLocator().getURL());
//            }
//        });

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
     * @param bettaLIMSMessage from deck
     * @return enum value, or null if not found
     */
    private LabEventType getLabEventType(BettaLIMSMessage bettaLIMSMessage) {
        LabEventType labEventType = null;
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            labEventType = LabEventType.getByName(plateCherryPickEvent.getEventType());
            if(labEventType != null) {
                break;
            }
        }
        if (labEventType == null) {
            for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
                labEventType = LabEventType.getByName(plateEventType.getEventType());
                if(labEventType != null) {
                    break;
                }
            }
        }
        if(labEventType == null) {
            for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
                labEventType = LabEventType.getByName(plateTransferEventType.getEventType());
                if(labEventType != null) {
                    break;
                }
            }
        }
        if(labEventType == null) {
            for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
                labEventType = LabEventType.getByName(receptaclePlateTransferEvent.getEventType());
                if(labEventType != null) {
                    break;
                }
            }
        }
        if(labEventType == null) {
            for (ReceptacleEventType receptacleEventType : bettaLIMSMessage.getReceptacleEvent()) {
                labEventType = LabEventType.getByName(receptacleEventType.getEventType());
                if(labEventType != null) {
                    break;
                }
            }
        }
        return labEventType;
    }

    /**
     * Not needed until March 1st.
     * Determine whether BettaLIMS/Squid is responsible for the plastic referred to by the message.
     * @param bettaLIMSMessage from deck
     * @return true if the message should be routed to BettaLIMS/Squid
     */
    private boolean sourcesExistInSquid(BettaLIMSMessage bettaLIMSMessage) {
        List<String> sourceTubeBarcodes = new ArrayList<String>();
        List<String> sourcePlateBarcodes = new ArrayList<String>();
        for (PlateCherryPickEvent plateCherryPickEvent : bettaLIMSMessage.getPlateCherryPickEvent()) {
            for (PositionMapType positionMapType : plateCherryPickEvent.getSourcePositionMap()) {
                for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                    sourceTubeBarcodes.add(receptacleType.getBarcode());
                }
            }
        }
        for (PlateEventType plateEventType : bettaLIMSMessage.getPlateEvent()) {
            if(plateEventType.getPlate() != null) {
                sourcePlateBarcodes.add(plateEventType.getPlate().getBarcode());
            }
            if(plateEventType.getPositionMap() != null) {
                for (ReceptacleType receptacleType : plateEventType.getPositionMap().getReceptacle()) {
                    sourceTubeBarcodes.add(receptacleType.getBarcode());
                }
            }
        }
        for (PlateTransferEventType plateTransferEventType : bettaLIMSMessage.getPlateTransferEvent()) {
            if(plateTransferEventType.getSourcePlate() != null) {
                sourcePlateBarcodes.add(plateTransferEventType.getSourcePlate().getBarcode());
            }
            if(plateTransferEventType.getSourcePositionMap() != null) {
                for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap().getReceptacle()) {
                    sourceTubeBarcodes.add(receptacleType.getBarcode());
                }
            }
        }
        for (ReceptaclePlateTransferEvent receptaclePlateTransferEvent : bettaLIMSMessage.getReceptaclePlateTransferEvent()) {
            sourceTubeBarcodes.add(receptaclePlateTransferEvent.getSourceReceptacle().getBarcode());
        }
        if(sourceTubeBarcodes.isEmpty() && sourcePlateBarcodes.isEmpty()) {
            throw new RuntimeException("Failed to find any sources");
        }
        boolean squidRecognizesTubes = false;
        Map<String,Boolean> mapPositionToOccupied = null;
        if(!sourceTubeBarcodes.isEmpty()) {
            squidRecognizesTubes = thriftService.doesSquidRecognizeAllLibraries(sourceTubeBarcodes);
        } else if(!sourcePlateBarcodes.isEmpty()) {
            // todo jmt fetchPlateInfo would be more efficient
            // todo jmt catch exception
            mapPositionToOccupied = thriftService.fetchParentRackContentsForPlate(sourcePlateBarcodes.get(0));
        }
        return (squidRecognizesTubes || (mapPositionToOccupied != null));
    }

    /**
     * Process JAXB message bean after it is converted from string
     * @param message JAXB
     */
    public void processMessage(BettaLIMSMessage message) {
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        for (LabEvent labEvent : labEvents) {
            labEventHandler.processEvent(labEvent);

/*
//          TODO SGM  Commenting to revisit after GPLIM-517

            Map<WorkflowStepDef, Collection<LabVessel>> bucketVessels = labEventHandler.itemizeBucketItems(labEvent);

            if(bucketVessels.keySet().size() ==1) {

                WorkflowStepDef workingBucketIdentifier = bucketVessels.keySet().iterator().next();
                Bucket workingBucket = bucketDao.findByName(workingBucketIdentifier.getName());
                if(workingBucket == null) {
                    workingBucket = new Bucket(workingBucketIdentifier);
                }

                bucketBean.start(bspUserList.getById(labEvent.getEventOperator()).getUsername(),
                                 labEvent.getAllLabVessels(),
                                 workingBucket,
                                 labEvent.getEventLocation());
            }
*/
        }

    }

    /** Allows documents that don't include a namespace */
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
