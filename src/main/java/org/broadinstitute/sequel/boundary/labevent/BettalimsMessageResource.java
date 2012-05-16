package org.broadinstitute.sequel.boundary.labevent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.control.dao.labevent.LabEventDao;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.control.labevent.MessageStore;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
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
import javax.ws.rs.WebApplicationException;
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
@Stateless
public class BettalimsMessageResource {

    private static final Log LOG = LogFactory.getLog(BettalimsMessageResource.class);

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private LabEventHandler labEventHandler;

    @Inject
    private LabEventDao labEventDao;

    public static class BettaLIMSException extends WebApplicationException {
        public BettaLIMSException(String message, int status) {
            super(Response.status(status).entity(message).type(MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    /**
     * Accepts a message from (typically) a liquid handling deck
     * @param message the text of the message
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML})
    public Response processMessage(String message) {
        Date now = new Date();
        // todo jmt make this configurable
        MessageStore messageStore = new MessageStore("c:/temp/sequel/messages");
        try {
            messageStore.store(message, now);

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
            messageStore.recordError(message, now, e);
            LOG.error("Failed to process run", e);
/*
todo jmt fix this
            if(e.getMessage().contains(LabWorkflowBatchException.MESSAGE)) {
                throw new BettaLIMSException(e.getMessage(), 201);
            } else {
*/
                throw new BettaLIMSException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
/*
            }
*/
        }
        // The VWorks client seems to prefer 200 to 204
        return Response.status(Response.Status.OK).entity("Message persisted").type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    public void processMessage(BettaLIMSMessage message) {
        List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
        for (LabEvent labEvent : labEvents) {
            labEventHandler.processEvent(labEvent, null);
            labEventDao.persist(labEvent);
        }
    }

    /** Allows documents that don't include a namespace */
    static class NamespaceFilter extends XMLFilterImpl {

        private String usedNamespaceUri;
        private boolean addNamespace;

        private boolean addedNamespace;

        NamespaceFilter(String namespaceUri, boolean addNamespace) {
            if (addNamespace) {
                this.usedNamespaceUri = namespaceUri;
            } else {
                this.usedNamespaceUri = "";
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
            super.startElement(this.usedNamespaceUri, localName, qName, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(this.usedNamespaceUri, localName, qName);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (addNamespace) {
                this.startControlledPrefixMapping();
            } else {
                //Remove the namespace, i.e. don´t call startPrefixMapping for parent!
            }
        }

        private void startControlledPrefixMapping() throws SAXException {
            if (this.addNamespace && !this.addedNamespace) {
                //We should add namespace since it is set and has not yet been done.
                super.startPrefixMapping("", this.usedNamespaceUri);

                //Make sure we dont do it twice
                this.addedNamespace = true;
            }
        }
    }
}