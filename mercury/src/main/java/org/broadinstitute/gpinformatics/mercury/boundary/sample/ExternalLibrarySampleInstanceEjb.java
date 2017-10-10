package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import clover.org.apache.commons.lang.math.NumberUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryMapped;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntityTsk;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.ExternalLibraryUploadActionBean;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExternalLibrarySampleInstanceEjb {
    private final String EZPASS = "ezpass";
    private List<List<String>> jiraSubTaskList = new ArrayList<List<String>>();
    private List<String> collaboratorSampleIds = new ArrayList<>();
    private List<String> collaboratorParticipantIds = new ArrayList<>();
    private List<String> broadParticipantIds = new ArrayList<>();
    private List<String> genders = new ArrayList<>();
    private List<String> species = new ArrayList<>();
    private List<String> lsids = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<String> sampleLibraryName = new ArrayList<>();
    private List<MolecularIndexingScheme> molecularIndexSchemes = new ArrayList<>();
    private List<ReagentDesign> reagents = new ArrayList<>();
    private List<MercurySample> mercurySamples = new ArrayList<>();
    private List<MercurySample> rootSamples = new ArrayList<>();
    private List<Boolean> sampleRegistrationFlag = new ArrayList<>();
    private List<ProductOrder> productOrders = new ArrayList<>();
    private List<ResearchProject> researchProjects = new ArrayList<>();
    private int rowOffset = 2;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private JiraService jiraService;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SampleInstanceEntityDao sampleInstanceEntityDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private SampleKitRequestDao sampleKitRequestDao;

    @Inject
    private MolecularIndexDao molecularIndexDao;

    /**
     * Build and return the collaborator metadata for the samples. (If supplied)
     */
    private Set<Metadata> collaboratorSampleIdMetadata(final int index) {
        return new HashSet<Metadata>() {{
            add(new Metadata(Metadata.Key.SAMPLE_ID, collaboratorSampleIds.get(index)));
            add(new Metadata(Metadata.Key.BROAD_SAMPLE_ID, broadParticipantIds.get(index)));
            add(new Metadata(Metadata.Key.PATIENT_ID, collaboratorParticipantIds.get(index)));
            add(new Metadata(Metadata.Key.GENDER, genders.get(index)));
            add(new Metadata(Metadata.Key.LSID, lsids.get(index)));
            add(new Metadata(Metadata.Key.SPECIES, species.get(index)));
        }};
    }

    /**
     * Save uploaded data after it hs been verified by verifySpreadSheet();
     */
    private void persistPooledTubeResults(VesselPooledTubesProcessor vesselSpreadsheetProcessor,
            MessageCollection messageCollection) {
        int sampleIndex = 0;

        for (String sampleId : vesselSpreadsheetProcessor.getBroadSampleId()) {
            LabVessel labVessel =
                    labVesselDao.findByIdentifier(vesselSpreadsheetProcessor.getBarcodes().get(sampleIndex));

            if (labVessel == null) {
                labVessel = new BarcodedTube(vesselSpreadsheetProcessor.getBarcodes().get(sampleIndex),
                        BarcodedTube.BarcodedTubeType.MatrixTube);
            }

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao
                    .findByName(vesselSpreadsheetProcessor.getSingleSampleLibraryName().get(sampleIndex));
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
            }

            //Check if the sample is registered in Mercury.
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleId);

            //Add the new registration metadata.
            if (!sampleRegistrationFlag.get(sampleIndex)) {
                if (mercurySample == null) {
                    mercurySample = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
                }
                mercurySample.addMetadata(collaboratorSampleIdMetadata(sampleIndex));
                mercurySample.addLabVessel(labVessel);
            }

            mercurySample.addLabVessel(labVessel);
            mercurySampleDao.persist(mercurySample);
            mercurySamples.add(mercurySample);

            sampleInstanceEntity.setSampleLibraryName(vesselSpreadsheetProcessor.getSingleSampleLibraryName()
                    .get(sampleIndex));
            sampleInstanceEntity.setReagentDesign(reagents.get(sampleIndex));
            sampleInstanceEntity.setMolecularIndexScheme(molecularIndexSchemes.get(sampleIndex));
            sampleInstanceEntity.setMercurySampleId(mercurySamples.get(sampleIndex));

            if (rootSamples.size() >= sampleIndex) {
                sampleInstanceEntity.setRootSample(rootSamples.get(sampleIndex));
            }

            sampleInstanceEntity.setExperiment(vesselSpreadsheetProcessor.getExperiment().get(sampleIndex));
            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setUploadDate();
            sampleInstanceEntity.removeSubTasks();

            //Persist the dev sub-tasks in the order they where provided.
            for (String subTask : jiraSubTaskList.get(sampleIndex)) {
                SampleInstanceEntityTsk sampleInstanceEntityTsk = new SampleInstanceEntityTsk();
                sampleInstanceEntityTsk.setSubTask(subTask);
                sampleInstanceEntity.addSubTasks(sampleInstanceEntityTsk);
            }

            sampleInstanceEntityDao.persist(sampleInstanceEntity);
            sampleInstanceEntityDao.flush();
            ++sampleIndex;
        }

        if (sampleIndex > 0) {
            messageCollection.addInfo("Spreadsheet with " + String.valueOf(sampleIndex) +
                    " rows successfully uploaded!");
        } else {
            messageCollection.addError("No valid data found.");
        }

    }

    /**
     * Collate sample metadata for all external library types
     */
    private Set<Metadata> setExternalMetaData(final String sex, final String strain, final String cellLine,
            final String sampleId, final String individualName, final String organism) {
        return new HashSet<Metadata>() {{
            add(new Metadata(Metadata.Key.GENDER, sex));
            add(new Metadata(Metadata.Key.STRAIN, strain));
            add(new Metadata(Metadata.Key.CELL_LINE, cellLine));
            add(new Metadata(Metadata.Key.SAMPLE_ID, sampleId));
            add(new Metadata(Metadata.Key.INDIVIDUAL_NAME, individualName));
            if (organism != null) {
                add(new Metadata(Metadata.Key.ORGANISM, organism));
            }
        }};
    }


    /**
     * Persist External library spreadsheet data to the database if it passes all the edit checks.
     */
    private void persistExternalLibraries(ExternalLibraryMapped vesselSpreadsheetProcessor,
            MessageCollection messageCollection, String spreadsheetType) {

        int sampleIndex = 0;
        String organism = null;
        SampleKitRequest sampleKitRequest = getSampleKit(vesselSpreadsheetProcessor.getOrganization());
        sampleKitRequest.setCollaboratorName(vesselSpreadsheetProcessor.getCollaboratorName());
        sampleKitRequest.setFirstName(vesselSpreadsheetProcessor.getFirstName());
        sampleKitRequest.setLastName(vesselSpreadsheetProcessor.getLastName());
        sampleKitRequest.setOrganization(vesselSpreadsheetProcessor.getOrganization());
        sampleKitRequest.setAddress(vesselSpreadsheetProcessor.getAddress());
        sampleKitRequest.setCity(vesselSpreadsheetProcessor.getCity());
        sampleKitRequest.setState(vesselSpreadsheetProcessor.getState());
        sampleKitRequest.setPostalCode(vesselSpreadsheetProcessor.getZip());
        sampleKitRequest.setCountry(vesselSpreadsheetProcessor.getCountry());
        sampleKitRequest.setPhone(vesselSpreadsheetProcessor.getPhone());
        sampleKitRequest.setEmail(vesselSpreadsheetProcessor.getEmail());
        sampleKitRequest.setCommonName(vesselSpreadsheetProcessor.getCommonName());
        sampleKitRequest.setGenus(vesselSpreadsheetProcessor.getGenus());
        sampleKitRequest.setSpecies(vesselSpreadsheetProcessor.getSpecies());
        sampleKitRequest.setIrbApprovalRequired(vesselSpreadsheetProcessor.getIrbRequired());

        for (String libraryName : vesselSpreadsheetProcessor.getSingleSampleLibraryName()) {
            LabVessel labVessel = getLabVessel(vesselSpreadsheetProcessor.getSingleSampleLibraryName()
                    .get(sampleIndex));
            if (messageCollection.hasErrors()) {
                return;
            }
            labVessel.setVolume(new BigDecimal(vesselSpreadsheetProcessor.getTotalLibraryVolume().get(sampleIndex)));
            labVessel.setConcentration(new BigDecimal(vesselSpreadsheetProcessor.getTotalLibraryConcentration()
                    .get(sampleIndex)));
            MercurySample mercurySample =
                    getMercurySample(vesselSpreadsheetProcessor.getCollaboratorSampleId().get(sampleIndex));
            if (spreadsheetType.equals(ExternalLibraryUploadActionBean.MULTI_ORG)) {
                organism = vesselSpreadsheetProcessor.getOrganism().get(sampleIndex);
            }
            mercurySample.addMetadata(setExternalMetaData(vesselSpreadsheetProcessor.getSex().get(sampleIndex),
                    vesselSpreadsheetProcessor.getStrain().get(sampleIndex),
                    vesselSpreadsheetProcessor.getStrain().get(sampleIndex),
                    vesselSpreadsheetProcessor.getCollaboratorSampleId().get(sampleIndex),
                    vesselSpreadsheetProcessor.getIndividualName().get(sampleIndex),
                    organism));
            mercurySample.addLabVessel(labVessel);
            mercurySampleDao.persist(mercurySample);

            SampleInstanceEntity sampleInstanceEntity = getSampleInstanceEntity(libraryName, sampleKitRequest);
            if (!spreadsheetType.equals(ExternalLibraryUploadActionBean.NON_POOLED)) {
                sampleInstanceEntity.setPooled(vesselSpreadsheetProcessor.getPooled().get(sampleIndex));
            }
            sampleInstanceEntity.setTissueType(vesselSpreadsheetProcessor.getTissueType().get(sampleIndex));
            sampleInstanceEntity.setSampleLibraryName(vesselSpreadsheetProcessor.getSingleSampleLibraryName()
                    .get(sampleIndex));
            sampleInstanceEntity.setInsertSizeRange(vesselSpreadsheetProcessor.getInsertSizeRangeBp()
                    .get(sampleIndex));
            sampleInstanceEntity.setLibrarySizeRange(vesselSpreadsheetProcessor.getLibrarySizeRangeBp()
                    .get(sampleIndex));
            sampleInstanceEntity.setJumpSize(vesselSpreadsheetProcessor.getJumpSize().get(sampleIndex));
            sampleInstanceEntity.setRestrictionEnzyme(vesselSpreadsheetProcessor.getRestrictionEnzymes()
                    .get(sampleIndex));
            if (!spreadsheetType.contains(EZPASS)) {
                sampleInstanceEntity.setDesiredReadLength(new Integer(WalkupSequencingResource.asNumber(
                        vesselSpreadsheetProcessor.getDesiredReadLength().get(sampleIndex))));
            }
            sampleInstanceEntity.setReferenceSequence(vesselSpreadsheetProcessor.getReferenceSequence()
                    .get(sampleIndex));
            MolecularIndex molecularIndex = molecularIndexDao.findBySequence(vesselSpreadsheetProcessor
                    .getMolecularBarcodeSequence().get(sampleIndex));
            if (molecularIndex != null) {
                sampleInstanceEntity.setMolecularIndexScheme(molecularIndex.getMolecularIndexingSchemes()
                        .iterator().next());
            }
            sampleInstanceEntity.setLibraryType(vesselSpreadsheetProcessor.getLibraryType().get(sampleIndex));
            sampleInstanceEntity.setResearchProject(researchProjects.get(sampleIndex));
            sampleInstanceEntity.setProductOrder(productOrders.get(sampleIndex));
            sampleInstanceEntity.setCollaboratorSampleId(mercurySample.getSampleKey());
            sampleInstanceEntity.setUploadDate();
            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setMercurySampleId(mercurySample);
            sampleInstanceEntityDao.persist(sampleInstanceEntity);
            ++sampleIndex;

            messageCollection.addInfo("Spreadsheet with " + String.valueOf(sampleIndex) +
                    " rows successfully uploaded!");
        }
    }

    /**
     * Verify required fields for External Libraries.
     */
    public void verifyExternalLibrary(ExternalLibraryMapped vesselSpreadsheetProcessor,
            MessageCollection messageCollection, boolean overWriteFlag, String spreadsheetType) {

        int index = 0;
        int displayIndex = ExternalLibraryUploadActionBean.externalLibraryRowOffset + 1;
        researchProjects.clear();
        productOrders.clear();
        for (String libraryName : vesselSpreadsheetProcessor.getSingleSampleLibraryName()) {
            if (spreadsheetType.equals(ExternalLibraryUploadActionBean.EZPASS_KIOSK)) {
                //TODO: Should we be validating and retaining SQUID AND GSSR Fields???
                validateRequiredFields(vesselSpreadsheetProcessor.getBarcodes().get(index),
                        ExternalLibraryMapped.Headers.TUBE_BARCODE.getText(), displayIndex, messageCollection);
                validateRequiredFields(vesselSpreadsheetProcessor.getSourceSampleGssrId().get(index),
                        ExternalLibraryMapped.Headers.SOURCE_SAMPLE_GSSR_ID.getText(), displayIndex, messageCollection);
                validateRequiredFields(vesselSpreadsheetProcessor.getSquidProject().get(index),
                        ExternalLibraryMapped.Headers.SQUID_PROJECT.getText(), displayIndex, messageCollection);
                validateRequiredFields(vesselSpreadsheetProcessor.getVirtualGssrId().get(index),
                        ExternalLibraryMapped.Headers.VIRTUAL_GSSR_ID.getText(), displayIndex, messageCollection);
            }
            if (!spreadsheetType.contains(EZPASS)) {
                validateRequiredFields(vesselSpreadsheetProcessor.getIrbNumber().get(index),
                        ExternalLibraryMapped.Headers.IRB_NUMBER.getText(), displayIndex, messageCollection);
                validateRequiredFields(vesselSpreadsheetProcessor.getDesiredReadLength().get(index),
                        ExternalLibraryMapped.Headers.DESIRED_READ_LENGTH.getText(), displayIndex, messageCollection);
            }
            validateRequiredFields(vesselSpreadsheetProcessor.getCollaboratorSampleId().get(index),
                    ExternalLibraryMapped.Headers.COLLABORATOR_SAMPLE_ID.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getIndividualName().get(index),
                    ExternalLibraryMapped.Headers.INDIVIDUAL_NAME.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getSingleSampleLibraryName().get(index),
                    ExternalLibraryMapped.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(), displayIndex,
                    messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getLibraryType().get(index),
                    ExternalLibraryMapped.Headers.LIBRARY_TYPE.getText(), displayIndex, messageCollection);
            if (!spreadsheetType.equals(ExternalLibraryUploadActionBean.NON_POOLED)) {
                validateRequiredFields(vesselSpreadsheetProcessor.getPooled().get(index),
                        ExternalLibraryMapped.Headers.POOLED.getText(), displayIndex, messageCollection);
            }
            validateRequiredFields(vesselSpreadsheetProcessor.getInsertSizeRangeBp().get(index),
                    ExternalLibraryMapped.Headers.INSERT_SIZE_RANGE_BP.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getLibrarySizeRangeBp().get(index),
                    ExternalLibraryMapped.Headers.LIBRARY_SIZE_RANGE_BP.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getTotalLibraryVolume().get(index),
                    ExternalLibraryMapped.Headers.TOTAL_LIBRARY_VOLUME.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getTotalLibraryConcentration().get(index),
                    ExternalLibraryMapped.Headers.TOTAL_LIBRARY_CONCENTRATION.getText(), displayIndex,
                    messageCollection);
            validateNumericFields(vesselSpreadsheetProcessor.getTotalLibraryVolume().get(index),
                    ExternalLibraryMapped.Headers.TOTAL_LIBRARY_VOLUME.getText(), displayIndex, messageCollection);
            validateNumericFields(vesselSpreadsheetProcessor.getTotalLibraryConcentration().get(index),
                    ExternalLibraryMapped.Headers.TOTAL_LIBRARY_CONCENTRATION.getText(), displayIndex,
                    messageCollection);

            if (spreadsheetType.equals(ExternalLibraryUploadActionBean.MULTI_ORG)) {
                validateRequiredFields(vesselSpreadsheetProcessor.getOrganism().get(index),
                        ExternalLibraryMapped.Headers.ORGANISM.getText(), displayIndex, messageCollection);
            }
            validateRequiredFields(vesselSpreadsheetProcessor.getFundingSource().get(index),
                    ExternalLibraryMapped.Headers.FUNDING_SOURCE.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getReferenceSequence().get(index),
                    ExternalLibraryMapped.Headers.REFERENCE_SEQUENCE.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getRequestedCompletionDate().get(index),
                    ExternalLibraryMapped.Headers.REQUESTED_COMPLETION_DATE.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getDataSubmission().get(index),
                    ExternalLibraryMapped.Headers.DATA_SUBMISSION.getText(), displayIndex, messageCollection);
            validateRequiredFields(vesselSpreadsheetProcessor.getDataAnalysisType().get(index),
                    ExternalLibraryMapped.Headers.DATA_ANALYSIS_TYPE.getText(), displayIndex, messageCollection);

            //Database validations.
            getMolIndex(vesselSpreadsheetProcessor.getMolecularBarcodeSequence().get(index), messageCollection,
                    displayIndex);
            String project = vesselSpreadsheetProcessor.getProjectTitle().get(index);

            String dataAnalysisType = vesselSpreadsheetProcessor.getDataAnalysisType().get(index);
            productOrders.add(getPdo(project, dataAnalysisType, displayIndex, messageCollection));
            researchProjects.add(getResearchProject(productOrders.get(index), displayIndex, messageCollection));

            if (!spreadsheetType.contains(EZPASS)) {
                String irbNumber = vesselSpreadsheetProcessor.getIrbNumber().get(index);
                validateIRB(researchProjects.get(index), irbNumber, displayIndex, messageCollection);
            }

            sampleExists(libraryName, overWriteFlag, messageCollection, displayIndex);
            displayIndex++;
            index++;
        }
        if (index < 1) {
            messageCollection.addError("No data found.");
        }
        if (!messageCollection.hasErrors()) {
            persistExternalLibraries(vesselSpreadsheetProcessor, messageCollection, spreadsheetType);
        }
    }

    /**
     * Check that required fields exist
     */
    private void validateRequiredFields(String value, String header, int index, MessageCollection messageCollection) {
        if (isFieldEmpty(value)) {
            messageCollection.addError(header + " was missing a required value at Row: " + (index));
        }
    }

    /**
     * Check if a field contains a valid number.
     */
    private void validateNumericFields(String value, String header, int index, MessageCollection messageCollection) {
        if (!NumberUtils.isNumber(value)) {
            messageCollection.addError(header + " is not a valid number. " + (index));
        }
    }

    /**
     * Verify the spreadsheet contents before attempting to persist data for pooled tubes (not external libraries)
     */
    public void verifyPooledTubes(VesselPooledTubesProcessor vesselSpreadsheetProcessor,
            MessageCollection messageCollection, boolean overWriteFlag) {
        //Is the sample library name unique to the spreadsheet??
        Map<String, String> map = new HashMap<String, String>();
        int mapIndex = 0;
        for (String libraryName : vesselSpreadsheetProcessor.getSingleSampleLibraryName()) {
            map.put(libraryName, libraryName);
            mapIndex++;
            if (mapIndex > map.size()) {
                messageCollection.addError("Single sample library name : " + libraryName +
                        " at Row: " + (mapIndex + 1) +
                        " Column: " + VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText() +
                        " must be unique");
            } else {
                sampleLibraryName.add(libraryName);
            }

            if (sampleInstanceEntityDao.findByName(libraryName) != null && !overWriteFlag) {
                messageCollection.addError("Single sample library name : " + libraryName +
                        " at Row: " + (mapIndex + 1) +
                        " Column: " + VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()
                        + " exists in the database. Please choose the overwrite previous upload option.");
            }
        }

        //Are Tubes registered.
        int barcodeIndex = 0;
        for (String barcode : vesselSpreadsheetProcessor.getBarcodes()) {
            if (barcode == null) {
                messageCollection.addError("Barcode not found: " + barcode.toString() +
                        " At Row: " + (barcodeIndex + rowOffset) +
                        " Column: " + VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText());
            } else if (labVesselDao.findByIdentifier(barcode) != null && !overWriteFlag) {
                messageCollection.addError("Barcode already registered: " + barcode.toString() +
                        " At Row: " + (barcodeIndex + rowOffset) +
                        " Column: " + VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText());
            } else {
                barcodes.add(barcode);
            }
            barcodeIndex++;
        }

        //Does molecular index scheme exist.
        int molecularIndexSchemeIndex = 0;
        for (String molecularIndexScheme : vesselSpreadsheetProcessor.getMolecularIndexingScheme()) {
            MolecularIndexingScheme molecularIndexingScheme =
                    molecularIndexingSchemeDao.findByName(molecularIndexScheme);
            if (molecularIndexingScheme == null) {
                messageCollection.addError("Molecular Indexing Scheme not found: " + molecularIndexScheme.toString()
                        + " At Row: " + (molecularIndexSchemeIndex + rowOffset)
                        + " Column: " + VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText());
            } else {
                molecularIndexSchemes.add(molecularIndexingScheme);
            }
            molecularIndexSchemeIndex++;
        }

        //Add root samples
        for (String rootSampleId : vesselSpreadsheetProcessor.getRootSampleId()) {
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(rootSampleId);
            if (mercurySample != null) {
                rootSamples.add(mercurySample);
            }
        }

        //Was both bait and cat specified.
        int baitCatIndex = 0;
        for (String bait : vesselSpreadsheetProcessor.getBait()) {
            ReagentDesign reagentDesignBait = reagentDesignDao.findByBusinessKey(bait);
            String cat = vesselSpreadsheetProcessor.getCat().get(baitCatIndex);
            ReagentDesign reagentDesignCat = reagentDesignDao.findByBusinessKey(cat);

            if (!bait.isEmpty() && !cat.isEmpty()) {
                messageCollection.addError("Found both Bait and CAT on same line. Bait: " + bait + " CAT: " + cat +
                        " At Row: " + (baitCatIndex + rowOffset) +
                        " Column: " + VesselPooledTubesProcessor.Headers.BAIT.getText() +
                        " & " + VesselPooledTubesProcessor.Headers.CAT.getText());
            }
            if ((!cat.isEmpty() && cat.isEmpty()) && reagentDesignBait == null) {
                messageCollection.addError("Bait: " + bait + " is not registered. " +
                        " At Row: " + (baitCatIndex + rowOffset) +
                        " Column: " + VesselPooledTubesProcessor.Headers.BAIT.getText());
            }
            if (reagentDesignBait != null) {
                reagents.add(reagentDesignBait);
            }
            if ((!cat.isEmpty() && bait.isEmpty()) && reagentDesignCat == null) {
                messageCollection.addError("Cat: " + cat + " is not registered. " +
                        " At Row: " + (baitCatIndex + rowOffset) +
                        " Column: " + VesselPooledTubesProcessor.Headers.CAT.getText());
            }
            if (reagentDesignCat != null) {
                reagents.add(reagentDesignCat);
            }
            ++baitCatIndex;
        }

        //Find the Jira ticket & list of dev conditions (sub tasks) for the experiment.
        int experimentIndex = 2;
        int conditionIndex = 0;
        List<Map<String, String>> devConditions = vesselSpreadsheetProcessor.getConditions();
        List<String> subTaskList = new ArrayList<String>();
        for (Map<String, String> devCondition : devConditions) {
            String experiment = vesselSpreadsheetProcessor.getExperiment().get(conditionIndex);
            JiraIssue jiraIssue = getIssueInfoNoException(experiment, null);
            List<String> jiraSubTasks;
            if (jiraIssue == null) {
                messageCollection.addError("Dev ticket not found for Experiment: " + experiment +
                        " At Row: " + experimentIndex +
                        " Column: " + VesselPooledTubesProcessor.Headers.EXPERIMENT.getText());
            } else {
                jiraSubTasks = jiraIssue.getSubTasks();
                if (jiraSubTasks != null && devConditions.size() > 0) {
                    subTaskList = new ArrayList<>(devCondition.values());
                    for (String subTask : subTaskList) {
                        boolean foundFlag = false;
                        for (String jiraSubTask : jiraSubTasks) {
                            if (devCondition.containsKey(jiraSubTask)) {
                                foundFlag = true;
                                jiraSubTasks.remove(jiraSubTask);
                                break;
                            }
                        }
                        if (!foundFlag) {
                            messageCollection.addError("Condition / Sub Task: " + subTask +
                                    " not found for Experiment: " + experiment +
                                    " At Row: " + experimentIndex +
                                    " Column: " + VesselPooledTubesProcessor.Headers.EXPERIMENT.getText());
                        }
                    }
                }
            }
            jiraSubTaskList.add(subTaskList);
            experimentIndex++;
            conditionIndex++;
        }

        //Is the sample ID registered in Mercury? If not check for optional ID fields.
        int sampleIndex = 0;
        for (String sampleId : vesselSpreadsheetProcessor.getBroadSampleId()) {
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleId);

            //If the sample is missing or not registered check to see if the alternate info was supplied
            if ((sampleId == null || mercurySample == null)) {
                collaboratorSampleIds.add(vesselSpreadsheetProcessor.getCollaboratorSampleId().get(sampleIndex));
                checkForOptionalHeaders(collaboratorSampleIds.get(sampleIndex),
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID, sampleIndex, messageCollection);
                collaboratorParticipantIds.add(vesselSpreadsheetProcessor.getCollaboratorParticipantId()
                        .get(sampleIndex));
                checkForOptionalHeaders(collaboratorParticipantIds.get(sampleIndex),
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID, sampleIndex, messageCollection);
                broadParticipantIds.add(vesselSpreadsheetProcessor.getBroadParticipantId().get(sampleIndex));
                checkForOptionalHeaders(broadParticipantIds.get(sampleIndex),
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID, sampleIndex, messageCollection);
                genders.add(vesselSpreadsheetProcessor.getGender().get(sampleIndex));
                checkForOptionalHeaders(genders.get(sampleIndex), VesselPooledTubesProcessor.Headers.GENDER,
                        sampleIndex, messageCollection);
                species.add(vesselSpreadsheetProcessor.getSpecies().get(sampleIndex));
                checkForOptionalHeaders(species.get(sampleIndex), VesselPooledTubesProcessor.Headers.SPECIES,
                        sampleIndex, messageCollection);
                lsids.add(vesselSpreadsheetProcessor.getLsid().get(sampleIndex));
                checkForOptionalHeaders(lsids.get(sampleIndex), VesselPooledTubesProcessor.Headers.LSID,
                        sampleIndex, messageCollection);
                sampleRegistrationFlag.add(false);
            } else {
                mercurySamples.add(mercurySample);
                sampleRegistrationFlag.add(true);
            }
            ++sampleIndex;
        }

        /**
         * If there are no errors and this is not a verification test, attempt save the data to the database.
         */
        if (!messageCollection.hasErrors()) {
            persistPooledTubeResults(vesselSpreadsheetProcessor, messageCollection);
        }

    }

    /**
     * Get the product order.
     */
    private ProductOrder getPdo(String project, String dataAnalysisType, int index,
            MessageCollection messageCollection) {

        ProductOrder productOrder = productOrderDao.findByTitle(project);
        if (productOrder == null) {
            messageCollection.addError("No valid product order found at row: " + index);
            return null;
        } else {
            //TODO: productOrder.getProduct().getAnalysisTypeKey() Is this the correct field???
            if (!dataAnalysisType.equals(productOrder.getProduct().getAnalysisTypeKey())) {
                messageCollection.addError("Data Analysis type is invalid at row: " + index);
                return null;
            }
        }
        return productOrder;
    }

    /**
     * Check that a valid IRB exists.
     */
    private void validateIRB(ResearchProject researchProject, String irbNumber, int index,
            MessageCollection messageCollection) {
        if (researchProject != null) {
            for (String irb : researchProject.getIrbNumbers()) {
                if (irb.contains(irbNumber)) {
                    return;
                }
            }
            messageCollection
                    .addError("IRB Number: " + irbNumber + " Does not appear to be valid at line" + (index + 2));
        }
    }

    /**
     * Method to find research project information.
     */
    private ResearchProject getResearchProject(ProductOrder productOrder, int index,
            MessageCollection messageCollection) {
        if (productOrder == null) {
            return null;
        }

        if (productOrder.getResearchProject() == null) {
            messageCollection.addError("Research Project was missing or is invalid value at Row: " + (index + 2));
            return null;
        }

        if (productOrder != null) {
            ResearchProject researchProject = productOrder.getResearchProject();
            return researchProject;
        }
        return null;
    }

    /**
     * Method to create errors when no Broad ID was supplied and some/all collaborator information is missing.
     */
    private void checkForOptionalHeaders(String value, VesselPooledTubesProcessor.Headers headers, int index,
            MessageCollection messageCollection) {
        if (isFieldEmpty(value)) {
            messageCollection.addError("No Valid Broad Sample ID found and column " + headers.getText()
                    + " was also missing at Row: " + (index + 2));
        }
    }

    /**
     * Check for empty spreadsheet fields that may have spaces or be null.
     */
    private boolean isFieldEmpty(String field) {
        if (field != null) {
            if (field.trim().isEmpty()) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Find the current mercury sample.
     */
    private MercurySample getMercurySample(String sampleId) {
        MercurySample mercurySample = null;
        if (sampleId != null) {
            mercurySample = mercurySampleDao.findBySampleKey(sampleId);
            if (mercurySample == null) {
                mercurySample = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
            }
        }
        return mercurySample;
    }

    /**
     * Find the current sample kit request or create a new one.
     */
    private SampleKitRequest getSampleKit(String Organization) {
        //TODO: Is Organization name a unique enough identifier???
        SampleKitRequest sampleKitRequest;
        sampleKitRequest = sampleKitRequestDao.findByOrganization(Organization);
        if (sampleKitRequest == null) {
            sampleKitRequest = new SampleKitRequest();
        }
        return sampleKitRequest;
    }

    /**
     * Find the current instance of the sample V2 entity.
     */
    private boolean sampleExists(String libraryName, Boolean overWriteFlag, MessageCollection messageCollection,
            int displayIndex) {
        SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(libraryName);
        if (sampleInstanceEntity == null) {
            return false;
        }
        if (!overWriteFlag) {
            messageCollection.addError("Single sample library name : " + libraryName + " at row: " + displayIndex
                    + " exists in the database. Please choose the overwrite previous upload option.");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find the current instance of the sample V2 entity.
     */
    private MolecularIndex getMolIndex(String molIndex, MessageCollection messageCollection, int displayIndex) {
        if (molIndex == null || molIndex.isEmpty()) {
            return null;
        }
        MolecularIndex molecularIndex = molecularIndexDao.findBySequence(molIndex);
        if (molecularIndex == null) {
            messageCollection.addError("No valid molecular barcode sequence found at line: " + displayIndex);
            return null;
        }
        return molecularIndex;
    }


    /**
     * Find the current instance of the sample V2 entity.
     */
    private SampleInstanceEntity getSampleInstanceEntity(String sampleName, SampleKitRequest sampleKitRequest) {
        SampleInstanceEntity sampleInstanceEntity = null;
        if (sampleName != null) {
            sampleInstanceEntity = sampleInstanceEntityDao.findByName(sampleName);
        }
        if (sampleInstanceEntity == null) {
            sampleInstanceEntity = new SampleInstanceEntity();
            sampleKitRequestDao.persist(sampleKitRequest);
            sampleInstanceEntity.setSampleKitRequest(sampleKitRequest);
        }
        return sampleInstanceEntity;
    }

    /**
     * Find the current lab vessel or create a new one.
     */
    public LabVessel getLabVessel(String barcode) {
        if (barcode == null) {
            return null;
        }
        LabVessel labVessel = null;
        labVessel = labVesselDao.findByIdentifier(barcode);
        if (labVessel == null) {
            labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
        }
        return labVessel;
    }

    /**
     * This intercepts the Jira Issue exception handler and keeps it from preempting the global validation errors
     */
    @Nullable
    private JiraIssue getIssueInfoNoException(String key, String... fields) {
        try {
            return jiraService.getIssueInfo(key, fields);
        } catch (Exception e) {
            return null;
        }
    }

    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    public void setSampleInstanceEntityDao(SampleInstanceEntityDao sampleInstanceEntityDao) {
        this.sampleInstanceEntityDao = sampleInstanceEntityDao;
    }

    public void setMolecularIndexingSchemeDao(MolecularIndexingSchemeDao molecularIndexingSchemeDao) {
        this.molecularIndexingSchemeDao = molecularIndexingSchemeDao;
    }

    public void setMercurySampleDao(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    public void setReagentDesignDao(ReagentDesignDao reagentDesignDao) {
        this.reagentDesignDao = reagentDesignDao;
    }

    public void setJiraService(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    public void setMolecularIndexDao(MolecularIndexDao molecularIndexDao) {
        this.molecularIndexDao = molecularIndexDao;

    }

    public void setProductOrderDao(ProductOrderDao productOrderDao) {
        this.productOrderDao = productOrderDao;
    }

    public void setSampleKitRequestDao(SampleKitRequestDao sampleKitRequestDao) {
        this.sampleKitRequestDao = sampleKitRequestDao;
    }

    public void setRootSamples(List<MercurySample> mercurySamples) {
        this.rootSamples = mercurySamples;
    }
}
