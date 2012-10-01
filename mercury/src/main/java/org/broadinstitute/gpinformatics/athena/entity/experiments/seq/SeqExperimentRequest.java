package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.bsp.BSPSample;
import org.broadinstitute.gpinformatics.athena.entity.bsp.SampleId;
import org.broadinstitute.gpinformatics.athena.entity.common.EntityUtils;
import org.broadinstitute.gpinformatics.athena.entity.common.QuoteId;
import org.broadinstitute.gpinformatics.athena.entity.experiments.*;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SubmissionException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.boundary.*;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:02 PM
 */
public abstract class SeqExperimentRequest extends AbstractExperimentRequest {

    private Log logger = LogFactory.getLog(SeqExperimentRequest.class);

    //    protected PassType passType;
    protected SeqCoverageModel seqCoverageModel;

    // Need the following to store lookup data since the pass only stores the integer id.
    private OrganismName organism;
    private ReferenceSequenceName referenceSequenceName;


    public SeqExperimentRequest(ExperimentRequestSummary experimentRequestSummary, ExperimentType experimentType) {
        super(experimentRequestSummary, experimentType);
//        this.passType = passType;
        // TODO set any defaults for other pass members ??
    }

    protected CoverageAndAnalysisInformation getOrCreateCoverageAndAnalysisInformation() {
        CoverageAndAnalysisInformation coverageAndAnalysisInformation;
        if ((null == getConcretePass().getCoverageAndAnalysisInformation())) {
            // Create default coverage model
            coverageAndAnalysisInformation = createDefaultCoverageModel();
            getConcretePass().setCoverageAndAnalysisInformation(coverageAndAnalysisInformation);
        } else {
            coverageAndAnalysisInformation = getConcretePass().getCoverageAndAnalysisInformation();
        }
        return coverageAndAnalysisInformation;
    }

    public SeqCoverageModel getSeqCoverageModel() {
        return seqCoverageModel;
    }

    /**
     * Implementing subclasses should always return a non-null concrete AbstractPass
     *
     * @return
     */

    protected abstract AbstractPass getConcretePass();


    public abstract Set<CoverageModelType> getCoverageModelTypes();

    protected abstract void setSeqCoverageModel(final SeqCoverageModel seqCoverageModel);


    protected abstract CoverageAndAnalysisInformation createDefaultCoverageModel();


    protected ProjectInformation getOrCreateProjectInformation() {
        ProjectInformation projectInformation;
        if ((null == getConcretePass().getProjectInformation())) {
            projectInformation = new ProjectInformation();
            getConcretePass().setProjectInformation(projectInformation);
        } else {
            projectInformation = getConcretePass().getProjectInformation();
        }
        return projectInformation;
    }

    protected FundingInformation getOrCreateFundingInformation() {
        FundingInformation fundingInformation;
        if ((null == getConcretePass().getFundingInformation())) {
            fundingInformation = new FundingInformation();
            getConcretePass().setFundingInformation(fundingInformation);
        } else {
            fundingInformation = getConcretePass().getFundingInformation();
        }
        return fundingInformation;
    }

    public SeqTechnology getSeqTechnology() {
        SeqTechnology seqTechnology = null;

        ProjectInformation projectInformation = getOrCreateProjectInformation();
        SequencingTechnology sequencingTechnology = projectInformation.getSequencingTechnology();
        if (null != sequencingTechnology) {
            // Convert one external enum type to local enum type by the common value
            seqTechnology = SeqTechnology.fromValue(sequencingTechnology.value());
        }
        return seqTechnology;
    }

    public void setSeqTechnology(final SeqTechnology seqTechnology) {
        if (seqTechnology == null) {
            throw new IllegalArgumentException(SequencingTechnology.class.getSimpleName() + " must be set to a valid value.");
        }
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        projectInformation.setSequencingTechnology(SequencingTechnology.fromValue(seqTechnology.value()));
        getConcretePass().setProjectInformation(projectInformation);
    }

    @Override
    public void setTitle(final String title) {
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        projectInformation.setTitle(getExperimentRequestSummary().getTitle());
        getConcretePass().setProjectInformation(projectInformation);
    }

    protected Set<Person> getOrCreatePersonsFromPass(SquidPersonList squidPersonList, RoleType roleType) {
        Set<Person> people = new HashSet<Person>();
        for (SquidPerson squidPerson : squidPersonList.getSquidPerson()) {
            if ((squidPerson != null) && (squidPerson.getPersonID() != null)) {
                Person person = new Person(
                        squidPerson.getLogin(),
                        squidPerson.getFirstName(),
                        squidPerson.getLastName());
                people.add(person);
            }
        }
        return people;
    }


    public Set<Person> getSponsoringScientists() {
        return getOrCreatePersonsFromPass(getOrCreateSponsoringScientists(), RoleType.BROAD_SCIENTIST);
    }

    /*
      The sponsoring scientists are associated with the Research Project
      but need to be set on the experiment request when submitting
     */
    public void setSponsoringScientists(final List<Person> sponsoringScientists) {

        ProjectInformation projectInformation = getOrCreateProjectInformation();

        SquidPersonList squidPersonList = getOrCreateSponsoringScientists();
        List<SquidPerson> squidPersons = squidPersonList.getSquidPerson();
        for (Person sponsoringScientist : sponsoringScientists) {
            SquidPerson squidPerson = new SquidPerson();
            squidPerson.setFirstName(sponsoringScientist.getFirstName());
            squidPerson.setLastName(sponsoringScientist.getLastName());
            squidPerson.setLogin(sponsoringScientist.getLogin());
            squidPersons.add(squidPerson);
        }
        projectInformation.setSponsoringScientists(squidPersonList);
    }

    private SquidPersonList getOrCreateSponsoringScientists() {
        SquidPersonList result = null;
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        if (null == projectInformation.getSponsoringScientists()) {
            SquidPersonList squidPersonList = new SquidPersonList();
            List<SquidPerson> squidPersons = squidPersonList.getSquidPerson();  //this call ensures list is initialized.
            projectInformation.setSponsoringScientists(squidPersonList);
        }
        result = projectInformation.getSponsoringScientists();
        return result;
    }

    private SquidPersonList getOrCreatePlatformProjectManagers() {
        SquidPersonList result = null;
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        if (null == projectInformation.getPlatformProjectManagers()) {
            SquidPersonList squidPersonList = new SquidPersonList();
            List<SquidPerson> squidPersons = squidPersonList.getSquidPerson();  //this call ensures list is initialized.
            projectInformation.setPlatformProjectManagers(squidPersonList);
        }
        result = projectInformation.getPlatformProjectManagers();
        return result;
    }


    @Override
    public void setRemoteId(final ExperimentId remoteId) {

//        RemoteId currentRemoteId = super.getExperimentRequestSummary().getExperimentId();
//        //Check against changing the remote Id of an experiment request with a new and different remoteId
//        if ( (currentRemoteId != null) &&
//                StringUtils.isNotBlank( currentRemoteId.value ) &&
//                (! currentRemoteId.value.equals(remoteId.value)) ) {
//            throw new IllegalStateException("Cannot overwrite the current remote Id <"+ currentRemoteId.value +
//                    "> with another different remote id <" + remoteId.value + ">." );
//        }
//        getExperimentRequestSummary().setExperimentId(remoteId);
        super.setRemoteId(remoteId);

        //Also ensure the remote id on the pass is up to date.
        if (StringUtils.isBlank(getOrCreateProjectInformation().getPassNumber())) {
            getOrCreateProjectInformation().setPassNumber(remoteId.value);
        }
    }


    public String getVersion() {
        return "" + getOrCreateProjectInformation().getVersion();
    }

    @Override
    public Set<Person> getPlatformProjectManagers() {
        // get the platform project managers list from the abstract pass the was received from squid.
        return getOrCreatePersonsFromPass(getOrCreatePlatformProjectManagers(), RoleType.PM);
    }

    @Override
    public Set<Person> getProgramProjectManagers() {
        Set<Person> result = new HashSet<Person>();
        ProjectInformation projectInformation = getOrCreateProjectInformation();
        String programPms = projectInformation.getProgramProjectManagers();
        if (StringUtils.isNotBlank(programPms)) {
            result = EntityUtils.extractPeopleFromUsernameList(programPms);
            //TODO - Do we need to lookup these usernames in a cache to get further info ??
        }
        return result;
    }

    @Override
    public void setProgramProjectManagers(final Set<Person> programPmPeople) {
        if (programPmPeople != null) {
            StringBuilder stringBuilder = new StringBuilder("");
            int i = 0;
            for (Person pmPerson : programPmPeople) {
                if ((pmPerson != null) && StringUtils.isNotBlank(pmPerson.getLogin())) {
                    if (i > 0) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(pmPerson.getLogin());
                    i++;
                } else {
                    String msg = (pmPerson != null ? pmPerson.getFirstName() + " " + pmPerson.getLastName() : "Null ProgramPM");
                    logger.error("Program PM has no username : " + msg);
                }
            }
            getOrCreateProjectInformation().setProgramProjectManagers(stringBuilder.toString());
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


    public QuoteId getSeqQuoteId() {
        return new QuoteId(getOrCreateFundingInformation().getSequencingQuoteID());
    }

    public void setSeqQuoteId(final QuoteId seqQuoteId) {
        getOrCreateFundingInformation().setSequencingQuoteID(seqQuoteId.value);
    }

    public QuoteId getBspQuoteId() {
        return new QuoteId(getOrCreateFundingInformation().getBspPlatingQuoteID());
    }

    public void setBspQuoteId(final QuoteId bspQuoteId) {
        getOrCreateFundingInformation().setBspPlatingQuoteID(bspQuoteId.value);
    }


    public OrganismName getOrganism() {
        if (organism != null) {
            return organism;
        }
        long organismId = getOrCreateProjectInformation().getOrganismID();
        if (organismId > 0L) {
            //TODO hmc lookup organism full name in cache given by the Id
            organism = new OrganismName("Unknown", "Unknown", organismId);
            throw new IllegalStateException("Not completed yet.");
        }
        return organism;
    }

    public void setOrganism(final OrganismName organism) {
        this.organism = organism;
        long orgId = organism.getId();
        getOrCreateProjectInformation().setOrganismID(orgId);
    }


    public ReferenceSequenceName getReferenceSequenceName() {
        if (referenceSequenceName != null) {
            return referenceSequenceName;
        }
        long refSeqId = getOrCreateCoverageAndAnalysisInformation().getReferenceSequenceId();
        if (refSeqId > 0L) {
            //TODO hmc lookup reference sequence full name in cache given by the Id ?
            referenceSequenceName = new ReferenceSequenceName("Unknown", refSeqId);
            throw new IllegalStateException("Not completed yet.");
        }
        return referenceSequenceName;
    }

    public void setReferenceSequenceName(final ReferenceSequenceName referenceSequenceName) {
        this.referenceSequenceName = referenceSequenceName;
        long refSeqId = referenceSequenceName.getId();
        getOrCreateCoverageAndAnalysisInformation().setReferenceSequenceId(refSeqId);
    }

    public AnalysisPipelineType getAnalysisPipelineType() {
        return getOrCreateCoverageAndAnalysisInformation().getAnalysisPipeline();
    }

    public void setAnalysisPipelineType(final AnalysisPipelineType analysisPipelineType) {
        getOrCreateCoverageAndAnalysisInformation().setAnalysisPipeline(analysisPipelineType);
    }

    public abstract AlignerType getAlignerType();

    public abstract void setAlignerType(final AlignerType alignerType);


    @Override
    public void associateWithResearchProject(final ResearchProject researchProject) {

        if (researchProject != null) {

            // Update the RP id that is associated with the research project.
            getConcretePass().setResearchProject("" + researchProject.getId());
        }
    }

    @Override
    public ExperimentRequest cloneRequest() {
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ExperimentRequest exportToExcel() {
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

//    public PassType getPassType() {
//        return passType;
//    }


    public List<String> validate(final SquidTopicPortype squidServicePort) throws ValidationException {
        List<String> errorMessages = new ArrayList<String>();

        String identifier = (getRemoteId() != null) ? getRemoteId().value : "null";

        // Check for zero samples
        if (getConcretePass().getSampleDetailsInformation().getSample().isEmpty()) {
            throw new ValidationException("No Samples found in experiment request " + identifier);
        }

        //TODO hmc Add any other validation logic for submitting a pass ??

        PassCritique passCritique = squidServicePort.validatePass(getConcretePass());
        errorMessages = passCritique.getErrors();

        return errorMessages;

    }


    @Override
    public List<BSPSample> getSamples() {
        List<BSPSample> bspSamples = new ArrayList<BSPSample>();

        if (! getConcretePass().getSampleDetailsInformation().getSample().isEmpty()) {
            for ( Sample sample : getConcretePass().getSampleDetailsInformation().getSample() ) {
                BSPSample bspSample = new BSPSample(new SampleId( sample.getBspSampleID()) );
                bspSamples.add(bspSample);
            }
        }

        return  bspSamples;
    }

    public String submit(final SquidTopicPortype squidServicePort) throws SubmissionException {
        List<String> errorMessages = new ArrayList<String>();

        String identifier = (getRemoteId() != null) ? getRemoteId().value : "null";

        String passNumber = squidServicePort.storePass(getConcretePass());

        if (StringUtils.isBlank(passNumber)) {
            throw new SubmissionException("No Pass number receive back from SQUID after submission of " + identifier);
        }

        setRemoteId(new ExperimentId(passNumber));

        return passNumber;

    }


//    protected abstract AbstractPass getOrCreateAbstractPass() {
//        if ((pass == null) && (passType != null)) {
//        // Create pass from pmbPassType.
//        switch (passType) {
//            case WG:
//                pass = new WholeGenomePass();
//                break;
//            case DIRECTED:
//                pass = new DirectedPass();
//                break;
//            case RNASeq:
//                pass = new RNASeqPass();
//                break;
//            default:
//                // in case the enum is extended but not this code.
//                throw new IllegalArgumentException("Unsupported Sequencing Pass Type. Supported values are : " + PassType.values().toString() );
//        }        }
//        return pass;
//    }


//    public PassType determinePassTypeFromPass(final AbstractPass pass) {
//        PassType result = null;
//        if ( pass != null ) {
//            if (pass instanceof WholeGenomePass) {
//                result = PassType.WG;
//            } else if ( pass instanceof DirectedPass) {
//                result = PassType.DIRECTED;
//            } else if (pass instanceof RNASeqPass ){
//                result = PassType.RNASeq;
//            } else {
//                throw new IllegalArgumentException("Unsupported type of PASS. Contents of pass : " + pass.toString() );
//            }
//        } else {
//            throw new IllegalArgumentException("Cannot determine type of Pass. Object was null");
//        }
//        return result;
//    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SeqExperimentRequest)) return false;
        if (!super.equals(o)) return false;

        final SeqExperimentRequest that = (SeqExperimentRequest) o;

        if (organism != null ? !organism.equals(that.organism) : that.organism != null) return false;
//        if (passType != that.passType) return false;
        if (referenceSequenceName != null ? !referenceSequenceName.equals(that.referenceSequenceName) : that.referenceSequenceName != null)
            return false;
        if (seqCoverageModel != null ? !seqCoverageModel.equals(that.seqCoverageModel) : that.seqCoverageModel != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
//        result = 31 * result + passType.hashCode();
        result = 31 * result + (seqCoverageModel != null ? seqCoverageModel.hashCode() : 0);
        result = 31 * result + (organism != null ? organism.hashCode() : 0);
        result = 31 * result + (referenceSequenceName != null ? referenceSequenceName.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return "SeqExperimentRequest{" +
                "seqCoverageModel=" + ((seqCoverageModel == null) ? "null" : seqCoverageModel.toString()) +
                ", organism=" + ((organism == null) ? "null" : organism.toString()) +
                ", referenceSequenceName=" + (referenceSequenceName == null ? "null" : referenceSequenceName.toString()) +
                ", " + super.toString() +
                '}';
    }

    protected void throwInvalidCoverageRuntimeException(CoverageModelType illegalCoverageModelType) {
        String invalidVal = (illegalCoverageModelType != null) ? illegalCoverageModelType.name() : "null";
        StringBuilder msg = new StringBuilder("Invalid coverage type " + invalidVal + ". " +
                "Valid coverage models are :");
        int i = 0;
        for (CoverageModelType coverageModelType : getCoverageModelTypes()) {
            if (i > 0) {
                msg.append(",");
            }
            ;
            msg.append(" ").append(coverageModelType.getFullName());
            i++;
        }
        throw new IllegalArgumentException(msg.toString());
    }

}


