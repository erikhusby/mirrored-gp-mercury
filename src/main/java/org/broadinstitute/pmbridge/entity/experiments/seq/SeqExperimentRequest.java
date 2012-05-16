package org.broadinstitute.pmbridge.entity.experiments.seq;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broad.squid.services.TopicService.*;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.AbstractExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.RemoteId;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.infrastructure.SubmissionException;
import org.broadinstitute.pmbridge.infrastructure.ValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:02 PM
 */
public class SeqExperimentRequest extends AbstractExperimentRequest {

    private Log logger = LogFactory.getLog(SeqExperimentRequest.class);
    // pass can be null if not constructing experiment request from data feed from Seq platform
    private AbstractPass pass;
    private PMBPassType passType;

    // Need the following to store lookup data since the pass only stores the integer id.
    private OrganismName organism;
    private ReferenceSequenceName referenceSequenceName;


    public SeqExperimentRequest(ExperimentRequestSummary experimentRequestSummary, PMBPassType passType ) {
        super(experimentRequestSummary);
        this.passType = passType;
    }

    public SeqExperimentRequest(ExperimentRequestSummary experimentRequestSummary, AbstractPass pass) {
        super(experimentRequestSummary);
        passType = determinePassTypeFromPass(pass);
        this.pass = pass;
    }

    private PMBPassType determinePassTypeFromPass(final AbstractPass pass) {
        PMBPassType result = null;
        if ( pass != null ) {
            if (pass instanceof WholeGenomePass) {
                result = PMBPassType.WG;
            } else if ( pass instanceof DirectedPass) {
                result = PMBPassType.DIRECTED;
            } else if (pass instanceof RNASeqPass ){
                result = PMBPassType.RNASeq;
            } else {
                throw new IllegalArgumentException("Unsupported type of PASS. Contents of pass : " + pass.toString() );
            }
        } else {
            throw new IllegalArgumentException("Cannot determine type of Pass. Object was null");
        }
        return result;
    }

    private AbstractPass getOrCreateAbstractPass() {
        if ((pass == null) && (passType != null)) {
        // Create pass from pmbPassType.
        switch (passType) {
            //TODO set mandatory defaults for each ??
            case WG:
                pass = new WholeGenomePass();
                break;
            case DIRECTED:
                pass = new DirectedPass();
                break;
            case RNASeq:
                pass = new RNASeqPass();
                break;
            default:
                // in case the enum is extended but not this code.
                throw new IllegalArgumentException("Unsupported Sequencing Pass Type. Supported values are : " + PMBPassType.values().toString() );
        }        }
        return pass;
    }

    private ProjectInformation getOrCreateProjectInformation() {
        ProjectInformation projectInformation;
        if( (null == getOrCreateAbstractPass().getProjectInformation()) ) {
            projectInformation = new ProjectInformation();
            getOrCreateAbstractPass().setProjectInformation(projectInformation);
        } else {
            projectInformation = getOrCreateAbstractPass().getProjectInformation();
        }
        return projectInformation;
    }

    private FundingInformation getOrCreateFundingInformation() {
        FundingInformation fundingInformation;
        if( (null == getOrCreateAbstractPass().getFundingInformation()) ) {
            fundingInformation = new FundingInformation();
            getOrCreateAbstractPass().setFundingInformation(fundingInformation);
        } else {
            fundingInformation = getOrCreateAbstractPass().getFundingInformation();
        }
        return fundingInformation;
    }

    private CoverageAndAnalysisInformation getOrCreateCoverageAndAnalysisInformation() {
            CoverageAndAnalysisInformation coverageAndAnalysisInformation;
            if( (null == getOrCreateAbstractPass().getCoverageAndAnalysisInformation()) ) {
                coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();
                getOrCreateAbstractPass().setCoverageAndAnalysisInformation(coverageAndAnalysisInformation);
            } else {
                coverageAndAnalysisInformation = getOrCreateAbstractPass().getCoverageAndAnalysisInformation();
            }
            return coverageAndAnalysisInformation;
        }

//
//    public Name getResearchProjectName() {
//        Name result = new Name(Name.UNSPECIFIED);  // initialized to Unspecified
//        String passResearchProjectStr = getOrCreateAbstractPass().getResearchProject();
//        if (StringUtils.isNotBlank( passResearchProjectStr ) ) {
//            result = new Name( passResearchProjectStr );
//        }
//        return result;
//    }
//
//    public void setResearchProjectName(final Name researchProjectName) {
//        //Set the research project Id on the pass DTO
//        getOrCreateAbstractPass().setResearchProject(researchProjectName.name);
//    }


    public String getRNAProtocol() {
        String rnaSeqProtocol = null;
        if (! (getOrCreateAbstractPass() instanceof RNASeqPass) ) {
                    throw new IllegalArgumentException("Cannot set the RNA protocol on a non-RNASeq experiment.");
        }
        RNASeqPass rnaSeqPass = (RNASeqPass) getOrCreateAbstractPass();
        RNASeqProtocolType rnaSeqProtocolType = rnaSeqPass.getProtocol();
        if ( null != rnaSeqProtocolType) {
            rnaSeqProtocol = rnaSeqProtocolType.value();
        }
        return rnaSeqProtocol;
    }

    public void setRNAProtocol(final String rnaProtocol) {
        if (! (getOrCreateAbstractPass() instanceof RNASeqPass) ) {
            throw new IllegalArgumentException("Cannot set the RNA protocol on a non-RNASeq experiment.");
        }
        RNASeqPass rnaSeqPass = (RNASeqPass) getOrCreateAbstractPass();
        RNASeqProtocolType rnaSeqProtocolType = convertToRNASeqProtocolEnumElseNull(rnaProtocol);
        if ( rnaSeqProtocolType == null ) {
            throw new IllegalArgumentException("Unrecognized RNA protocol type.");
        }
        rnaSeqPass.setProtocol(rnaSeqProtocolType );
    }

    public static RNASeqProtocolType convertToRNASeqProtocolEnumElseNull(String str) {
        for (RNASeqProtocolType eValue : RNASeqProtocolType.values()) {
            if (eValue.name().equalsIgnoreCase(str))
                return eValue;
        }
        return null;
    }

    public SeqTechnology getSeqTechnology() {
        SeqTechnology seqTechnology=null;

        ProjectInformation projectInformation = getOrCreateProjectInformation();
        SequencingTechnology sequencingTechnology = projectInformation.getSequencingTechnology();
        if ( null != sequencingTechnology ) {
            // Convert one external enum type to local enum type by the common value
            seqTechnology = SeqTechnology.fromValue(sequencingTechnology.value());
        }
        return seqTechnology;
    }

    public void setSeqTechnology(final SeqTechnology seqTechnology) {
        if (seqTechnology == null) {
            throw new IllegalStateException(SequencingTechnology.class.getSimpleName() + " must be set to a valid value.");
        }
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        projectInformation.setSequencingTechnology(SequencingTechnology.fromValue(seqTechnology.value()));
        getOrCreateAbstractPass().setProjectInformation(projectInformation);
    }

    public Name getTitle() {
        return new Name( getOrCreateProjectInformation().getTitle() );
    }

    public void setTitle(final Name title ) {
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        projectInformation.setTitle( getExperimentRequestSummary().getTitle().name );
        getOrCreateAbstractPass().setProjectInformation(projectInformation);
    }

    private Collection<Person> getOrCreatePersonsFromPass(SquidPersonList squidPersonList, RoleType roleType) {
        List<Person> people = null;
        for (SquidPerson squidPerson : squidPersonList.getSquidPerson() ) {
            Person person = new Person(
                    squidPerson.getLogin(),
                    squidPerson.getFirstName(),
                    squidPerson.getLastName(),
                    squidPerson.getPersonID().toString(),
                    roleType );
            people.add(person);
        }
        return people;
    }


    public Collection<Person> getSponsoringScientists() {
        return getOrCreatePersonsFromPass( getOrCreateSponsoringScientists(), RoleType.BROAD_SCIENTIST );
    }

    /*
      The sponsoring scientists are associated with the Research Project
      but need to be set on the experiment request when submitting
     */
    public void setSponsoringScientists(final Collection<Person> sponsoringScientists) {

        ProjectInformation projectInformation = getOrCreateProjectInformation();

        SquidPersonList squidPersonList = getOrCreateSponsoringScientists();
        List<SquidPerson> squidPersons  = squidPersonList.getSquidPerson();
        for (Person sponsoringScientist : sponsoringScientists ) {
            SquidPerson squidPerson = new SquidPerson();
            squidPerson.setFirstName( sponsoringScientist.getFirstName() );
            squidPerson.setLastName( sponsoringScientist.getLastName() );
            squidPerson.setLogin( sponsoringScientist.getUsername() );
            squidPersons.add( squidPerson );
        }
        projectInformation.setSponsoringScientists( squidPersonList );
    }

    private SquidPersonList getOrCreateSponsoringScientists() {
        SquidPersonList result = null;
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        if ( null == projectInformation.getSponsoringScientists() ) {
            SquidPersonList squidPersonList = new SquidPersonList();
            List<SquidPerson> squidPersons = squidPersonList.getSquidPerson();  //this call ensures list is initialized.
            projectInformation.setSponsoringScientists( squidPersonList );
        }
        result = projectInformation.getSponsoringScientists();
        return result;
    }

    private SquidPersonList getOrCreatePlatformProjectManagers() {
        SquidPersonList result = null;
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        if ( null == projectInformation.getPlatformProjectManagers() ) {
            SquidPersonList squidPersonList = new SquidPersonList();
            List<SquidPerson> squidPersons = squidPersonList.getSquidPerson();  //this call ensures list is initialized.
            projectInformation.setPlatformProjectManagers(squidPersonList);
        }
        result = projectInformation.getPlatformProjectManagers();
        return result;
    }

    @Override
    public RemoteId getRemoteId() {
        return super.getExperimentRequestSummary().getRemoteId();
    }

    @Override
    public void setRemoteId(final RemoteId remoteId) {

        RemoteId currentRemoteId = super.getExperimentRequestSummary().getRemoteId();
        //Check against changing the remote Id of an experiment request with a new and different remoteId
        if ( (currentRemoteId != null) &&
                StringUtils.isNotBlank( currentRemoteId.value ) &&
                (! currentRemoteId.value.equals(remoteId.value)) ) {
            throw new IllegalStateException("Cannot overwrite the current remote Id <"+ currentRemoteId.value +
                    "> with another different remote id <" + remoteId.value + ">." );
        }
        getExperimentRequestSummary().setRemoteId( remoteId );

        //Also ensure the remote id on the pass is up to date.
        if ( StringUtils.isBlank( getOrCreateProjectInformation().getPassNumber() ) ) {
            getOrCreateProjectInformation().setPassNumber( remoteId.value );
        }
    }


    public String getVersion() {
        return "" + getOrCreateProjectInformation().getVersion();
    }

    @Override
    public Collection<Person> getPlatformProjectManagers() {
        // get the platform project managers list from the abstract pass the was received from squid.
        return getOrCreatePersonsFromPass( getOrCreatePlatformProjectManagers(), RoleType.PLATFORM_PM );
    }

    public List<String> getIrbNumbers() {
        List<String> result = new ArrayList<String>();

        String irbNums = getOrCreateProjectInformation().getIrb();
        if ( StringUtils.isNotBlank( irbNums ) ) {
            StringTokenizer stringTokenizer = new StringTokenizer(irbNums);
            while ( stringTokenizer.hasMoreTokens()) {
                result.add( stringTokenizer.nextToken() );
            }
        }
        return result;
    }

    public void setIrbNumbers(final List<String> irbNumbers) {
        StringBuilder stringBuilder = new StringBuilder("");
        if ( irbNumbers != null ) {
            int i = 0;
            for ( String irbNumber : irbNumbers) {
                if ( i > 0 ) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(irbNumber);
                i++;
            }
            getOrCreateProjectInformation().setIrb( stringBuilder.toString() );
        }
    }

    @Override
    public Collection<Person> getProgramProjectManagers() {
        List<Person> result = new ArrayList<Person>();
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        String programPms = projectInformation.getProgramProjectManagers();
        if ( StringUtils.isNotBlank(programPms) ) {
            StringTokenizer stringTokenizer = new StringTokenizer( programPms );
            while ( stringTokenizer.hasMoreTokens()) {
                String programPMUsername = stringTokenizer.nextToken();
                Person person = new Person( programPMUsername, RoleType.PROGRAM_PM );
                result.add(person);
            }
        }
        return result;
    }


    public void setProgramProjectManagers(final Collection<Person> programPmPeople) {
        if ( programPmPeople != null ) {
            StringBuilder stringBuilder = new StringBuilder("");
            int i = 0;
            for ( Person pmPerson : programPmPeople) {
                if ( (pmPerson != null) && StringUtils.isNotBlank(pmPerson.getUsername()) ) {
                    if ( i > 0 ) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(pmPerson.getUsername());
                    i++;
                } else {
                    String msg = ( pmPerson != null ? pmPerson.getFirstName() + " " + pmPerson.getLastName() : "Null ProgramPM");
                    logger.error("Program PM has no username : " + msg );
                }
            }
            getOrCreateProjectInformation().setProgramProjectManagers( stringBuilder.toString() );
        }
    }


    public String getSynopsis() {
        return getOrCreateProjectInformation().getExperimentGoals();
    }

    public void setSynopsis(final String synopsis) {
        getOrCreateProjectInformation().setExperimentGoals(synopsis);
    }


    public String getDiseaseName() {
        return getOrCreateProjectInformation().getDiseaseName();
    }
    public void setDiseaseName(final String diseaseName) {
        getOrCreateProjectInformation().setDiseaseName(diseaseName);
    }

    public String getDiseaseCommonName() {
        return getOrCreateProjectInformation().getCommonName();
    }
    public void setDiseaseCommonName(final String diseaseCommonName) {
        getOrCreateProjectInformation().setCommonName(diseaseCommonName);
    }


    public String getSeqQuoteId() {
        return getOrCreateFundingInformation().getSequencingQuoteID();
    }
    public void setSeqQuoteId(final String seqQuoteId) {
        getOrCreateFundingInformation().setSequencingQuoteID(seqQuoteId);
    }

    public String getBspQuoteId() {
        return getOrCreateFundingInformation().getBspPlatingQuoteID();
    }
    public void setBspQuoteId(final String bspQuoteId) {
        getOrCreateFundingInformation().setBspPlatingQuoteID(bspQuoteId);
    }




    public OrganismName getOrganism() {
        if ( organism != null ) {
            return organism;
        }
        long organismId = getOrCreateProjectInformation().getOrganismID();
        if ( organismId > 0L ) {
            //TODO hmc lookup organism full name in cache given by the Id
            organism = new OrganismName("Unknown", "Unknown", organismId);
            throw new IllegalStateException( "Not completed yet.");
        }
        return organism;
    }

    public void setOrganism(final OrganismName organism) {
        this.organism = organism;
        long orgId = organism.getId();
        getOrCreateProjectInformation().setOrganismID(orgId);
    }


    public ReferenceSequenceName getReferenceSequenceName() {
        if ( referenceSequenceName != null) {
            return referenceSequenceName;
        }
        long refSeqId = getOrCreateCoverageAndAnalysisInformation().getReferenceSequenceId();
        if ( refSeqId > 0L ) {
            //TODO hmc lookup reference sequence full name in cache given by the Id ?
            referenceSequenceName = new ReferenceSequenceName("Unknown", refSeqId);
            throw new IllegalStateException( "Not completed yet.");
        }
        return referenceSequenceName;
    }

    public void setReferenceSequenceName(final ReferenceSequenceName referenceSequenceName) {
        this.referenceSequenceName = referenceSequenceName;
        long refSeqId = referenceSequenceName.getId();
        getOrCreateCoverageAndAnalysisInformation().setReferenceSequenceId( refSeqId );
    }

    public AnalysisPipelineType getAnalysisPipelineType() {
        return getOrCreateCoverageAndAnalysisInformation().getAnalysisPipeline();
    }
    public void setAnalysisPipelineType(final AnalysisPipelineType analysisPipelineType) {
        getOrCreateCoverageAndAnalysisInformation().setAnalysisPipeline(analysisPipelineType);
    }

    public AlignerType getAlignerType() {
        //TODO Need to display BWA for WGS and Directed pasTypes and need to display TopHat for RNASeq pass type.


        return getOrCreateCoverageAndAnalysisInformation().getAligner();
    }

    public void setAlignerType(final AlignerType alignerType) {

        if (PMBPassType.RNASeq.equals(passType)) {
            throw new IllegalStateException("Not implemented yet. TopHat aligner should be supported for RNASeq. Todo for Squid.");
        }
        if (AlignerType.MAQ.equals(alignerType)) {
            throw new IllegalStateException(AlignerType.MAQ.toString() + " aligner type no longer supported. Please use BWA.");
        }

        getOrCreateCoverageAndAnalysisInformation().setAligner( alignerType );
    }


    @Override
    public void associateWithResearchProject(final ResearchProject researchProject) {

        if ( researchProject != null ) {
            // Add this experiment to the list referred to by the research Project
            researchProject.addExperimentRequest(this);

            // Update the RP id that is associated with the research project.
            getOrCreateAbstractPass().setResearchProject("" + researchProject.getId().longValue());
        }
    }

    @Override
    public ExperimentRequest save() {
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ExperimentRequest exportToExcel() {
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Name getExperimentStatus() {
         Name state = ExperimentRequestSummary.DRAFT_STATUS;
         if (  pass != null && pass.getStatus() != null) {
             state = new Name( pass.getStatus().value() );
         }
         return state;
     }


    public PMBPassType getPassType() {
        return passType;
    }


    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
     }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public List<String> validate(final SquidTopicPortype squidServicePort) throws ValidationException {
        List<String>  errorMessages = new ArrayList<String>();

        String identifier = (getRemoteId() != null) ? getRemoteId().value : getLocalId().value;

        // Check for zero samples
        if (getOrCreateAbstractPass().getSampleDetailsInformation().getSample().isEmpty()) {
            throw new ValidationException("No Samples found in experiment request " +  identifier);
        }

        //TODO hmc Add validation logic for submitting a pass

        PassCritique passCritique  = squidServicePort.validatePass( getOrCreateAbstractPass() );
        errorMessages = passCritique.getErrors();

        return errorMessages;

    }

    public String submit( final SquidTopicPortype squidServicePort) throws SubmissionException {
        List<String>  errorMessages = new ArrayList<String>();

        String identifier = (getRemoteId() != null) ? getRemoteId().value : getLocalId().value;

        //TODO Under construction !!!!

        String passNumber = squidServicePort.storePass(pass);

        if ( StringUtils.isBlank(passNumber ) ) {
            throw new SubmissionException("No Pass number receive back from SQUID after submission of " + identifier);
        }

        setRemoteId( new RemoteId( passNumber ) );

        return passNumber;

    }

}


