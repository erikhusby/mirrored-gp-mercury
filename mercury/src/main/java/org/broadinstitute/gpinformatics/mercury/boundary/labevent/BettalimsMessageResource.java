package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStore;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.util.Date;
import java.util.List;

/**
 * Allows BettaLIMS messages to be submitted through JAX-RS
 */
@Path("/bettalimsmessage")
@Stateless // todo jmt should this be stateful?  It has stateful DAOs injected.
public class BettalimsMessageResource {

    private static final Log LOG = LogFactory.getLog(BettalimsMessageResource.class);

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private LabEventHandler labEventHandler;

    @Inject
    private WsMessageStore wsMessageStore;

    /**
     * Accepts a message from (typically) a liquid handling deck
     * @param message the text of the message
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML})
    public Response processMessage(String message) {
        try {
            storeAndProcess(message);
        } catch (Exception e) {
            /*
            todo jmt fix this
                        if(e.getMessage().contains(LabWorkflowBatchException.MESSAGE)) {
                            throw new ResourceException(e.getMessage(), Response.Status.CREATED);
                        } else {
            */
            throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            /*
                        }
            */
        }
        // The VWorks client seems to prefer 200 to 204
        return Response.status(Response.Status.OK).entity("Message persisted").type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    /**
     * Shared between JAX-RS and JMS
     * @param message from deck
     * @throws Exception
     */
    public void storeAndProcess(String message) throws Exception {
        Date now = new Date();
        //noinspection OverlyBroadCatchBlock
        try {
            wsMessageStore.store(message, now);

            // todo jmt move so done only once
            JAXBContext jc = JAXBContext.newInstance(BettaLIMSMessage.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            //Create an XMLReader to use with our filter
            XMLReader reader = XMLReaderFactory.createXMLReader();

            //Create the filter (to remove namespace) and set the xmlReader as its parent.
            NamespaceFilter inFilter = new NamespaceFilter(null, false);
            inFilter.setParent(reader);

            //Prepare the input
            InputSource is = new InputSource(new StringReader(message));

            //Create a SAXSource specifying the filter
            SAXSource source = new SAXSource(inFilter, is);

            processMessage((BettaLIMSMessage) unmarshaller.unmarshal(source));
        } catch (Exception e) {
            wsMessageStore.recordError(message, now, e);
            LOG.error("Failed to process run", e);
            throw e;
        }
    }

    /**
     * Process JAXB message bean after it is converted from string
     * @param message JAXB
     */
    public void processMessage(BettaLIMSMessage message) {
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        for (LabEvent labEvent : labEvents) {
            labEventHandler.processEvent(labEvent);
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
                //Remove the namespace, i.e. don't call startPrefixMapping for parent!
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