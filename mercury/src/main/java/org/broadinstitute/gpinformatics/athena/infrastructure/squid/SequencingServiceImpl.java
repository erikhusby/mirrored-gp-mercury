package org.broadinstitute.gpinformatics.athena.infrastructure.squid;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.common.ChangeEvent;
import org.broadinstitute.gpinformatics.athena.entity.common.Name;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentId;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentType;
import org.broadinstitute.gpinformatics.athena.entity.experiments.seq.*;
import org.broadinstitute.gpinformatics.athena.entity.person.Person;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.SubmissionException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.boundary.*;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 12:58 PM
 */
@Default
public class SequencingServiceImpl implements SequencingService {

    public static final IllegalArgumentException ILLEGAL_EXPERIMENTID_ARG_EXCEPTION =
            new IllegalArgumentException("Cannot get sequencing experiment request without a remote sequencing experiment Id");
    public static final IllegalArgumentException ILLEGAL_USER_ARG_EXCEPTION =
            new IllegalArgumentException("Cannot get experiment request summaries without a valid username.");
    public static final IllegalArgumentException ILLEGAL_EXPREQ_ARG_EXCEPTION = new IllegalArgumentException("Experiment request is null.");

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
            if (!initialized) {
                init(seqConnectionParameters);
            }

            // Try to test connection.
            String greeting = squidServicePort.getGreeting();
            logger.debug("Greeting from SQUID is : " + greeting);

        } catch (Exception e) {
            initialized = false;
            String squidRoot = (seqConnectionParameters != null ? seqConnectionParameters.getSquidRoot() : "Null SeqConnectionParameters.");
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
                Person person = new Person(squidPerson.getLogin(), squidPerson.getFirstName(), squidPerson.getLastName(),
                        "" + squidPerson.getPersonID().intValue(),
                        RoleType.BROAD_SCIENTIST);
                persons.add(person);
            }
        }
        return persons;
    }

    @Override
    public List<OrganismName> getOrganisms() {
        List<OrganismName> organismNames = new ArrayList<OrganismName>();

        List<Organism> organismList = null;
        try {
            organismList = getSquidServicePort().getOrganisms().getOrganismList();
        } catch (Exception exp) {
            String errMsg = "Exception occurred retrieving the sequencing platform organisms. ";
            logger.error(errMsg + exp.getMessage());
            throw new RuntimeException(errMsg, exp);
        }

        for (Organism organism : organismList) {
            if ((organism != null) && (organism.getId() > 0)) {
                OrganismName organismName = new OrganismName(organism.getGenus() + " " + organism.getSpecies(),
                        organism.getCommonName(),
                        organism.getId());
                organismNames.add(organismName);
            }
        }
        return organismNames;
    }

    @Override
    public List<BaitSetName> getBaitSets() {
        List<BaitSetName> baitSetNames = new ArrayList<BaitSetName>();

        List<BaitSet> baitSetList = null;
        try {
            baitSetList = getSquidServicePort().getBaitSets().getBaitSetList();
        } catch (Exception exp) {
            String errMsg = "Exception occurred retrieving the sequencing platform baitsets. ";
            logger.error(errMsg + exp.getMessage());
            throw new RuntimeException(errMsg, exp);
        }

        for (BaitSet baitSet : baitSetList) {
            if ((baitSet != null) && (baitSet.getId() > 0)) {
                BaitSetName baitSetName = new BaitSetName(baitSet.getDesignName(), baitSet.getId());
                baitSetNames.add(baitSetName);
            }
        }
        return baitSetNames;
    }

    @Override
    public List<ReferenceSequenceName> getReferenceSequences() {
        List<ReferenceSequenceName> referenceSequenceNames = new ArrayList<ReferenceSequenceName>();

        List<ReferenceSequence> referenceSequenceList = null;
        try {
            referenceSequenceList = getSquidServicePort().getReferenceSequences().getReferenceSequenceList();
        } catch (Exception exp) {
            String errMsg = "Exception occurred retrieving the sequencing platform reference sequences. ";
            logger.error(errMsg + exp.getMessage());
            throw new RuntimeException(errMsg, exp);
        }

        for (ReferenceSequence referenceSequence : referenceSequenceList) {
            if ((referenceSequence != null) && (referenceSequence.getId() > 0) && referenceSequence.isActive()) {
                ReferenceSequenceName referenceSequenceName = new ReferenceSequenceName(referenceSequence.getAlias(),
                        referenceSequence.getId());
                referenceSequenceNames.add(referenceSequenceName);
            }
        }
        return referenceSequenceNames;
    }


    @Override
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(Person creator) {


        // Sanity checks.
        if ((creator == null) ||
                (creator.getUsername() == null) ||
                StringUtils.isBlank(creator.getUsername())) {
            throw ILLEGAL_USER_ARG_EXCEPTION;
        }
        String userName = creator.getUsername();

        List<SummarizedPass> summarizedPassList = null;
        try {
            summarizedPassList = getSquidServicePort().searchPassesByCreator(userName).getSummarizedPassList();
        } catch (Exception exp) {
            String errMsg = "Exception occurred retrieving the sequencing experiment summaries for user : " + userName;
            logger.error(errMsg + exp.getMessage());
            throw new RuntimeException(errMsg, exp);
        }

        List<ExperimentRequestSummary> requestSummaries = new ArrayList<ExperimentRequestSummary>();

        for (SummarizedPass summary : summarizedPassList) {
            if (summary != null) {
                Date updatedDate = null;
                if (summary.getCreatedDate() != null) {
                    updatedDate = summary.getCreatedDate().getTime();
                }
                ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary(
                        summary.getTitle(), creator, updatedDate,
                        ExperimentType.WholeGenomeSequencing
                );
                experimentRequestSummary.setExperimentId(new ExperimentId(summary.getPassNumber()));
                experimentRequestSummary.setTitle(new Name(summary.getTitle()));
                experimentRequestSummary.setModification(new ChangeEvent(updatedDate, new Person(summary.getUpdatedBy(), RoleType.PROGRAM_PM)));
                experimentRequestSummary.setStatus(new Name(summary.getStatus().name()));

                if (StringUtils.isNotBlank(summary.getResearchProject()) && Pattern.matches("[\\d]+", summary.getResearchProject().trim())) {
                    experimentRequestSummary.setResearchProjectId(new Long(summary.getResearchProject().trim()));
                }
                requestSummaries.add(experimentRequestSummary);
            }
        }
        return requestSummaries;
    }

    @Override
    public SeqExperimentRequest getPlatformRequest(ExperimentRequestSummary experimentRequestSummary) {
        SeqExperimentRequest seqExperimentRequest;

        // Sanity checks.
        if ((experimentRequestSummary == null) ||
                (experimentRequestSummary.getExperimentId() == null) ||
                StringUtils.isBlank(experimentRequestSummary.getExperimentId().value)) {
            throw ILLEGAL_EXPERIMENTID_ARG_EXCEPTION;
        }
        String passId = experimentRequestSummary.getExperimentId().value;

        AbstractPass pass = null;
        try {
            pass = getSquidServicePort().loadPassByNumber(passId);
        } catch (Exception exp) {
            logger.error("Exception occurred during loading of pass " + passId + " - " + exp.getMessage());
            if ((exp != null) && exp.getMessage().contains("Unrecognized pass number")) {
                throw new RuntimeException("Experiment Request with Id " + passId + " not recognized by Sequencing platform.", exp);
            } else {
                throw new RuntimeException(exp);
            }
        }

        if (pass instanceof WholeGenomePass) {
            seqExperimentRequest = new WholeGenomeExperiment(experimentRequestSummary, (WholeGenomePass) pass);
        } else if (pass instanceof DirectedPass) {
            seqExperimentRequest = new HybridSelectionExperiment(experimentRequestSummary, (DirectedPass) pass);
        } else if (pass instanceof RNASeqPass) {
            seqExperimentRequest = new RNASeqExperiment(experimentRequestSummary, (RNASeqPass) pass);
        } else {
            throw new IllegalArgumentException("Unsupported type of PASS. Title  : " + pass.getClass().getName());
        }


        return seqExperimentRequest;
    }

    @Override
    public void validatePlatformRequest(SeqExperimentRequest seqExperimentRequest)
            throws ValidationException {

        // Check for null exp request
        if (seqExperimentRequest == null) {
            throw ILLEGAL_EXPREQ_ARG_EXCEPTION;
        }

        //Retrieve an identifier
        String identifier = (seqExperimentRequest.getRemoteId() != null) ? seqExperimentRequest.getRemoteId().value :
                "null";

        // Apply other PMBridge server side Validations. Start

        // Apply other PMBridge server side Validations. End

        List<String> errorMessages = seqExperimentRequest.validate(getSquidServicePort());

        logger.debug("Validated experiment request: " + identifier + " with " + errorMessages.size() + " messages.");

        // Handle the error messages returned here and propagate upwards to clients in an exception.
        if (errorMessages.size() > 0) {
            StringBuilder messages = new StringBuilder("Failed Sequencing Platform validation :");
            for (String msg : errorMessages) {
                messages.append(" ").append(msg);
            }
            throw new ValidationException(messages.toString());
        }

    }

    @Override
    public SeqExperimentRequest submitRequestToPlatform(Person programMgr, SeqExperimentRequest seqExperimentRequest)
            throws ValidationException, SubmissionException {

        validatePlatformRequest(seqExperimentRequest);

        // Set the update information.
        ChangeEvent updateEvent = new ChangeEvent(new Date(), programMgr);
        seqExperimentRequest.getExperimentRequestSummary().setModification(updateEvent);

        seqExperimentRequest.submit(getSquidServicePort());

        return seqExperimentRequest;
    }


    //For unit testing only
    SequencingServiceImpl(SquidTopicPortype squidServicePort) {
        this.squidServicePort = squidServicePort;
        initialized = true;
    }


}
