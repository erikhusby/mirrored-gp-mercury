package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.SquidPerson;
import org.broadinstitute.gpinformatics.mercury.boundary.SquidTopicPortype;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 12:58 PM
 */
@Impl
public class PMBSequencingServiceImpl implements PMBSequencingService {

    public static final IllegalArgumentException ILLEGAL_EXPERIMENTID_ARG_EXCEPTION =
            new IllegalArgumentException("Cannot get sequencing experiment request without a remote sequencing experiment Id");
    public static final IllegalArgumentException ILLEGAL_USER_ARG_EXCEPTION =
            new IllegalArgumentException("Cannot get experiment request summaries without a valid username.");
    public static final IllegalArgumentException ILLEGAL_EXPREQ_ARG_EXCEPTION = new IllegalArgumentException("Experiment request is null.");

    private org.apache.commons.logging.Log logger = LogFactory.getLog(PMBSequencingServiceImpl.class);

    private SquidTopicPortype squidServicePort;
    private boolean initialized = false;


    public final String SQUID_NAMESPACE = "urn:SquidTopic";
    public final String SQUID_TOPIC = "SquidTopicService";
    public final String SQUID_WSDL = "/services/SquidTopicService?WSDL";



    @Inject
    private SquidConfig squidConfig;

    public PMBSequencingServiceImpl() {
    }


    public PMBSequencingServiceImpl( SquidConfig squidConfig ) {
        this.squidConfig = squidConfig;
    }


    private void init(final SquidConfig seqConnectionParameters) throws MalformedURLException {
        QName serviceName = new QName(SQUID_NAMESPACE, SQUID_TOPIC);
        String wsdlURL = seqConnectionParameters.getUrl() + SQUID_WSDL;
        URL url = new URL(wsdlURL);
        Service service = Service.create(url, serviceName);
        squidServicePort = service.getPort(serviceName, SquidTopicPortype.class);
        this.initialized = true;
    }


    private SquidTopicPortype getSquidServicePort() {

        try {
            // Try to initialize.
            if (!initialized) {
                init(squidConfig);
            }

            // Try to test connection.
            String greeting = squidServicePort.getGreeting();
            logger.debug("Greeting from SQUID is : " + greeting);

        } catch (Exception e) {
            initialized = false;
            String squidRoot = (squidServicePort != null ? squidConfig.getUrl() : "Null SeqConnectionParameters.");
            String errMsg = "Could not connect to Sequencing platform. Squidroot : " + squidRoot;
            logger.error(errMsg);
            throw new RuntimeException(errMsg, e);
        }
        return squidServicePort;
    }

    @Override
    public List<Person> getPlatformPeople() {
        List<Person> persons = new ArrayList<Person>();

        List<SquidPerson> squidPeople = null;
        try {
            squidPeople = getSquidServicePort().getBroadPIList().getSquidPerson();
        } catch (Exception exp) {
            String errMsg = "Exception occurred retrieving the sequencing platform related personnel. ";
            logger.error(errMsg + exp.getMessage());
            throw new RuntimeException(errMsg, exp);
        }

        for (SquidPerson squidPerson : squidPeople) {
            if ((squidPerson != null) && (squidPerson.getPersonID() != null)) {
                Person person = new Person(squidPerson.getLogin(), squidPerson.getFirstName(), squidPerson.getLastName());
                persons.add(person);
            }
        }
        return persons;
    }

    //For unit testing only
    PMBSequencingServiceImpl(SquidTopicPortype squidServicePort) {
        this.squidServicePort = squidServicePort;
        initialized = true;
    }


}
