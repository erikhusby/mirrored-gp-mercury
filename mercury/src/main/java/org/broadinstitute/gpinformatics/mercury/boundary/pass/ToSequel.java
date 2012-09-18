package org.broadinstitute.gpinformatics.mercury.boundary.pass;

import org.broadinstitute.gpinformatics.mercury.boundary.*;

/**
 * Utility class to map from Squid JAXB DTOs (classes in {@link org.broadinstitute.gpinformatics.mercury.boundary.squid}) to their
 * SequeL equivalents (classes in {@link org.broadinstitute.gpinformatics.mercury.boundary}).
 *
 */
public class ToSequel {

    public static ReferenceSequenceListResult sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.ReferenceSequenceListResult squidList) {

        ReferenceSequenceListResult sequelList = new ReferenceSequenceListResult();

        for (org.broadinstitute.gpinformatics.mercury.boundary.squid.ReferenceSequence squidSequence : squidList.getReferenceSequenceList()) {
            ReferenceSequence sequelSequence = new ReferenceSequence();
            sequelSequence.setActive(squidSequence.isActive());
            sequelSequence.setAlias(squidSequence.getAlias());
            sequelSequence.setId(squidSequence.getId());

            sequelList.getReferenceSequenceList().add(sequelSequence);

        }


        return sequelList;
    }



    public static BaitSetListResult sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.BaitSetListResult squidList) {

        BaitSetListResult sequelList = new BaitSetListResult();

        for (org.broadinstitute.gpinformatics.mercury.boundary.squid.BaitSet baitSet : squidList.getBaitSetList()) {
            BaitSet sequelBaitSet = new BaitSet();

            sequelBaitSet.setActive(baitSet.isActive());
            sequelBaitSet.setDesignName(baitSet.getDesignName());
            sequelBaitSet.setDesignType(BaitSetDesignType.fromValue(baitSet.getDesignType().value()));
            sequelBaitSet.setId(baitSet.getId());

            sequelList.getBaitSetList().add(sequelBaitSet);

        }

        return sequelList;
    }



    public static SummarizedPassListResult sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.SummarizedPassListResult squidList) {

        SummarizedPassListResult ret = new SummarizedPassListResult();

        for (org.broadinstitute.gpinformatics.mercury.boundary.squid.SummarizedPass summarizedPass : squidList.getSummarizedPassList()) {

            SummarizedPass sequelSummarizedPass = new SummarizedPass();
// R3_725
//            sequelSummarizedPass.setCreatedDate(summarizedPass.getCreatedDate());
            sequelSummarizedPass.setCreator(summarizedPass.getCreator());
            sequelSummarizedPass.setLastAcceptedVersion(summarizedPass.getLastAcceptedVersion());
            sequelSummarizedPass.setLastModified(summarizedPass.getLastModified());
            sequelSummarizedPass.setNickname(summarizedPass.getNickname());
            sequelSummarizedPass.setPassNumber(summarizedPass.getPassNumber());
            sequelSummarizedPass.setResearchProject(summarizedPass.getResearchProject());
            sequelSummarizedPass.setStatus(PassStatus.fromValue(summarizedPass.getStatus().value()));
            sequelSummarizedPass.setTitle(summarizedPass.getTitle());

            /* R3_725 we get RNASeq back from test Squid, but that's not in the R3_724 XSD so null check needed */
            if ( summarizedPass.getType() != null )

                sequelSummarizedPass.setType(PassType.fromValue(summarizedPass.getType().value()));

            sequelSummarizedPass.setUpdatedBy(summarizedPass.getUpdatedBy());
            sequelSummarizedPass.setVersion(summarizedPass.getVersion());

            ret.getSummarizedPassList().add(sequelSummarizedPass);

        }

        return ret;

    }
    
    
    
    public static OrganismListResult sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.OrganismListResult squidList) {

        OrganismListResult ret = new OrganismListResult();

        for (org.broadinstitute.gpinformatics.mercury.boundary.squid.Organism organism : squidList.getOrganismList()) {

            Organism sequelOrganism = new Organism();
            sequelOrganism.setCommonName(organism.getCommonName());
            sequelOrganism.setGenus(organism.getGenus());
            sequelOrganism.setId(organism.getId());
            sequelOrganism.setSpecies(organism.getSpecies());

            ret.getOrganismList().add(sequelOrganism);
        }

        return ret;

    }


    public static SquidPersonList sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.SquidPersonList squidList) {

        if (squidList == null)
            return null;

        SquidPersonList ret = new SquidPersonList();

        for (org.broadinstitute.gpinformatics.mercury.boundary.squid.SquidPerson squidPerson : squidList.getSquidPerson()) {

            SquidPerson sequelPerson = new SquidPerson();
            sequelPerson.setFirstName(squidPerson.getFirstName());
            sequelPerson.setLastName(squidPerson.getLastName());
            sequelPerson.setLogin(squidPerson.getLogin());
            sequelPerson.setPersonID(squidPerson.getPersonID());

            ret.getSquidPerson().add(sequelPerson);
        }

        return ret;
    }


    public static AbstractPass sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.AbstractPass squidPass) {

        AbstractPass ret;

        if (squidPass instanceof org.broadinstitute.gpinformatics.mercury.boundary.squid.WholeGenomePass) {
            ret = new WholeGenomePass();
        }

        else if (squidPass instanceof org.broadinstitute.gpinformatics.mercury.boundary.squid.DirectedPass) {

            org.broadinstitute.gpinformatics.mercury.boundary.squid.DirectedPass squidDirectedPass =
                    (org.broadinstitute.gpinformatics.mercury.boundary.squid.DirectedPass) squidPass;

            DirectedPass directedRet = new DirectedPass();
            ret = directedRet;

            directedRet.setBaitSetID(squidDirectedPass.getBaitSetID());
// R3_725
//            directedRet.setExomeExpress(squidDirectedPass.isExomeExpress());
        }
        else if (squidPass instanceof org.broadinstitute.gpinformatics.mercury.boundary.squid.RNASeqPass) {


            org.broadinstitute.gpinformatics.mercury.boundary.squid.RNASeqPass squidRNASeqPass =
                    (org.broadinstitute.gpinformatics.mercury.boundary.squid.RNASeqPass) squidPass;

            RNASeqPass rnaSeqRet = new RNASeqPass();
            ret = rnaSeqRet;

            rnaSeqRet.setTranscriptomeReferenceSequenceID(squidRNASeqPass.getTranscriptomeReferenceSequenceID());
            if (squidRNASeqPass.getProtocol() != null)
                rnaSeqRet.setProtocol(RNASeqProtocolType.fromValue(squidRNASeqPass.getProtocol().value()));
        }
        else
            throw new RuntimeException("Unrecognized PASS type: " + squidPass.getClass().getCanonicalName());


        ret.setAdditionalInformation(squidPass.getAdditionalInformation());


        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();
        ret.setCoverageAndAnalysisInformation(coverageAndAnalysisInformation);
        coverageAndAnalysisInformation.setAligner(AlignerType.fromValue(squidPass.getCoverageAndAnalysisInformation().getAligner().value()));
        coverageAndAnalysisInformation.setAnalysisPipeline(AnalysisPipelineType.fromValue(squidPass.getCoverageAndAnalysisInformation().getAnalysisPipeline().value()));
        coverageAndAnalysisInformation.setKeepFastQs(squidPass.getCoverageAndAnalysisInformation().isKeepFastQs());
        coverageAndAnalysisInformation.setPlex(squidPass.getCoverageAndAnalysisInformation().getPlex());
        coverageAndAnalysisInformation.setReferenceSequenceId(squidPass.getCoverageAndAnalysisInformation().getReferenceSequenceId());
        coverageAndAnalysisInformation.setSamplesPooled(squidPass.getCoverageAndAnalysisInformation().isSamplesPooled());

        if (squidPass.getCoverageAndAnalysisInformation().getAcceptedBasesCoverageModel() != null) {
            AcceptedBasesCoverageModel acceptedBasesCoverageModel = new AcceptedBasesCoverageModel();
            coverageAndAnalysisInformation.setAcceptedBasesCoverageModel(acceptedBasesCoverageModel);
            acceptedBasesCoverageModel.setCoverageDesired(squidPass.getCoverageAndAnalysisInformation().getAcceptedBasesCoverageModel().getCoverageDesired());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getAttemptedLanesCoverageModel() != null) {
            AttemptedLanesCoverageModel attemptedLanesCoverageModel = new AttemptedLanesCoverageModel();
            coverageAndAnalysisInformation.setAttemptedLanesCoverageModel(attemptedLanesCoverageModel);
            attemptedLanesCoverageModel.setAttemptedLanes(squidPass.getCoverageAndAnalysisInformation().getAttemptedLanesCoverageModel().getAttemptedLanes());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getAttemptedRegionsCoverageModel() != null) {
            AttemptedRegionsCoverageModel attemptedRegionsCoverageModel = new AttemptedRegionsCoverageModel();
            coverageAndAnalysisInformation.setAttemptedRegionsCoverageModel(attemptedRegionsCoverageModel);
            attemptedRegionsCoverageModel.setNumRegions(squidPass.getCoverageAndAnalysisInformation().getAttemptedRegionsCoverageModel().getNumRegions());
            attemptedRegionsCoverageModel.setPtpType(PTPType.fromValue(squidPass.getCoverageAndAnalysisInformation().getAttemptedRegionsCoverageModel().getPtpType().value()));
        }
        if (squidPass.getCoverageAndAnalysisInformation().getMeanTargetCoverageModel() != null) {
            MeanTargetCoverageModel meanTargetCoverageModel = new MeanTargetCoverageModel();
            coverageAndAnalysisInformation.setMeanTargetCoverageModel(meanTargetCoverageModel);
            meanTargetCoverageModel.setCoverageDesired(squidPass.getCoverageAndAnalysisInformation().getMeanTargetCoverageModel().getCoverageDesired());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getPfAlignedBasesCoverageModel() != null) {
            PFAlignedBasesCoverageModel pfAlignedBasesCoverageModel = new PFAlignedBasesCoverageModel();
            coverageAndAnalysisInformation.setPfAlignedBasesCoverageModel(pfAlignedBasesCoverageModel);
            pfAlignedBasesCoverageModel.setCoverageDesired(squidPass.getCoverageAndAnalysisInformation().getPfAlignedBasesCoverageModel().getCoverageDesired());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getPfBasesCoverageModel() != null) {
            PFBasesCoverageModel pfBasesCoverageModel = new PFBasesCoverageModel();
            coverageAndAnalysisInformation.setPfBasesCoverageModel(pfBasesCoverageModel);
            pfBasesCoverageModel.setCoverageDesired(squidPass.getCoverageAndAnalysisInformation().getPfBasesCoverageModel().getCoverageDesired());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getPfReadsCoverageModel() != null) {
            PFReadsCoverageModel pfReadsCoverageModel = new PFReadsCoverageModel();
            coverageAndAnalysisInformation.setPfReadsCoverageModel(pfReadsCoverageModel);
            pfReadsCoverageModel.setReadsDesired(squidPass.getCoverageAndAnalysisInformation().getPfReadsCoverageModel().getReadsDesired());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getPhysicalCoverageModel() != null) {
            PhysicalCoverageModel physicalCoverageModel = new PhysicalCoverageModel();
            coverageAndAnalysisInformation.setPhysicalCoverageModel(physicalCoverageModel);
            physicalCoverageModel.setCoverageDesired(squidPass.getCoverageAndAnalysisInformation().getPhysicalCoverageModel().getCoverageDesired());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getProgramPseudoDepthCoverageModel() != null) {
            ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel = new ProgramPseudoDepthCoverageModel();
            coverageAndAnalysisInformation.setProgramPseudoDepthCoverageModel(programPseudoDepthCoverageModel);
            programPseudoDepthCoverageModel.setCoverageDesired(squidPass.getCoverageAndAnalysisInformation().getProgramPseudoDepthCoverageModel().getCoverageDesired());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getQ20BasesCoverageModel() != null) {
            Q20BasesCoverageModel q20BasesCoverageModel = new Q20BasesCoverageModel();
            coverageAndAnalysisInformation.setQ20BasesCoverageModel(q20BasesCoverageModel);
            q20BasesCoverageModel.setCoverageDesired(squidPass.getCoverageAndAnalysisInformation().getQ20BasesCoverageModel().getCoverageDesired());
        }
        if (squidPass.getCoverageAndAnalysisInformation().getTargetCoverageModel() != null) {
            PhysicalCoverageModel physicalCoverageModel = new PhysicalCoverageModel();
            coverageAndAnalysisInformation.setPhysicalCoverageModel(physicalCoverageModel);
            physicalCoverageModel.setCoverageDesired(physicalCoverageModel.getCoverageDesired());
        }


        ret.setCreator(squidPass.getCreator());

        FundingInformation fundingInformation = new FundingInformation();
        ret.setFundingInformation(fundingInformation);
        fundingInformation.setBspPlatingQuoteID(squidPass.getFundingInformation().getBspPlatingQuoteID());
        fundingInformation.setFundingAgencies(squidPass.getFundingInformation().getFundingAgencies());
        fundingInformation.setSequencingQuoteID(squidPass.getFundingInformation().getSequencingQuoteID());
// R3_725
//        fundingInformation.setBspPriceItem(sequelize(squidPass.getFundingInformation().getBspPriceItem()));
//        fundingInformation.setGspPriceItem(sequelize(squidPass.getFundingInformation().getGspPriceItem()));

        ProjectInformation projectInformation = new ProjectInformation();
        ret.setProjectInformation(projectInformation);
        projectInformation.setAnalysisContacts(squidPass.getProjectInformation().getAnalysisContacts());

        if (squidPass.getProjectInformation().getBspPlatingOptions() != null) {
            BSPPlatingOptions bspPlatingOptions = new BSPPlatingOptions();
            projectInformation.setBspPlatingOptions(bspPlatingOptions);
            bspPlatingOptions.setCanDepleteStocks(squidPass.getProjectInformation().getBspPlatingOptions().isCanDepleteStocks());
            bspPlatingOptions.setCanTake1UL(squidPass.getProjectInformation().getBspPlatingOptions().isCanTake1UL());
        }

        projectInformation.setCommonName(squidPass.getProjectInformation().getCommonName());
        projectInformation.setControlledAccess(squidPass.getProjectInformation().isControlledAccess());
        projectInformation.setDateCreated(squidPass.getProjectInformation().getDateCreated());
        projectInformation.setDiseaseName(squidPass.getProjectInformation().getDiseaseName());
        projectInformation.setExperimentGoals(squidPass.getProjectInformation().getExperimentGoals());
        projectInformation.setIrb(squidPass.getProjectInformation().getIrb());
        projectInformation.setLastAcceptedVersion(squidPass.getProjectInformation().getLastAcceptedVersion());
        projectInformation.setLastModified(squidPass.getProjectInformation().getLastModified());
        projectInformation.setNickname(squidPass.getProjectInformation().getNickname());
        projectInformation.setOrganismID(squidPass.getProjectInformation().getOrganismID());
        projectInformation.setPassNumber(squidPass.getProjectInformation().getPassNumber());
        projectInformation.setPlatformProjectManagers(sequelize(squidPass.getProjectInformation().getPlatformProjectManagers()));
        projectInformation.setPlatformProjectNumber(squidPass.getProjectInformation().getPlatformProjectNumber());
        projectInformation.setProgramProjectManagers(squidPass.getProjectInformation().getProgramProjectManagers());
        projectInformation.setSequencingTechnology(SequencingTechnology.fromValue(squidPass.getProjectInformation().getSequencingTechnology().value()));
        projectInformation.setSponsoringScientists(sequelize(squidPass.getProjectInformation().getSponsoringScientists()));
        projectInformation.setTitle(squidPass.getProjectInformation().getTitle());
        projectInformation.setVersion(squidPass.getProjectInformation().getVersion());


        ret.setResearchProject(squidPass.getResearchProject());
        ret.setSampleDetailsInformation(sequelize(squidPass.getSampleDetailsInformation()));
        ret.setStatus(PassStatus.fromValue(squidPass.getStatus().value()));


        SubmissionsInformation submissionsInformation = new SubmissionsInformation();
        ret.setSubmissionsInformation(submissionsInformation);
        submissionsInformation.setDbGaPStudyName(squidPass.getSubmissionsInformation().getDbGaPStudyName());
        submissionsInformation.setPrimaryAgency(squidPass.getSubmissionsInformation().getPrimaryAgency());
        submissionsInformation.setSecondaryAgency(squidPass.getSubmissionsInformation().getSecondaryAgency());
        submissionsInformation.setSubmit(squidPass.getSubmissionsInformation().isSubmit());


        ret.setUpdatedBy(squidPass.getUpdatedBy());

        return ret;
    }

    private static SampleList sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.SampleList squidSamples) {
        if (squidSamples == null)
            return null;

        SampleList ret = new SampleList();

        for (org.broadinstitute.gpinformatics.mercury.boundary.squid.Sample squidSample : squidSamples.getSample()) {

            Sample sample = new Sample();
            ret.getSample().add(sample);

            sample.setBspSampleID(squidSample.getBspSampleID());
            sample.setNote(squidSample.getNote());

            if (squidSample.getValidation() != null) {
                SampleValidation validation = new SampleValidation();
                sample.setValidation(validation);

                if (squidSample.getValidation().getConcentration() != null)
                    validation.setConcentration(SampleValidationStatus.fromValue(squidSample.getValidation().getConcentration().value()));
                if (squidSample.getValidation().getTotalDNA() != null)
                    validation.setTotalDNA(SampleValidationStatus.fromValue(squidSample.getValidation().getTotalDNA().value()));
            }

            if (squidSample.getWorkRequests() != null) {
                WorkRequests workRequests = new WorkRequests();
                sample.setWorkRequests(workRequests);

                for (Integer wrid : squidSample.getWorkRequests().getWorkRequest())
                    workRequests.getWorkRequest().add(wrid);
            }

        }

        return ret;

    }


/*  R3_725

    private static PriceItem sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.PriceItem priceItem) {

        if (priceItem == null)
            return null;

        PriceItem ret = new PriceItem();
        ret.setCategoryName(priceItem.getCategoryName());
        ret.setId(priceItem.getId());
        ret.setName(priceItem.getName());
        ret.setPlatform(priceItem.getPlatform());
        ret.setPrice(priceItem.getPrice());
        ret.setUnits(priceItem.getUnits());

        return ret;
    }*/



    public static PassCritique sequelize(org.broadinstitute.gpinformatics.mercury.boundary.squid.PassCritique squidCritique) {

        if (squidCritique == null)
            return null;

        PassCritique critique = new PassCritique();

        for (String error : squidCritique.getErrors())
            critique.getErrors().add(error);


        return critique;
    }


}
