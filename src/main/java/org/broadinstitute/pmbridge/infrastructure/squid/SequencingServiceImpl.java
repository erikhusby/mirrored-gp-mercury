package org.broadinstitute.pmbridge.infrastructure.squid;

import clover.org.jfree.util.Log;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.broad.squid.services.TopicService.*;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.RemoteId;
import org.broadinstitute.pmbridge.entity.experiments.seq.*;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.PlatformType;
import org.broadinstitute.pmbridge.infrastructure.SubmissionException;
import org.broadinstitute.pmbridge.infrastructure.ValidationException;

import javax.enterprise.inject.Default;
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
@Default
public class SequencingServiceImpl implements SequencingService {

    private org.apache.commons.logging.Log logger = LogFactory.getLog(SequencingServiceImpl.class);
    private SquidTopicPortype squidServicePort;
    private boolean initialized = false;
    private SeqConnectionParameters seqConnectionParameters;

    @Inject
    public SequencingServiceImpl(SeqConnectionParameters seqConnectionParameters) throws MalformedURLException {

        this.seqConnectionParameters = seqConnectionParameters;
        init(seqConnectionParameters);
    }

    private void init(final SeqConnectionParameters seqConnectionParameters) throws MalformedURLException {
        QName serviceName = new QName(SeqConnectionParameters.SQUID_NAMESPACE, SeqConnectionParameters.SQUID_TOPIC);
        String wsdlURL = seqConnectionParameters.getSquidRoot() + SeqConnectionParameters.SQUID_WSDL;
        URL url = new URL(wsdlURL);
        Service service = Service.create(url, serviceName);
        squidServicePort = service.getPort(serviceName, SquidTopicPortype.class);
        this.initialized = true;
    }


    private SquidTopicPortype getSquidServicePort() {

        try {
            // Try to initialize.
            if ( ! initialized ) {
                init(seqConnectionParameters);
            }

            // Try to test connection.
            String greeting = squidServicePort.getGreeting();
            logger.info("Greeting from SQUID is : " + greeting);

        }  catch (Exception e ) {
            initialized = false;
            String squidRoot = ( seqConnectionParameters != null ? seqConnectionParameters.getSquidRoot() : "Null SeqConnectionParameters.");
            throw new RuntimeException( "Cannot connect to SQUID at : " +
                    squidRoot, e );
        }
        return squidServicePort;
    }

    @Override
    public List<Person> getPlatformPeople() {
        List<Person> persons = new ArrayList<Person>();

        List<SquidPerson> squidPeople = getSquidServicePort().getBroadPIList().getSquidPerson();
        for (SquidPerson squidPerson : squidPeople ) {
            Person person = new Person(squidPerson.getLogin(), squidPerson.getFirstName(), squidPerson.getLastName(),
                    squidPerson.getPersonID().toString(),
                    RoleType.BROAD_SCIENTIST );
            persons.add(person);
        }
        return persons;
    }

    @Override
    public List<OrganismName> getOrganisms() {
        List<OrganismName> organismNames = new ArrayList<OrganismName>();

        List<Organism> organismlist = getSquidServicePort().getOrganisms().getOrganismList();
        for (Organism organism : organismlist) {
            OrganismName organismName = new OrganismName(organism.getGenus() + " " + organism.getSpecies() ,
                    organism.getCommonName(),
                    organism.getId());
            organismNames.add(organismName);
        }
        return organismNames;
    }

    @Override
    public List<BaitSetName> getBaitSets() {
        List<BaitSetName> baitSetNames = new ArrayList<BaitSetName>();

        List<BaitSet> baitSetList = getSquidServicePort().getBaitSets().getBaitSetList();
        for (BaitSet baitSet : baitSetList) {
            BaitSetName baitSetName = new BaitSetName(baitSet.getDesignName());
            baitSetNames.add(baitSetName);
        }
        return baitSetNames;
    }

    @Override
    public List<ReferenceSequenceName> getReferenceSequences() {
        List<ReferenceSequenceName> referenceSequenceNames = new ArrayList<ReferenceSequenceName>();

        List<ReferenceSequence> ReferenceSequenceList = getSquidServicePort().getReferenceSequences().getReferenceSequenceList();
        for (ReferenceSequence referencesequence : ReferenceSequenceList) {
            ReferenceSequenceName referenceSequenceName = new ReferenceSequenceName(referencesequence.getAlias(),
                    referencesequence.getId());
            referenceSequenceNames.add(referenceSequenceName);
        }
        return referenceSequenceNames;
    }


    @Override
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(Person creator) {
        List<ExperimentRequestSummary> requestSummaries = new ArrayList<ExperimentRequestSummary>();

        List<SummarizedPass> summarizedPassList =
                getSquidServicePort().searchPassesByCreator(creator.getUsername()).getSummarizedPassList();

        for (SummarizedPass summary : summarizedPassList ) {
            ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary(creator, PlatformType.GSP,
                    summary.getType().name() );
            experimentRequestSummary.setRemoteId(new RemoteId(summary.getPassNumber()));
            experimentRequestSummary.setTitle(new Name(summary.getTitle()));
            requestSummaries.add(experimentRequestSummary);
        }
        return requestSummaries;
    }

    @Override
    public SeqExperimentRequest getPlatformRequest(ExperimentRequestSummary experimentRequestSummary) {
        SeqExperimentRequest seqExperimentRequest;

        // Sanity checks.
        if ( (experimentRequestSummary == null)  ||
             (experimentRequestSummary.getRemoteId() == null) ||
             StringUtils.isBlank(experimentRequestSummary.getRemoteId().value)) {
            throw new IllegalArgumentException("Cannot get sequencing experiment request without a remote sequencing experiment Id");
        }

        AbstractPass pass = getSquidServicePort().loadPassByNumber(experimentRequestSummary.getRemoteId().toString());

        if (pass instanceof WholeGenomePass) {
            seqExperimentRequest = new WholeGenomeExperiment(experimentRequestSummary, (WholeGenomePass) pass);
        } else if ( pass instanceof DirectedPass) {
            seqExperimentRequest = new HybridSelectionExperiment(experimentRequestSummary, (DirectedPass) pass);
        } else if (pass instanceof RNASeqPass ){
            seqExperimentRequest = new RNASeqExperiment(experimentRequestSummary, (RNASeqPass) pass);
        } else {
            throw new IllegalArgumentException("Unsupported type of PASS. Title  : " + pass.getClass().getName() );
        }


        return seqExperimentRequest;
    }

    @Override
    public void validatePlatformRequest(SeqExperimentRequest seqExperimentRequest)
            throws ValidationException {

        // Check for null exp request
        if (seqExperimentRequest == null) {
            throw new ValidationException("Experiment request is null." );
        }

        //Retrieve an identifier
        //TODO - Maybe move this to a helper method in the experiment request class so (depending on the state) the most appropriate identifier will be returned.
        String identifier = (seqExperimentRequest.getRemoteId() != null) ? seqExperimentRequest.getRemoteId().value :
                seqExperimentRequest.getLocalId().value;

        //TODO Apply other PMBridge server side Validations. Start

        //TODO Apply other PMBridge server side Validations. End

        List<String>  errorMessages = seqExperimentRequest.validate ( getSquidServicePort() );

        Log.debug("Validated experiment request: " + identifier + " with " + errorMessages.size() +  " messages.");

        // Handle the error messages returned here and propagate upwards to clients in an exception.
        if ( errorMessages.size() > 0 ) {
            StringBuilder messages = new StringBuilder("Failed Sequencing Platform validation :");
            for ( String msg : errorMessages ) {
                messages.append(" ").append(msg);
            }
            throw new ValidationException( messages.toString() );
        }

    }

    @Override
    public SeqExperimentRequest submitRequestToPlatform(SeqExperimentRequest seqExperimentRequest)
            throws ValidationException, SubmissionException {

        validatePlatformRequest(seqExperimentRequest);

        //TODO hmc under construction
        seqExperimentRequest.submit(getSquidServicePort());

        return  seqExperimentRequest;
    }


    //For unit testing only
    SequencingServiceImpl(SquidTopicPortype squidServicePort) {
        this.squidServicePort = squidServicePort;
        initialized = true;
    }


}
