package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntityTsk;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.jvnet.inflector.Noun;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
public class SampleInstanceEjb  {

    private Map<String, LabVessel> mapBarcodeToVessel;
    private List<String> collaboratorSampleId = new ArrayList<>();
    private List<String> collaboratorParticipantId = new ArrayList<>();
    private List<String> broadParticipantId = new ArrayList<>();
    private List<String> gender = new ArrayList<>();
    private List<String> species = new ArrayList<>();
    private List<String> lsid = new ArrayList<>();
    private List<String> barcodes = new ArrayList<>();
    private List<MolecularIndexingScheme> molecularIndexSchemes = new ArrayList<>();
    private List<ReagentDesign> reagents = new ArrayList<>();
    private Map<String, MercurySample> broadSampleMap = new HashMap<>();
    private Map<String, MercurySample> rootSampleMap = new HashMap<>();
    private Map<String, BigDecimal> barcodeToVolumeMap = new HashMap<>();
    private Map<String, BspSampleData> bspSampleDataMap = new HashMap<>();

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

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
    private BSPSampleDataFetcher bspSampleDataFetcher;

    /**
     * Save uploaded data after it hs been verified by verifySpreadSheet();
     */
    private void persistResults(VesselPooledTubesProcessor vesselSpreadsheetProcessor, MessageCollection messageCollection) {
        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>();
        List<LabVessel> labVessels = new ArrayList<>();

        int rowIndex = 0;
        for (String broadSampleName : vesselSpreadsheetProcessor.getBroadSampleId()) {
            String barcode = vesselSpreadsheetProcessor.getBarcodes().get(rowIndex);
            LabVessel labVessel = mapBarcodeToVessel.get(barcode);
            if (labVessel == null) {
                labVessel = new BarcodedTube(vesselSpreadsheetProcessor.getBarcodes().get(rowIndex),
                        BarcodedTube.BarcodedTubeType.MatrixTube);
            }
            labVessel.setVolume(barcodeToVolumeMap.get(barcode));

            //Add library size lab metric to to lab vessel.
            addLibrarySize(labVessel, new BigDecimal(vesselSpreadsheetProcessor.getFragmentSize().get(rowIndex)));

            // Finds or creates the Broad MercurySample.
            MercurySample broadSample = broadSampleMap.get(broadSampleName);
            if (broadSample == null) {
                // New sample uses either BSP data or the spreadsheet row.
                BspSampleData bspSampleData = bspSampleDataMap.get(broadSampleName);
                if (bspSampleData != null) {
                    broadSample = new MercurySample(broadSampleName, bspSampleData);
                } else {
                    Set<Metadata> metadata = new HashSet<>();
                    metadata.add(new Metadata(Metadata.Key.SAMPLE_ID, collaboratorSampleId.get(rowIndex)));
                    metadata.add(new Metadata(Metadata.Key.BROAD_PARTICIPANT_ID, broadParticipantId.get(rowIndex)));
                    metadata.add(new Metadata(Metadata.Key.PATIENT_ID, collaboratorParticipantId.get(rowIndex)));
                    metadata.add(new Metadata(Metadata.Key.GENDER, gender.get(rowIndex)));
                    metadata.add(new Metadata(Metadata.Key.LSID, lsid.get(rowIndex)));
                    metadata.add(new Metadata(Metadata.Key.SPECIES, species.get(rowIndex)));
                    metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                    broadSample = new MercurySample(broadSampleName, metadata);
                }
                broadSampleMap.put(broadSampleName, broadSample);
            }
            broadSample.addLabVessel(labVessel);

            // Finds or creates the root MercurySample.
            MercurySample rootSample = null;
            String rootSampleName = vesselSpreadsheetProcessor.getRootSampleId().get(rowIndex);
            if (StringUtils.isNotBlank(rootSampleName)) {
                if (broadSampleName.equals(rootSampleName)) {
                    rootSample = broadSample;
                } else {
                    // Checks for a root sample used on an earlier row.
                    rootSample = rootSampleMap.get(rootSampleName);
                    if (rootSample == null) {
                        // Root sample is unknown to Mercury. Creates a new sample using metadata from either
                        // BSP or the Broad Sample, which was newly created using the spreadsheet row metadata
                        // and should apply to the root sample too.
                        rootSample = bspSampleDataMap.containsKey(rootSampleName) ?
                                new MercurySample(rootSampleName, bspSampleDataMap.get(rootSampleName)) :
                                new MercurySample(rootSampleName, broadSample.getMetadata());
                        rootSampleMap.put(rootSampleName, rootSample);
                    }
                    rootSample.addLabVessel(labVessel);
                }
            }

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(
                    vesselSpreadsheetProcessor.getSingleSampleLibraryName().get(rowIndex));
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
            }
            sampleInstanceEntity.setMercurySample(broadSample);
            sampleInstanceEntity.setRootSample(rootSample);
            sampleInstanceEntity.setMolecularIndexScheme(molecularIndexSchemes.get(rowIndex));
            sampleInstanceEntity.setSampleLibraryName(vesselSpreadsheetProcessor.getSingleSampleLibraryName().get(rowIndex));
            sampleInstanceEntity.setReagentDesign(reagents.get(rowIndex));

            if (StringUtils.isNotBlank(vesselSpreadsheetProcessor.getReadLength().get(rowIndex))) {
                sampleInstanceEntity.setReadLength(Integer.valueOf(vesselSpreadsheetProcessor.getReadLength().get(rowIndex)));
            }

            sampleInstanceEntity.setExperiment(vesselSpreadsheetProcessor.getExperiment().get(rowIndex));

            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setUploadDate();
            sampleInstanceEntity.removeSubTasks();

            // Persists the experiment conditions in the order they appeared on the row.
            for (String subTask : vesselSpreadsheetProcessor.getConditions().get(rowIndex)) {
                SampleInstanceEntityTsk sampleInstanceEntityTsk = new SampleInstanceEntityTsk();
                sampleInstanceEntityTsk.setSubTask(subTask);
                sampleInstanceEntity.addSubTasks(sampleInstanceEntityTsk);
            }

            labVessels.add(labVessel);
            sampleInstanceEntities.add(sampleInstanceEntity);
            mapBarcodeToVessel.put(labVessel.getLabel(),labVessel);
            ++rowIndex;
        }

        labVesselDao.persistAll(labVessels);
        sampleInstanceEntityDao.persistAll(sampleInstanceEntities);

        if (rowIndex > 0) {
            messageCollection.addInfo("Spreadsheet with " + String.valueOf(rowIndex) + " rows successfully uploaded!");
        } else {
            messageCollection.addError("No valid data found.");
        }
    }


    /**
     * Verifies the spreadsheet contents and if it's ok, persists the data.
     */
    public void verifyAndPersistSpreadsheet(VesselPooledTubesProcessor vesselSpreadsheetProcessor,
            MessageCollection messageCollection, boolean overWriteFlag) {

        int rowOffset = vesselSpreadsheetProcessor.getHeaderRowIndex() + 2; //converts index to 1-based row number
        collaboratorSampleId.clear();
        collaboratorParticipantId.clear();
        broadParticipantId.clear();
        gender.clear();
        species.clear();
        lsid.clear();
        barcodes.clear();
        molecularIndexSchemes.clear();
        reagents.clear();
        broadSampleMap.clear();
        rootSampleMap.clear();
        barcodeToVolumeMap.clear();
        bspSampleDataMap.clear();

        mapBarcodeToVessel = labVesselDao.findByBarcodes( vesselSpreadsheetProcessor.getBarcodes());
        Set<String> libraryNames = new HashSet<>();
        int rowIndex = 0;
        for (String libraryName : vesselSpreadsheetProcessor.getSingleSampleLibraryName()) {
            if (!libraryNames.add(libraryName)) {
                messageCollection.addError("Duplicate Library " + libraryName + " found at row " +
                        (rowIndex + rowOffset));
            }
            if (sampleInstanceEntityDao.findByName(libraryName) != null && !overWriteFlag) {
                messageCollection.addError("Library " + libraryName + " at row " + (rowIndex + rowOffset) +
                        " already exists. Set Overwrite checkbox to re-upload it.");
            }
            rowIndex++;
        }

        //Are Tubes registered.
        rowIndex = 0;
        Set<String> barcodeAndMis = new HashSet<>();
        for (String barcode : vesselSpreadsheetProcessor.getBarcodes()) {
            if (StringUtils.isBlank(barcode)) {
                messageCollection.addError("Tube barcode is blank at row " + (rowIndex + rowOffset));
            } else if (mapBarcodeToVessel.get(barcode) != null && !overWriteFlag) {
                messageCollection.addError("Tube " + barcode + " at row " + (rowIndex + rowOffset) +
                        " already exists. Set Overwrite checkbox to re-upload it.");
            } else {
                barcodes.add(barcode);
            }
            // Errors if a tube has duplicate Molecular Index Scheme.
            String misName = vesselSpreadsheetProcessor.getMolecularIndexingScheme().get(rowIndex);
            if (!barcodeAndMis.add(barcode + "_" + misName)) {
                messageCollection.addError("Duplicate Molecular Indexing Scheme " + misName + " in tube " +
                        barcode + " found at row " + (rowIndex + rowOffset));
            }

            // Maps tube barcode to its first non-zero value for volume.
            if (!barcodeToVolumeMap.containsKey(barcode)) {
                String value = vesselSpreadsheetProcessor.getVolume().get(rowIndex);
                if (StringUtils.isNotBlank(value) && StringUtils.isNumeric(value)) {
                    BigDecimal volume = new BigDecimal(value);
                    if (volume.compareTo(BigDecimal.ZERO) > 0) {
                        barcodeToVolumeMap.put(barcode, volume);
                    }
                }
            }
            rowIndex++;
        }

        // Every tube must have a volume.
        Set<String> uniqueBarcodes = new HashSet<>(barcodes);
        uniqueBarcodes.removeAll(barcodeToVolumeMap.keySet());
        if (!uniqueBarcodes.isEmpty()) {
            messageCollection.addError(Noun.pluralOf("Tube", uniqueBarcodes.size()) +
                    " missing a value for volume: " + StringUtils.join(uniqueBarcodes, ", "));
        }

        //Does molecular index scheme exist.
        rowIndex = 0;
        Multimap<String, String> mapMisAndSampleToBarcodes = HashMultimap.create();
        for (String molecularIndexScheme : vesselSpreadsheetProcessor.getMolecularIndexingScheme()) {
            MolecularIndexingScheme molecularIndexingScheme =
                    molecularIndexingSchemeDao.findByName(molecularIndexScheme);
            if (molecularIndexingScheme == null) {
                messageCollection.addError("Unknown Molecular Indexing Scheme " + molecularIndexScheme +
                        " found at row " + (rowIndex + rowOffset));
            }
            molecularIndexSchemes.add(molecularIndexingScheme);
            String broadSample = vesselSpreadsheetProcessor.getBroadSampleId().get(rowIndex);
            mapMisAndSampleToBarcodes.put(molecularIndexScheme + ", " + broadSample,
                    vesselSpreadsheetProcessor.getBarcodes().get(rowIndex));
            rowIndex++;
        }
        // Warns if the spreadsheet has duplicate combination of Broad Sample and Molecular Index Scheme.
        // It's not an error as long as the tubes don't get pooled later on, which isn't known at upload time.
        for (String misSample : mapMisAndSampleToBarcodes.keySet()) {
            if (mapMisAndSampleToBarcodes.get(misSample).size() > 1) {
                List<String> barcodes = new ArrayList<>(new HashSet<>(mapMisAndSampleToBarcodes.get(misSample)));
                Collections.sort(barcodes);
                messageCollection.addWarning("The same Molecular Indexing Scheme and Broad Sample (" + misSample +
                        ") was found in tubes " + StringUtils.join(barcodes, ", "));
            }
        }

        //Was both bait and cat specified.
        rowIndex = 0;
        for (String bait : vesselSpreadsheetProcessor.getBait()) {
            ReagentDesign reagentDesignBait = reagentDesignDao.findByBusinessKey(bait);
            String cat = vesselSpreadsheetProcessor.getCat().get(rowIndex);
            ReagentDesign reagentDesignCat = reagentDesignDao.findByBusinessKey(cat);

            if (!bait.isEmpty() && !cat.isEmpty()) {
                messageCollection.addError("A row must not have both a Bait and a CAT, found at row " +
                        (rowIndex + rowOffset));
            }
            if ((!bait.isEmpty() && cat.isEmpty()) && reagentDesignBait == null) {
                messageCollection.addError("Unknown bait " + bait + " found at row " + (rowIndex + rowOffset));
            }
            if ((!cat.isEmpty() && bait.isEmpty()) && reagentDesignCat == null) {
                messageCollection.addError("Unknown cat " + cat + " found at row " + (rowIndex + rowOffset));
            }
            // It's ok if both are null, and indicates no reagents on this row.
            reagents.add(reagentDesignBait != null ? reagentDesignBait : reagentDesignCat);
            ++rowIndex;
        }

        //Find the Jira ticket & list of dev conditions (sub tasks) for the experiment.
        Multimap<String, String> experimentConditions = HashMultimap.create();
        Multimap<String, Integer> experimentConditionsRow = HashMultimap.create();
        for (rowIndex = 0; rowIndex < vesselSpreadsheetProcessor.getBarcodes().size(); ++rowIndex) {
            String experiment = vesselSpreadsheetProcessor.getExperiment().get(rowIndex);
            if (StringUtils.isBlank(experiment)) {
                messageCollection.addError("Experiment is blank at row " + (rowIndex + rowOffset));
            }
            List<String> conditions = vesselSpreadsheetProcessor.getConditions().get(rowIndex);
            if (CollectionUtils.isEmpty(conditions)) {
                messageCollection.addError("Conditions is blank at row " + (rowIndex + rowOffset));
            } else {
                experimentConditions.putAll(experiment, conditions);
                // Collects all row numbers that have the same experiment & condition.
                for (String condition : conditions) {
                    experimentConditionsRow.put(experiment + " " + condition, rowIndex + rowOffset);
                }
            }
        }

        // Iterates on experiment and its conditions and checks for their existence.
        for (String experiment : experimentConditions.keySet()) {
            // Gets the Experiment JIRA ticket.
            JiraIssue jiraIssue = null;
            try {
                jiraIssue = jiraService.getIssueInfo(experiment, (String[]) null);
            } catch (Exception e) {
                // Silently ignores any Jira exception.
            }
            if (jiraIssue == null) {
                // Errors the rows that have this unknown experiment.
                List<Integer> rowNumbers = new ArrayList<>();
                for (int i = 0; i < vesselSpreadsheetProcessor.getBarcodes().size(); ++i) {
                    if (vesselSpreadsheetProcessor.getExperiment().get(rowIndex).equals(experiment)) {
                        rowNumbers.add(i + rowOffset);
                    }
                }
                Collections.sort(rowNumbers);
                messageCollection.addError("Unknown DEV ticket " + experiment + " given for Experiment at " +
                        Noun.pluralOf("row", rowNumbers.size()) + " " + StringUtils.join(rowNumbers, ", "));
            } else {
                // Checks that the Conditions are listed as sub-tasks on the Experiment JIRA ticket.
                Collection<String> jiraSubTasks = CollectionUtils.emptyIfNull(jiraIssue.getSubTaskKeys());
                Set<String> invalidConditions = new HashSet<>(experimentConditions.get(experiment));
                invalidConditions.removeAll(jiraSubTasks);
                for (String condition : invalidConditions) {
                    List<Integer> rowNumbers = new ArrayList<>(experimentConditionsRow.get(
                            experiment + " " + condition));
                    Collections.sort(rowNumbers);
                    messageCollection.addError("Experiment ticket " + experiment +
                            " does not have a sub-task for Condition " + condition + " found at " +
                            Noun.pluralOf("row", rowNumbers.size()) + " " + StringUtils.join(rowNumbers, ", "));
                }
            }
        }

        // Fetches Broad samples and root samples from Mercury. If not in Mercury then fetches from BSP.
        Set<String> fetchFromBsp = new HashSet<>();
        for (rowIndex = 0; rowIndex < vesselSpreadsheetProcessor.getBroadSampleId().size(); ++rowIndex) {
            String broadSampleName = vesselSpreadsheetProcessor.getBroadSampleId().get(rowIndex);
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(broadSampleName);
            if (mercurySample == null) {
                fetchFromBsp.add(broadSampleName);
            } else {
                broadSampleMap.put(broadSampleName, mercurySample);
            }
            String rootSampleName = vesselSpreadsheetProcessor.getRootSampleId().get(rowIndex);
            mercurySample = mercurySampleDao.findBySampleKey(rootSampleName);
            if (mercurySample == null) {
                fetchFromBsp.add(rootSampleName);
            } else {
                rootSampleMap.put(rootSampleName, mercurySample);
            }
        }
        bspSampleDataMap.clear();
        bspSampleDataMap.putAll(bspSampleDataFetcher.fetchSampleData(fetchFromBsp,
                BSPSampleSearchColumn.ROOT_SAMPLE,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
                BSPSampleSearchColumn.PARTICIPANT_ID,
                BSPSampleSearchColumn.GENDER,
                BSPSampleSearchColumn.SPECIES,
                BSPSampleSearchColumn.LSID,
                BSPSampleSearchColumn.MATERIAL_TYPE));

        // Check Broad Sample Name and any contingent fields.
        rowIndex = 0;
        for (String broadSampleName : vesselSpreadsheetProcessor.getBroadSampleId()) {
            // These are possibly blank, but needed to keep the lists in order.
            collaboratorSampleId.add(vesselSpreadsheetProcessor.getCollaboratorSampleId().get(rowIndex));
            collaboratorParticipantId.add(vesselSpreadsheetProcessor.getCollaboratorParticipantId().get(rowIndex));
            broadParticipantId.add(vesselSpreadsheetProcessor.getBroadParticipantId().get(rowIndex));
            gender.add(vesselSpreadsheetProcessor.getGender().get(rowIndex));
            species.add(vesselSpreadsheetProcessor.getSpecies().get(rowIndex));
            lsid.add(vesselSpreadsheetProcessor.getLsid().get(rowIndex));

            boolean needsMetadata = !broadSampleMap.containsKey(broadSampleName) &&
                    !bspSampleDataMap.containsKey(broadSampleName);
            // Errors if it's a new Broad Sample name that could collide with a future BSP SM-id.
            if (needsMetadata && BSPUtil.isInBspFormat(broadSampleName)) {
                messageCollection.addError("Broad Sample " + broadSampleName +
                        " that is not in BSP cannot be have the format of a BSP sample name. Found at line " +
                        (rowIndex + rowOffset));
            }
            // Validates root sample name. If it's non-blank for a BSP sample, it must match the BSP metadata.
            String rootSampleName = vesselSpreadsheetProcessor.getRootSampleId().get(rowIndex);
            if (StringUtils.isBlank(rootSampleName)) {
                checkContingentFields(needsMetadata, rootSampleName,
                        VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), rowIndex + rowOffset,
                        messageCollection);
            } else {
                if (bspSampleDataMap.containsKey(broadSampleName)) {
                    String bspMetadataRootSample = bspSampleDataMap.get(broadSampleName).getRootSample();
                    if (!rootSampleName.equals(bspMetadataRootSample)) {
                        messageCollection.addError("Root Sample in BSP (" + bspMetadataRootSample +
                                ") does not match Root Sample " + rootSampleName + " at row " + (rowIndex + rowOffset));
                    }
                }
            }
            // The contingent fields must not be blank if the sample is unknown and needs to be created
            // in this class. If the sample is already in either Mercury or BSP the fields must be blank
            // since updating the sample metadata from spreadsheet values is not allowed.
            checkContingentFields(needsMetadata, collaboratorSampleId.get(rowIndex),
                    VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), rowIndex + rowOffset,
                    messageCollection);
            checkContingentFields(needsMetadata, collaboratorParticipantId.get(rowIndex),
                    VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(), rowIndex + rowOffset,
                    messageCollection);
            checkContingentFields(needsMetadata, broadParticipantId.get(rowIndex),
                    VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), rowIndex + rowOffset,
                    messageCollection);
            checkContingentFields(needsMetadata, gender.get(rowIndex),
                    VesselPooledTubesProcessor.Headers.GENDER.getText(), rowIndex + rowOffset, messageCollection);
            checkContingentFields(needsMetadata, species.get(rowIndex),
                    VesselPooledTubesProcessor.Headers.SPECIES.getText(), rowIndex + rowOffset, messageCollection);
            checkContingentFields(needsMetadata, lsid.get(rowIndex),
                    VesselPooledTubesProcessor.Headers.LSID.getText(), rowIndex + rowOffset, messageCollection);

            if (StringUtils.isBlank(vesselSpreadsheetProcessor.getFragmentSize().get(rowIndex)) ||
                    !StringUtils.isNumeric(vesselSpreadsheetProcessor.getFragmentSize().get(rowIndex))) {
                messageCollection.addError("A number must be given for Fragment Size at row " + (rowIndex + rowOffset));
            }
            if (StringUtils.isNotBlank(vesselSpreadsheetProcessor.getReadLength().get(rowIndex)) &&
                    !StringUtils.isNumeric(vesselSpreadsheetProcessor.getReadLength().get(rowIndex))) {
                messageCollection.addError("Read Length is not a number, at row " + (rowIndex + rowOffset));
            }
            ++rowIndex;
        }

        // If there are no errors attempt save the data to the database.
        if (!messageCollection.hasErrors()) {
            persistResults(vesselSpreadsheetProcessor, messageCollection);
        }
    }

    /** Adds an error message if a contingent field is missing or present. */
    private void checkContingentFields(boolean mustBePresent, String value, String columnName, int rowNumber,
            MessageCollection messageCollection) {
        if (mustBePresent && StringUtils.isBlank(value)) {
            messageCollection.addError("Broad Sample is unknown so " + columnName + " must not be blank at row " +
                    rowNumber);
        } else if (!mustBePresent && StringUtils.isNotBlank(value)) {
            messageCollection.addError("Broad Sample already exists so " + columnName + " must be blank at row " +
                    rowNumber);
        }
    }

    /** Adds library size metric to the given lab vessel. */
    private void addLibrarySize(LabVessel labVessel, BigDecimal librarySize) {
        // Does not add the same value if it already exists for the that vessel.
        for (LabMetric labMetric: labVessel.getMetrics()) {
            if (labMetric.getName() == LabMetric.MetricType.FINAL_LIBRARY_SIZE &&
                    labMetric.getValue().equals(librarySize)) {
                return;
            }
        }
        LabMetric finalLibrarySizeMetric = new LabMetric(librarySize, LabMetric.MetricType.FINAL_LIBRARY_SIZE,
                LabMetric.LabUnit.UG_PER_ML, null, new Date());
        labVessel.addMetric(finalLibrarySizeMetric);
    }
}