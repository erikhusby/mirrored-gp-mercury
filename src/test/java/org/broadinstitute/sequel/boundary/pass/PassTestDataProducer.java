package org.broadinstitute.sequel.boundary.pass;


import org.broadinstitute.sequel.TestData;
import org.broadinstitute.sequel.boundary.*;

import javax.enterprise.inject.Produces;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

/**
 * Factory class for test data, might be used to {@link javax.inject.Inject} test data into a test class, though that
 * may only work well for one-test-per-class tests.
 */
public class PassTestDataProducer {

    //Test StartingSamples
    public static final String masterSample1 = "SM-1111";
    public static final String masterSample2 = "SM-2222";

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
        TargetCoverageModel targetCoverageModel = new TargetCoverageModel();
        targetCoverageModel.setCoveragePercentage(new BigInteger("80"));
        targetCoverageModel.setDepth(new BigInteger("20"));
        coverageAndAnalysisInfo.setTargetCoverageModel(targetCoverageModel);


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
        //sample.setBspSampleID("SM-18CJ5");
        sample.setBspSampleID(masterSample1);
        sample.setNote("this is a note on the sample");
        sampleList.getSample().add(sample);

        Sample sample2 = new Sample();
        sample2.setBspSampleID(masterSample2);
        sample2.setNote("this is a note on the sample2");
        sampleList.getSample().add(sample2);

        return pass;

    }



    @Produces
    @TestData
    public DirectedPass produceDirectedPass() {

        DirectedPass directedPass = new DirectedPass();
        directedPass = (DirectedPass)fleshOutAbstractPass(directedPass);

        directedPass.setBaitSetID(5L);

        CoverageAndAnalysisInformation coverageAndAnalysisInfo = directedPass.getCoverageAndAnalysisInformation();

        ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel = new ProgramPseudoDepthCoverageModel();
        programPseudoDepthCoverageModel.setCoverageDesired(BigInteger.valueOf(32));
        coverageAndAnalysisInfo.setProgramPseudoDepthCoverageModel(programPseudoDepthCoverageModel);

        directedPass.setExomeExpress(true);

        return directedPass;
    }


    @Produces
    @TestData
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
    @TestData
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
