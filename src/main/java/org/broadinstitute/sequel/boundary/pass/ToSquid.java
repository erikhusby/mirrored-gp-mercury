package org.broadinstitute.sequel.boundary.pass;


import org.broadinstitute.sequel.boundary.squid.*;

/**
 * Utility class to map from SequeL JAXB DTOs (classes in {@link org.broadinstitute.sequel.boundary}) to their
 * Squid equivalents (classes in {@link org.broadinstitute.sequel.boundary.squid}).
 *
 */
public class ToSquid {

    public static Organism squidify(org.broadinstitute.sequel.boundary.Organism organism) {

        Organism ret = new Organism();

        ret.setCommonName(organism.getCommonName());
        ret.setGenus(organism.getGenus());
        ret.setId(organism.getId());
        ret.setSpecies(organism.getSpecies());

        return ret;
    }



    public static BaitSet squidify(org.broadinstitute.sequel.boundary.BaitSet baitSet) {

        BaitSet ret = new BaitSet();

        ret.setActive(baitSet.isActive());
        ret.setDesignName(baitSet.getDesignName());
        ret.setDesignType(BaitSetDesignType.fromValue(baitSet.getDesignType().value()));
        ret.setId(baitSet.getId());

        return ret;

    }


    public static ReferenceSequence squidify(org.broadinstitute.sequel.boundary.ReferenceSequence referenceSequence) {

        ReferenceSequence ret = new ReferenceSequence();

        ret.setActive(referenceSequence.isActive());
        ret.setAlias(referenceSequence.getAlias());
        ret.setId(referenceSequence.getId());

        return ret;
    }


    public static AbstractPass squidify(org.broadinstitute.sequel.boundary.AbstractPass sequelPass) {
        AbstractPass ret;


        if (sequelPass instanceof org.broadinstitute.sequel.boundary.WholeGenomePass) {
            ret = new WholeGenomePass();
        }
        else if (sequelPass instanceof org.broadinstitute.sequel.boundary.DirectedPass) {

            org.broadinstitute.sequel.boundary.DirectedPass sequelDirectedPass =
                    (org.broadinstitute.sequel.boundary.DirectedPass) sequelPass;

            DirectedPass directedPass = new DirectedPass();
            ret = directedPass;

            directedPass.setBaitSetID(sequelDirectedPass.getBaitSetID());
            directedPass.setExomeExpress(sequelDirectedPass.isExomeExpress());
        }
        else if (sequelPass instanceof org.broadinstitute.sequel.boundary.RNASeqPass) {

            org.broadinstitute.sequel.boundary.RNASeqPass sequelRNASeqPass =
                    (org.broadinstitute.sequel.boundary.RNASeqPass) sequelPass;

            RNASeqPass rnaSeqPass = new RNASeqPass();
            ret = rnaSeqPass;

            rnaSeqPass.setTranscriptomeReferenceSequenceID(sequelRNASeqPass.getTranscriptomeReferenceSequenceID());

            if (sequelRNASeqPass.getProtocol() != null)
                rnaSeqPass.setProtocol(RNASeqProtocolType.fromValue(sequelRNASeqPass.getProtocol().value()));

        }
        else {
            throw new RuntimeException("Unrecognized PASS class: " + sequelPass.getClass());
        }


        ret.setAdditionalInformation(sequelPass.getAdditionalInformation());

        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();
        ret.setCoverageAndAnalysisInformation(coverageAndAnalysisInformation);

        coverageAndAnalysisInformation.setAligner(AlignerType.fromValue(sequelPass.getCoverageAndAnalysisInformation().getAligner().value()));
        coverageAndAnalysisInformation.setAnalysisPipeline(AnalysisPipelineType.fromValue(sequelPass.getCoverageAndAnalysisInformation().getAnalysisPipeline().value()));
        coverageAndAnalysisInformation.setKeepFastQs(sequelPass.getCoverageAndAnalysisInformation().isKeepFastQs());
        coverageAndAnalysisInformation.setPlex(sequelPass.getCoverageAndAnalysisInformation().getPlex());
        coverageAndAnalysisInformation.setReferenceSequenceId(sequelPass.getCoverageAndAnalysisInformation().getReferenceSequenceId());
        coverageAndAnalysisInformation.setSamplesPooled(sequelPass.getCoverageAndAnalysisInformation().isSamplesPooled());

        if (sequelPass.getCoverageAndAnalysisInformation().getAcceptedBasesCoverageModel() != null) {
            AcceptedBasesCoverageModel acceptedBasesCoverageModel = new AcceptedBasesCoverageModel();
            ret.getCoverageAndAnalysisInformation().setAcceptedBasesCoverageModel(acceptedBasesCoverageModel);
            acceptedBasesCoverageModel.setCoverageDesired(sequelPass.getCoverageAndAnalysisInformation().getAcceptedBasesCoverageModel().getCoverageDesired());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getAttemptedLanesCoverageModel() != null) {
            AttemptedLanesCoverageModel attemptedLanesCoverageModel = new AttemptedLanesCoverageModel();
            ret.getCoverageAndAnalysisInformation().setAttemptedLanesCoverageModel(attemptedLanesCoverageModel);
            attemptedLanesCoverageModel.setAttemptedLanes(sequelPass.getCoverageAndAnalysisInformation().getAttemptedLanesCoverageModel().getAttemptedLanes());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getAttemptedRegionsCoverageModel() != null) {
            AttemptedRegionsCoverageModel attemptedRegionsCoverageModel = new AttemptedRegionsCoverageModel();
            ret.getCoverageAndAnalysisInformation().setAttemptedRegionsCoverageModel(attemptedRegionsCoverageModel);
            attemptedRegionsCoverageModel.setNumRegions(sequelPass.getCoverageAndAnalysisInformation().getAttemptedRegionsCoverageModel().getNumRegions());
            attemptedRegionsCoverageModel.setPtpType(PTPType.fromValue(sequelPass.getCoverageAndAnalysisInformation().getAttemptedRegionsCoverageModel().getPtpType().value()));
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getMeanTargetCoverageModel() != null) {
            MeanTargetCoverageModel meanTargetCoverageModel = new MeanTargetCoverageModel();
            ret.getCoverageAndAnalysisInformation().setMeanTargetCoverageModel(meanTargetCoverageModel);
            meanTargetCoverageModel.setCoverageDesired(sequelPass.getCoverageAndAnalysisInformation().getMeanTargetCoverageModel().getCoverageDesired());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getPfAlignedBasesCoverageModel() != null) {
            PFAlignedBasesCoverageModel pfAlignedBasesCoverageModel = new PFAlignedBasesCoverageModel();
            ret.getCoverageAndAnalysisInformation().setPfAlignedBasesCoverageModel(pfAlignedBasesCoverageModel);
            pfAlignedBasesCoverageModel.setCoverageDesired(sequelPass.getCoverageAndAnalysisInformation().getPfAlignedBasesCoverageModel().getCoverageDesired());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getPfBasesCoverageModel() != null) {
            PFBasesCoverageModel pfBasesCoverageModel = new PFBasesCoverageModel();
            ret.getCoverageAndAnalysisInformation().setPfBasesCoverageModel(pfBasesCoverageModel);
            pfBasesCoverageModel.setCoverageDesired(sequelPass.getCoverageAndAnalysisInformation().getPfBasesCoverageModel().getCoverageDesired());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getPfReadsCoverageModel() != null) {
            PFReadsCoverageModel pfReadsCoverageModel = new PFReadsCoverageModel();
            ret.getCoverageAndAnalysisInformation().setPfReadsCoverageModel(pfReadsCoverageModel);
            pfReadsCoverageModel.setReadsDesired(sequelPass.getCoverageAndAnalysisInformation().getPfReadsCoverageModel().getReadsDesired());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getPhysicalCoverageModel() != null) {
            PhysicalCoverageModel physicalCoverageModel = new PhysicalCoverageModel();
            ret.getCoverageAndAnalysisInformation().setPhysicalCoverageModel(physicalCoverageModel);
            physicalCoverageModel.setCoverageDesired(sequelPass.getCoverageAndAnalysisInformation().getPhysicalCoverageModel().getCoverageDesired());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getProgramPseudoDepthCoverageModel() != null) {
            ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel = new ProgramPseudoDepthCoverageModel();
            ret.getCoverageAndAnalysisInformation().setProgramPseudoDepthCoverageModel(programPseudoDepthCoverageModel);
            programPseudoDepthCoverageModel.setCoverageDesired(sequelPass.getCoverageAndAnalysisInformation().getProgramPseudoDepthCoverageModel().getCoverageDesired());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getQ20BasesCoverageModel() != null) {
            Q20BasesCoverageModel q20BasesCoverageModel = new Q20BasesCoverageModel();
            ret.getCoverageAndAnalysisInformation().setQ20BasesCoverageModel(q20BasesCoverageModel);
            q20BasesCoverageModel.setCoverageDesired(sequelPass.getCoverageAndAnalysisInformation().getQ20BasesCoverageModel().getCoverageDesired());
        }
        if (sequelPass.getCoverageAndAnalysisInformation().getTargetCoverageModel() != null) {
            TargetCoverageModel targetCoverageModel = new TargetCoverageModel();
            ret.getCoverageAndAnalysisInformation().setTargetCoverageModel(targetCoverageModel);
            targetCoverageModel.setCoveragePercentage(sequelPass.getCoverageAndAnalysisInformation().getTargetCoverageModel().getCoveragePercentage());
            targetCoverageModel.setDepth(sequelPass.getCoverageAndAnalysisInformation().getTargetCoverageModel().getDepth());
        }


        ret.setCreator(sequelPass.getCreator());

        FundingInformation fundingInformation = new FundingInformation();
        ret.setFundingInformation(fundingInformation);

        fundingInformation.setBspPlatingQuoteID(sequelPass.getFundingInformation().getBspPlatingQuoteID());
        fundingInformation.setFundingAgencies(sequelPass.getFundingInformation().getFundingAgencies());
        fundingInformation.setSequencingQuoteID(sequelPass.getFundingInformation().getSequencingQuoteID());
        fundingInformation.setBspPriceItem(squidify(sequelPass.getFundingInformation().getBspPriceItem()));
        fundingInformation.setGspPriceItem(squidify(sequelPass.getFundingInformation().getGspPriceItem()));


        ProjectInformation projectInformation = new ProjectInformation();
        ret.setProjectInformation(projectInformation);

        projectInformation.setAnalysisContacts(sequelPass.getProjectInformation().getAnalysisContacts());
        projectInformation.setBspPlatingOptions(squidify(sequelPass.getProjectInformation().getBspPlatingOptions()));
        projectInformation.setCommonName(sequelPass.getProjectInformation().getCommonName());
        projectInformation.setControlledAccess(sequelPass.getProjectInformation().isControlledAccess());
        projectInformation.setDateCreated(sequelPass.getProjectInformation().getDateCreated());
        projectInformation.setDiseaseName(sequelPass.getProjectInformation().getDiseaseName());
        projectInformation.setExperimentGoals(sequelPass.getProjectInformation().getExperimentGoals());
        projectInformation.setIrb(sequelPass.getProjectInformation().getIrb());
        projectInformation.setLastAcceptedVersion(sequelPass.getProjectInformation().getLastAcceptedVersion());
        projectInformation.setLastModified(sequelPass.getProjectInformation().getLastModified());
        projectInformation.setNickname(sequelPass.getProjectInformation().getNickname());
        projectInformation.setOrganismID(sequelPass.getProjectInformation().getOrganismID());
        projectInformation.setPassNumber(sequelPass.getProjectInformation().getPassNumber());
        projectInformation.setPlatformProjectManagers(squidify(sequelPass.getProjectInformation().getPlatformProjectManagers()));
        projectInformation.setPlatformProjectNumber(sequelPass.getProjectInformation().getPlatformProjectNumber());
        projectInformation.setProgramProjectManagers(sequelPass.getProjectInformation().getProgramProjectManagers());
        projectInformation.setSequencingTechnology(SequencingTechnology.fromValue(sequelPass.getProjectInformation().getSequencingTechnology().value()));
        projectInformation.setSponsoringScientists(squidify(sequelPass.getProjectInformation().getSponsoringScientists()));
        projectInformation.setTitle(sequelPass.getProjectInformation().getTitle());
        projectInformation.setVersion(sequelPass.getProjectInformation().getVersion());

        ret.setResearchProject(sequelPass.getResearchProject());

        ret.setSampleDetailsInformation(squidify(sequelPass.getSampleDetailsInformation()));

        ret.setStatus(PassStatus.fromValue(sequelPass.getStatus().value()));

        SubmissionsInformation submissionsInformation = new SubmissionsInformation();
        ret.setSubmissionsInformation(submissionsInformation);

        submissionsInformation.setDbGaPStudyName(sequelPass.getSubmissionsInformation().getDbGaPStudyName());
        submissionsInformation.setPrimaryAgency(sequelPass.getSubmissionsInformation().getPrimaryAgency());
        submissionsInformation.setSecondaryAgency(sequelPass.getSubmissionsInformation().getSecondaryAgency());
        submissionsInformation.setSubmit(sequelPass.getSubmissionsInformation().isSubmit());

        ret.setUpdatedBy(sequelPass.getUpdatedBy());

        return ret;
    }



    private static SampleList squidify(org.broadinstitute.sequel.boundary.SampleList sequelSampleList) {

        if (sequelSampleList == null)
            return null;

        
        SampleList ret = new SampleList();

        for (org.broadinstitute.sequel.boundary.Sample sequelSample : sequelSampleList.getSample()) {

            Sample sample = new Sample();
            ret.getSample().add(sample);

            sample.setBspSampleID(sequelSample.getBspSampleID());
            sample.setNote(sequelSample.getNote());


            if (sequelSample.getValidation() != null) {
                SampleValidation validation = new SampleValidation();
                sample.setValidation(validation);

                if (sequelSample.getValidation().getConcentration() != null)
                    validation.setConcentration(SampleValidationStatus.fromValue(sequelSample.getValidation().getConcentration().value()));

                if (sequelSample.getValidation().getTotalDNA() != null)
                    validation.setTotalDNA(SampleValidationStatus.fromValue(sequelSample.getValidation().getTotalDNA().value()));

            }


            if (sequelSample.getWorkRequests() != null) {
                WorkRequests workRequests = new WorkRequests();
                sample.setWorkRequests(workRequests);

                for (Integer wrid : sequelSample.getWorkRequests().getWorkRequest())
                    workRequests.getWorkRequest().add(wrid);

            }
        }

        return ret;

    }

    private static SquidPersonList squidify(org.broadinstitute.sequel.boundary.SquidPersonList personList) {
        if (personList == null)
            return null;

        SquidPersonList ret = new SquidPersonList();

        for (org.broadinstitute.sequel.boundary.SquidPerson person : personList.getSquidPerson()) {
            SquidPerson squidPerson = new SquidPerson();
            ret.getSquidPerson().add(squidPerson);
            squidPerson.setFirstName(person.getFirstName());
            squidPerson.setLastName(person.getLastName());
            squidPerson.setLogin(person.getLogin());
            squidPerson.setPersonID(person.getPersonID());
        }

        return ret;

    }


    private static BSPPlatingOptions squidify(org.broadinstitute.sequel.boundary.BSPPlatingOptions sequelBSPPlatingOptions) {

        if (sequelBSPPlatingOptions == null)
            return null;

        BSPPlatingOptions ret = new BSPPlatingOptions();
        ret.setCanDepleteStocks(sequelBSPPlatingOptions.isCanDepleteStocks());
        ret.setCanTake1UL(sequelBSPPlatingOptions.isCanTake1UL());

        return ret;

    }



    private static PriceItem squidify(org.broadinstitute.sequel.boundary.PriceItem sequelPriceItem) {

        if (sequelPriceItem == null)
            return null;

        PriceItem priceItem = new PriceItem();
        priceItem.setCategoryName(sequelPriceItem.getCategoryName());
        priceItem.setId(sequelPriceItem.getId());
        priceItem.setName(sequelPriceItem.getName());
        priceItem.setPlatform(sequelPriceItem.getPlatform());
        priceItem.setPrice(sequelPriceItem.getPrice());
        priceItem.setUnits(sequelPriceItem.getUnits());

        return priceItem;
    }
}
