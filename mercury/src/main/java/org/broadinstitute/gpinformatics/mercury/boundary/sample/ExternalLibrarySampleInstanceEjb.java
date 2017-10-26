package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import clover.org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooledMultiOrganism;
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
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExternalLibrarySampleInstanceEjb {
    private final String EZPASS = "ezpass";
    private List<List<String>> jiraSubTaskLists = new ArrayList<>();
    private List<String> collaboratorSampleIds = new ArrayList<>();
    private List<String> collaboratorParticipantIds = new ArrayList<>();
    private List<String> broadParticipantIds = new ArrayList<>();
    private List<String> genders = new ArrayList<>();
    private List<String> species = new ArrayList<>();
    private List<String> lsids = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<String> sampleLibraryNames = new ArrayList<>();
    private List<MolecularIndexingScheme> molecularIndexSchemes = new ArrayList<>();
    private List<ReagentDesign> reagents = new ArrayList<>();
    private List<MercurySample> mercurySamples = new ArrayList<>();
    private List<MercurySample> rootSamples = new ArrayList<>();
    private List<Boolean> sampleRegistrationFlags = new ArrayList<>();
    private List<ProductOrder> productOrders = new ArrayList<>();
    private List<ResearchProject> researchProjects = new ArrayList<>();
    private static final int ROW_OFFSET = 2;

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
            if (!sampleRegistrationFlags.get(sampleIndex)) {
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
            for (String subTask : jiraSubTaskLists.get(sampleIndex)) {
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
    private void persistExternalLibraries(ExternalLibraryProcessor processor,
            MessageCollection messageCollection, String spreadsheetType) {

        int sampleIndex = 0;
        String organism = null;

        SampleKitRequest sampleKitRequest = getSampleKit(processor.getOrganization());
        sampleKitRequest.setCollaboratorName(processor.getFirstName() + " " + processor.getLastName());
        sampleKitRequest.setFirstName(processor.getFirstName());
        sampleKitRequest.setLastName(processor.getLastName());
        sampleKitRequest.setOrganization(processor.getOrganization());
        sampleKitRequest.setAddress(processor.getAddress());
        sampleKitRequest.setCity(processor.getCity());
        sampleKitRequest.setState(processor.getState());
        sampleKitRequest.setPostalCode(processor.getZip());
        sampleKitRequest.setCountry(processor.getCountry());
        sampleKitRequest.setPhone(processor.getPhone());
        sampleKitRequest.setEmail(processor.getEmail());
        sampleKitRequest.setCommonName(processor.getCommonName());
        sampleKitRequest.setGenus(processor.getGenus());
        sampleKitRequest.setSpecies(processor.getSpecies());
        sampleKitRequest.setIrbApprovalRequired(processor.getIrbRequired());

        for (String libraryName : processor.getSingleSampleLibraryName()) {

            LabVessel labVessel = getLabVessel(processor.getSingleSampleLibraryName().get(sampleIndex));

            if (messageCollection.hasErrors()) {
                return;
            }

            labVessel.setVolume(new BigDecimal(processor.getTotalLibraryVolume().get(sampleIndex)));
            labVessel.setConcentration(new BigDecimal(processor.getTotalLibraryConcentration().get(sampleIndex)));

            MercurySample mercurySample = getMercurySample(processor.getCollaboratorSampleId().get(sampleIndex));


            if (spreadsheetType.equals(ExternalLibraryUploadActionBean.MULTI_ORG)) {
                organism = processor.getOrganism().get(sampleIndex);
            }

            mercurySample.addMetadata(setExternalMetaData(processor.getSex().get(sampleIndex),
                    processor.getStrain().get(sampleIndex),
                    processor.getStrain().get(sampleIndex),
                    processor.getCollaboratorSampleId().get(sampleIndex),
                    processor.getIndividualName().get(sampleIndex),
                    organism));

            mercurySample.addLabVessel(labVessel);
            mercurySampleDao.persist(mercurySample);


            SampleInstanceEntity sampleInstanceEntity = getSampleInstanceEntity(libraryName, sampleKitRequest);
            if (!spreadsheetType.equals(ExternalLibraryUploadActionBean.NON_POOLED)) {
                sampleInstanceEntity.setPooled(processor.getPooled().get(sampleIndex));
            }
            sampleInstanceEntity.setTissueType(processor.getTissueType().get(sampleIndex));
            sampleInstanceEntity.setSampleLibraryName(processor.getSingleSampleLibraryName().get(sampleIndex));
            sampleInstanceEntity.setInsertSizeRange(processor.getInsertSizeRangeBp().get(sampleIndex));
            sampleInstanceEntity.setLibrarySizeRange(processor.getLibrarySizeRangeBp().get(sampleIndex));
            sampleInstanceEntity.setJumpSize(processor.getJumpSize().get(sampleIndex));
            sampleInstanceEntity.setRestrictionEnzyme(processor.getRestrictionEnzymes().get(sampleIndex));

            if(!spreadsheetType.contains(EZPASS)) {
                sampleInstanceEntity.setDesiredReadLength(new Integer(processor.getDesiredReadLength().get(sampleIndex)));
            }

            sampleInstanceEntity.setReferenceSequence(processor.getReferenceSequence().get(sampleIndex));
            MolecularIndex molecularIndex = molecularIndexDao.findBySequence(processor.getMolecularBarcodeSequence().get(sampleIndex));
            if (molecularIndex != null) {
                sampleInstanceEntity.setMolecularIndexScheme(molecularIndex.getMolecularIndexingSchemes().iterator().next());
            }
            sampleInstanceEntity.setLibraryType(processor.getLibraryType().get(sampleIndex));
            sampleInstanceEntity.setResearchProject(researchProjects.get(sampleIndex));
            sampleInstanceEntity.setProductOrder(productOrders.get(sampleIndex));
            sampleInstanceEntity.setCollaboratorSampleId(mercurySample.getSampleKey());
            sampleInstanceEntity.setUploadDate();


            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setMercurySampleId(mercurySample);
            sampleInstanceEntityDao.persist(sampleInstanceEntity);

            ++sampleIndex;
            messageCollection.addInfo("Spreadsheet with " + String.valueOf(sampleIndex) + " rows successfully uploaded!");
        }

    }


    /**
     * Verify required fields for External Libraries.
     */
    public void verifyExternalLibrary(ExternalLibraryProcessor processor,
            MessageCollection messageCollection, boolean overWriteFlag, String spreadsheetType) {

        researchProjects.clear();
        productOrders.clear();
        for (int index = 0; index < processor.getSingleSampleLibraryName().size(); ++index) {
            int rowNumber = processor.getHeaderRowIndex() + 2;
            String libraryName = processor.getSingleSampleLibraryName().get(index);

            if (spreadsheetType.equals(ExternalLibraryUploadActionBean.EZPASS_KIOSK)) {
                validateRequiredFields(processor.getBarcodes().get(index),
                        ExternalLibraryProcessorEzPass.Headers.TUBE_BARCODE.getText(), rowNumber, messageCollection);
            }
            if (!spreadsheetType.contains(EZPASS)) {
                validateRequiredFields(processor.getIrbNumber().get(index),
                        ExternalLibraryProcessorPooled.Headers.IRB_NUMBER.getText(), rowNumber, messageCollection);
                validateRequiredFields(processor.getDesiredReadLength().get(index),
                        ExternalLibraryProcessorPooled.Headers.DESIRED_READ_LENGTH.getText(), rowNumber, messageCollection);
            }
            if (!spreadsheetType.equals(ExternalLibraryUploadActionBean.NON_POOLED)) {
                validateRequiredFields(processor.getPooled().get(index),
                        ExternalLibraryProcessorPooled.Headers.POOLED.getText(), rowNumber, messageCollection);
            }
            if (spreadsheetType.equals(ExternalLibraryUploadActionBean.MULTI_ORG)) {
                validateRequiredFields(processor.getOrganism().get(index),
                        ExternalLibraryProcessorPooledMultiOrganism.Headers.ORGANISM.getText(), rowNumber, messageCollection);
            }

            validateRequiredFields(processor.getCollaboratorSampleId().get(index),
                    ExternalLibraryProcessorPooled.Headers.COLLABORATOR_SAMPLE_ID.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getIndividualName().get(index),
                    ExternalLibraryProcessorPooled.Headers.INDIVIDUAL_NAME.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getSingleSampleLibraryName().get(index),
                    ExternalLibraryProcessorPooled.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(), rowNumber,
                    messageCollection);
            validateRequiredFields(processor.getLibraryType().get(index),
                    ExternalLibraryProcessorPooled.Headers.LIBRARY_TYPE.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getInsertSizeRangeBp().get(index),
                    ExternalLibraryProcessorPooled.Headers.INSERT_SIZE_RANGE_BP.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getLibrarySizeRangeBp().get(index),
                    ExternalLibraryProcessorPooled.Headers.LIBRARY_SIZE_RANGE_BP.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getTotalLibraryVolume().get(index),
                    ExternalLibraryProcessorPooled.Headers.TOTAL_LIBRARY_VOLUME.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getTotalLibraryConcentration().get(index),
                    ExternalLibraryProcessorPooled.Headers.TOTAL_LIBRARY_CONCENTRATION.getText(), rowNumber,
                    messageCollection);
            validateNumericFields(processor.getTotalLibraryVolume().get(index),
                    ExternalLibraryProcessorPooled.Headers.TOTAL_LIBRARY_VOLUME.getText(), rowNumber, messageCollection);
            validateNumericFields(processor.getTotalLibraryConcentration().get(index),
                    ExternalLibraryProcessorPooled.Headers.TOTAL_LIBRARY_CONCENTRATION.getText(), rowNumber,
                    messageCollection);
            validateRequiredFields(processor.getFundingSource().get(index),
                    ExternalLibraryProcessorPooledMultiOrganism.Headers.FUNDING_SOURCE.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getReferenceSequence().get(index),
                    ExternalLibraryProcessorPooledMultiOrganism.Headers.REFERENCE_SEQUENCE.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getRequestedCompletionDate().get(index),
                    ExternalLibraryProcessorPooledMultiOrganism.Headers.REQUESTED_COMPLETION_DATE.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getDataSubmission().get(index),
                    ExternalLibraryProcessorPooledMultiOrganism.Headers.DATA_SUBMISSION.getText(), rowNumber, messageCollection);
            validateRequiredFields(processor.getDataAnalysisType().get(index),
                    ExternalLibraryProcessorPooledMultiOrganism.Headers.DATA_ANALYSIS_TYPE.getText(), rowNumber, messageCollection);

            //Database validations.
            getMolIndex(processor.getMolecularBarcodeSequence().get(index), messageCollection, rowNumber);
            String project = processor.getProjectTitle().get(index);
            String dataAnalysisType = processor.getDataAnalysisType().get(index);
            productOrders.add(getPdo(project, dataAnalysisType, rowNumber, messageCollection));
            researchProjects.add(getResearchProject(productOrders.get(index), rowNumber, messageCollection));
            sampleExists(libraryName, overWriteFlag, messageCollection, rowNumber);
            if (!spreadsheetType.contains(EZPASS)) {
                validateIRB(researchProjects.get(index), processor.getIrbNumber().get(index), rowNumber, messageCollection);
            }
        }
        if (CollectionUtils.isEmpty(processor.getSingleSampleLibraryName())) {
            messageCollection.addError("No data found.");
        }

        if (!messageCollection.hasErrors()) {
            persistExternalLibraries(processor, messageCollection, spreadsheetType);
        }
    }

    /**
     * Check that required fields exist
     */
    private void validateRequiredFields(String value, String header, int index, MessageCollection messageCollection) {
        if (StringUtils.isBlank(value)) {
            messageCollection.addError(header + " is required at row " + (index));
        }
    }

    /**
     * Check if a field contains a valid number.
     */
    private void validateNumericFields(String value, String header, int index, MessageCollection messageCollection) {
        if (!NumberUtils.isNumber(value)) {
            messageCollection.addError(header + " is not a valid number at row " + (index));
        }
    }

    /**
     * Verify the spreadsheet contents before attempting to persist data for pooled tubes (not external libraries)
     */
    public void verifyPooledTubes(VesselPooledTubesProcessor vesselSpreadsheetProcessor,
            MessageCollection messageCollection, boolean overWriteFlag) {

        // Sample library names must all be unique on this spreadsheet.
        int rowNumber = vesselSpreadsheetProcessor.getHeaderRowIndex() + 2;
        HashSet<String> names = new HashSet<>();
        for (String libraryName : vesselSpreadsheetProcessor.getSingleSampleLibraryName()) {
            if (names.add(libraryName)) {
                sampleLibraryNames.add(libraryName);
            } else {
                messageCollection.addError("Duplicated single sample library name \"" + libraryName +
                        "\" at row " + rowNumber +
                        " column " + VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText());
            }
            if (sampleInstanceEntityDao.findByName(libraryName) != null && !overWriteFlag) {
                messageCollection.addError("Pre-existing single sample library name (need to click overwrite?) \"" +
                        libraryName + "\" at row " + rowNumber +
                        " column " + VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText());
            }
            ++rowNumber;
        }

        //Are Tubes registered.
        rowNumber = vesselSpreadsheetProcessor.getHeaderRowIndex() + 2;
        for (String barcode : vesselSpreadsheetProcessor.getBarcodes()) {
            if (StringUtils.isBlank(barcode)) {
                messageCollection.addError("Missing barcode at row " + rowNumber +
                        " column " + VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText());
            } else if (labVesselDao.findByIdentifier(barcode) != null && !overWriteFlag) {
                messageCollection.addError("Pre-existing barcode (need to click overwrite?) \"" + barcode +
                        "\" at row " + rowNumber +
                        " column " + VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText());
            } else {
                barcodes.add(barcode);
            }
            ++rowNumber;
        }

        //Does molecular index scheme exist.
        rowNumber = vesselSpreadsheetProcessor.getHeaderRowIndex() + 2;
        for (String molecularIndexScheme : vesselSpreadsheetProcessor.getMolecularIndexingScheme()) {
            MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(
                    molecularIndexScheme);
            if (molecularIndexingScheme == null) {
                messageCollection.addError("Unknown molecular indexing scheme \"" + molecularIndexScheme
                        + "\" at row " + rowNumber +
                        " column " + VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText());
            } else {
                this.molecularIndexSchemes.add(molecularIndexingScheme);
            }
            ++rowNumber;
        }

        //Add root samples
        for (String rootSampleId : vesselSpreadsheetProcessor.getRootSampleId()) {
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(rootSampleId);
            if (mercurySample != null) {
                this.rootSamples.add(mercurySample);
            }
        }

        // Check bait and cat.
        for (int index = 0; index < vesselSpreadsheetProcessor.getBait().size(); ++index) {
            rowNumber = vesselSpreadsheetProcessor.getHeaderRowIndex() + 2 + index;
            String bait = vesselSpreadsheetProcessor.getBait().get(index);
            String cat = vesselSpreadsheetProcessor.getCat().get(index);
            if (!bait.isEmpty() && !cat.isEmpty()) {
                messageCollection.addError("Cannot specify both bait and cat" +
                        " at row " + rowNumber +
                        " columns " + VesselPooledTubesProcessor.Headers.BAIT.getText() +
                        ", " + VesselPooledTubesProcessor.Headers.CAT.getText());
            } else if (!bait.isEmpty()) {
                ReagentDesign reagentDesignBait = reagentDesignDao.findByBusinessKey(bait);
                if (reagentDesignBait == null) {
                    messageCollection.addError("Unknown bait \"" + bait + "\" at row " + rowNumber +
                            " column " + VesselPooledTubesProcessor.Headers.BAIT.getText());
                } else {
                    reagents.add(reagentDesignBait);
                }
            } else if (!cat.isEmpty()) {
                ReagentDesign reagentDesignCat = reagentDesignDao.findByBusinessKey(cat);
                if (reagentDesignCat == null) {
                    messageCollection.addError("Unknown cat \"" + cat + "\" at row " + rowNumber +
                            " column " + VesselPooledTubesProcessor.Headers.CAT.getText());
                } else {
                    reagents.add(reagentDesignCat);
                }
            }
        }

        //Find the Jira ticket & list of dev conditions (sub tasks) for the experiment.
        rowNumber = vesselSpreadsheetProcessor.getHeaderRowIndex() + 2;
        int conditionIndex = 0;
        List<Map<String, String>> devConditions = vesselSpreadsheetProcessor.getConditions();
        List<String> subTaskList = new ArrayList<>();
        for (Map<String, String> devCondition : devConditions) {
            String experiment = vesselSpreadsheetProcessor.getExperiment().get(conditionIndex);
            JiraIssue jiraIssue = getIssueInfoNoException(experiment);
            List<String> jiraSubTasks;
            if (jiraIssue == null) {
                messageCollection.addError("JIRA Dev ticket not found for experiment \"" + experiment +
                        "\" at row " + rowNumber +
                        " column " + VesselPooledTubesProcessor.Headers.EXPERIMENT.getText());
            } else {
                jiraSubTasks = jiraIssue.getSubTasks();
                if (jiraSubTasks != null && devConditions.size() > 0) {
                    subTaskList = new ArrayList<>(devCondition.values());
                    for (String subTask : subTaskList) {
                        boolean foundSubTask = false;
                        for (String jiraSubTask : jiraSubTasks) {
                            if (devCondition.containsKey(jiraSubTask)) {
                                foundSubTask = true;
                                jiraSubTasks.remove(jiraSubTask);
                                break;
                            }
                        }
                        if (!foundSubTask) {
                            messageCollection.addError("Condition/SubTask \"" + subTask +
                                    "\" not found for experiment \"" + experiment + "\" at row " + rowNumber +
                                    " column " + VesselPooledTubesProcessor.Headers.EXPERIMENT.getText());
                        }
                    }
                }
            }
            jiraSubTaskLists.add(subTaskList);
            rowNumber++;
            conditionIndex++;
        }

        //Is the sample ID registered in Mercury? If not check for optional ID fields.
        int sampleIndex = 0;
        for (String sampleId : vesselSpreadsheetProcessor.getBroadSampleId()) {
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleId);

            //If the sample is missing or not registered check to see if the alternate info was supplied
            if ((sampleId == null || mercurySample == null)) {
                collaboratorSampleIds.add(vesselSpreadsheetProcessor.getCollaboratorSampleId().get(sampleIndex));
                checkContingentValues(collaboratorSampleIds.get(sampleIndex),
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID, sampleIndex, messageCollection);
                collaboratorParticipantIds.add(vesselSpreadsheetProcessor.getCollaboratorParticipantId()
                        .get(sampleIndex));
                checkContingentValues(collaboratorParticipantIds.get(sampleIndex),
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID, sampleIndex, messageCollection);
                broadParticipantIds.add(vesselSpreadsheetProcessor.getBroadParticipantId().get(sampleIndex));
                checkContingentValues(broadParticipantIds.get(sampleIndex),
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID, sampleIndex, messageCollection);
                genders.add(vesselSpreadsheetProcessor.getGender().get(sampleIndex));
                checkContingentValues(genders.get(sampleIndex), VesselPooledTubesProcessor.Headers.GENDER,
                        sampleIndex, messageCollection);
                species.add(vesselSpreadsheetProcessor.getSpecies().get(sampleIndex));
                checkContingentValues(species.get(sampleIndex), VesselPooledTubesProcessor.Headers.SPECIES,
                        sampleIndex, messageCollection);
                lsids.add(vesselSpreadsheetProcessor.getLsid().get(sampleIndex));
                checkContingentValues(lsids.get(sampleIndex), VesselPooledTubesProcessor.Headers.LSID, sampleIndex,
                        messageCollection);
                sampleRegistrationFlags.add(false);
            } else {
                mercurySamples.add(mercurySample);
                sampleRegistrationFlags.add(true);
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
            messageCollection.addError("Unknown product order found at row " + index);
            return null;
        } else {
            //TODO: productOrder.getProduct().getAnalysisTypeKey() Is this the correct field???
            if (!dataAnalysisType.equals(productOrder.getProduct().getAnalysisTypeKey())) {
                messageCollection.addError("Data analysis not compatible with product at row " + index +
                        " column " + ExternalLibraryProcessorEzPass.Headers.DATA_ANALYSIS_TYPE.getText());
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
            messageCollection.addError("IRB number \"" + irbNumber + "\" not compatible with research project " +
                    researchProject.getBusinessKey() + " at row " + (index + ROW_OFFSET) +
                    " column " + ExternalLibraryProcessorPooled.Headers.IRB_NUMBER.getText());
        }
    }

    /**
     * Method to find research project information.
     */
    private ResearchProject getResearchProject(ProductOrder productOrder, int index,
            MessageCollection messageCollection) {
        if (productOrder != null) {
            if (productOrder.getResearchProject() == null) {
                messageCollection.addError("Undefined research project on product order at row " +
                        (index + ROW_OFFSET));
            } else {
                return productOrder.getResearchProject();
            }
        }
        return null;
    }

    /**
     * Method to create errors when no Broad ID was supplied and some/all collaborator information is missing.
     */
    private void checkContingentValues(String value, VesselPooledTubesProcessor.Headers headers, int index,
            MessageCollection messageCollection) {
        if (StringUtils.isNotBlank(value)) {
            messageCollection.addError(String.format("Spreadsheet must supply %s at row %d since Broad Sample is blank",
                    headers.getText(), index + ROW_OFFSET));
        }
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
            messageCollection.addError("Pre-existing sample library name (need to check overwrite?) \"" +
                    libraryName + "\" at row " + displayIndex);
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
            messageCollection.addError("Unknown molecular barcode sequence at row " + displayIndex);
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
        if (StringUtils.isBlank(barcode)) {
            return null;
        }
        LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
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

