package org.broadinstitute.sequel.boundary.pass;


import org.broadinstitute.sequel.boundary.*;

import javax.enterprise.inject.Produces;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

public class PassTestDataProducer {


    private static PassTestDataProducer instance;


    public static PassTestDataProducer instance() {
        if (instance == null)
            instance = new PassTestDataProducer();
        return instance;
    }


    enum PlatformPM {
        SARA(10000000001838L, "Sara", "schauvin"),
        FERNANDO(10000000001498L, "Fernando", "fviloria"),
        LAUREN(10000000000625L, "Lauren", "laurena");


        private Long id;
        private String name;
        private String login;

        PlatformPM(Long id, String name, String login) {
            this.id = id;
            this.name = name;
            this.login = login;
        }

        public Long getId() {
            return id;
        }


        public String getName() {
            return name;
        }


        public String getLogin() {
            return login;
        }
    }


    private AbstractPass fleshOutAbstractPass(AbstractPass pass) {

        pass.setStatus(PassStatus.NEW);
        pass.setCreator("QADude");


        ProjectInformation projectInfo = new ProjectInformation();
        pass.setProjectInformation(projectInfo);
        projectInfo.setAnalysisContacts("Mary");
        projectInfo.setCommonName("a common name");
        projectInfo.setControlledAccess(true);
        projectInfo.setDateCreated(Calendar.getInstance());
        projectInfo.setDiseaseName("a disease");
        projectInfo.setExperimentGoals("some goals");
        projectInfo.setIrb("internal review board");
        projectInfo.setLastModified(Calendar.getInstance());
        projectInfo.setNickname("nick");
        projectInfo.setOrganismID(1L);
        projectInfo.setPlatformProjectNumber("G1");
        projectInfo.setProgramProjectManagers("Tom, Bob");
        projectInfo.setSequencingTechnology(SequencingTechnology.ILLUMINA);

        SquidPersonList squidPersonList;
        squidPersonList = new SquidPersonList();
        SquidPerson squidPerson;
        squidPerson = new SquidPerson();
        squidPerson.setPersonID(BigInteger.valueOf(11));
        squidPersonList.getSquidPerson().add(squidPerson);

        squidPerson = new SquidPerson();
        squidPerson.setPersonID(BigInteger.valueOf(42));
        squidPersonList.getSquidPerson().add(squidPerson);
        projectInfo.setSponsoringScientists(squidPersonList);


        squidPersonList = new SquidPersonList();
        squidPerson = new SquidPerson();

        squidPerson.setPersonID(BigInteger.valueOf(PlatformPM.LAUREN.getId()));
        squidPersonList.getSquidPerson().add(squidPerson);



        squidPerson = new SquidPerson();
        squidPerson.setPersonID(BigInteger.valueOf(PlatformPM.SARA.getId()));
        squidPersonList.getSquidPerson().add(squidPerson);

        projectInfo.setPlatformProjectManagers(squidPersonList);

        projectInfo.setTitle("title of pass");


        CoverageAndAnalysisInformation coverageAndAnalysisInfo = new CoverageAndAnalysisInformation();
        pass.setCoverageAndAnalysisInformation(coverageAndAnalysisInfo);

        coverageAndAnalysisInfo.setAligner(AlignerType.BWA);
        coverageAndAnalysisInfo.setAnalysisPipeline(AnalysisPipelineType.CANCER);
        coverageAndAnalysisInfo.setReferenceSequenceId(181L);
        coverageAndAnalysisInfo.setSamplesPooled(true);
        coverageAndAnalysisInfo.setPlex(BigDecimal.valueOf(88.1));
        coverageAndAnalysisInfo.setKeepFastQs(true);


        FundingInformation fundingInfo = new FundingInformation();
        pass.setFundingInformation(fundingInfo);
        fundingInfo.setBspPlatingQuoteID("BSP-QUOTE-1");
        fundingInfo.setSequencingQuoteID("SEQ-QUOTE-2");
        fundingInfo.setFundingAgencies("some agency");

        SubmissionsInformation submissionsInfo = new SubmissionsInformation();
        pass.setSubmissionsInformation(submissionsInfo);
        submissionsInfo.setDbGaPStudyName("dbgap name");
        submissionsInfo.setPrimaryAgency("agency #1");
        submissionsInfo.setSecondaryAgency("agency #2");
        submissionsInfo.setSubmit(true);

        SampleList sampleList = new SampleList();
        pass.setSampleDetailsInformation(sampleList);
        Sample sample = new Sample();
        sample.setBspSampleID("SM-18CJ5");
        sample.setNote("this is a note on the sample");

        sampleList.getSample().add(sample);

        return pass;

    }



    @Produces
    public DirectedPass produceDirectedPass() {

        DirectedPass directedPass = new DirectedPass();
        fleshOutAbstractPass(directedPass);

        directedPass.setBaitSetID(1L);

        CoverageAndAnalysisInformation coverageAndAnalysisInfo = directedPass.getCoverageAndAnalysisInformation();

        ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel = new ProgramPseudoDepthCoverageModel();
        programPseudoDepthCoverageModel.setCoverageDesired(BigInteger.valueOf(32));
        coverageAndAnalysisInfo.setProgramPseudoDepthCoverageModel(programPseudoDepthCoverageModel);

        return directedPass;
    }


    @Produces
    public WholeGenomePass produceWholeGenomePass() {

        WholeGenomePass wholeGenomePass = new WholeGenomePass();
        fleshOutAbstractPass(wholeGenomePass);

        CoverageAndAnalysisInformation coverageAndAnalysisInfo = wholeGenomePass.getCoverageAndAnalysisInformation();

        ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel = new ProgramPseudoDepthCoverageModel();
        programPseudoDepthCoverageModel.setCoverageDesired(BigInteger.valueOf(32));
        coverageAndAnalysisInfo.setProgramPseudoDepthCoverageModel(programPseudoDepthCoverageModel);

        return wholeGenomePass;

    }


    @Produces
    public RNASeqPass generateRNASeqPass() {

        RNASeqPass rnaseqPass = new RNASeqPass();
        fleshOutAbstractPass(rnaseqPass);

        rnaseqPass.setTranscriptomeReferenceSequenceID(1133L);

        CoverageAndAnalysisInformation coverageAndAnalysisInfo = rnaseqPass.getCoverageAndAnalysisInformation();

        PFReadsCoverageModel pfReadsCoverageModel = new PFReadsCoverageModel();
        pfReadsCoverageModel.setReadsDesired(BigInteger.valueOf(123456789L));
        coverageAndAnalysisInfo.setPfReadsCoverageModel(pfReadsCoverageModel);

        return rnaseqPass;
    }
}
