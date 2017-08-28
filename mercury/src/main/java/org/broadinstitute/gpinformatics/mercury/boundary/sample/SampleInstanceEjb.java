package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
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

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
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
    private List<List<String>> jiraSubTaskList = new ArrayList<List<String>>();
    private List<String> collaboratorSampleId = new ArrayList<>();
    private List<String> collaboratorParticipantId = new ArrayList<>();
    private List<String> broadParticipantId = new ArrayList<>();
    private List<String> gender = new ArrayList<>();
    private List<String> species = new ArrayList<>();
    private List<String> lsid = new ArrayList<>();
    private List<String> barcode = new ArrayList<>();
    private List<String> sampleLibraryName = new ArrayList<>();
    private List<MolecularIndexingScheme> molecularIndexSchemes = new ArrayList<>();
    private List<ReagentDesign> reagents = new ArrayList<>();
    private List<MercurySample> mercurySamples = new ArrayList<>();
    private List<MercurySample> mercuryRootSamples = new ArrayList<>();
    private List<Boolean> sampleRegistrationFlag = new ArrayList<>();
    private int rowOffset = 2;


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



    /**
     * Build and return the collaborator metadata for the samples. (If supplied)
     */
    private Set<Metadata> collaboratorSampleIdMetadata(final int index, MercurySample mercurySample) {

        doesMetaDataExist(mercurySample);
        if(!collaboratorSampleId.get(index).isEmpty()) {
            return new HashSet<Metadata>() {{
                add(new Metadata(Metadata.Key.SAMPLE_ID, collaboratorSampleId.get(index)));
                add(new Metadata(Metadata.Key.BROAD_PARTICIPANT_ID, broadParticipantId.get(index)));
                add(new Metadata(Metadata.Key.PATIENT_ID, collaboratorParticipantId.get(index)));
                add(new Metadata(Metadata.Key.GENDER, gender.get(index)));
                add(new Metadata(Metadata.Key.LSID, lsid.get(index)));
                add(new Metadata(Metadata.Key.SPECIES, species.get(index)));
                add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
            }};
        }
        return new HashSet<>();
    }

    /**
     *  Handle instances where metadata is updated / or deleted by an upload.
     */
    private void doesMetaDataExist(MercurySample mercurySample) {

        List<Metadata.Key> metadatas = new ArrayList<>();
        metadatas.add(Metadata.Key.SAMPLE_ID);
        metadatas.add(Metadata.Key.BROAD_PARTICIPANT_ID);
        metadatas.add(Metadata.Key.PATIENT_ID);
        metadatas.add(Metadata.Key.GENDER);
        metadatas.add(Metadata.Key.LSID);
        metadatas.add(Metadata.Key.SPECIES);
        metadatas.add(Metadata.Key.MATERIAL_TYPE);

        HashSet<Metadata> metadataHashSet = new HashSet<>();
        for(Metadata metadata : mercurySample.getMetadata()) {
            if(metadatas.contains(metadata.getKey())) {
                metadataHashSet.add(metadata);
            }
        }

        mercurySample.getMetadata().removeAll(metadataHashSet);

    }

    /**
     * Save uploaded data after it hs been verified by verifySpreadSheet();
     */
    private void persistResults(VesselPooledTubesProcessor vesselSpreadsheetProcessor, MessageCollection messageCollection) {
        int sampleIndex = 0;
        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>();
        Map<String, MercurySample> mercurySampleMap = new HashMap<>();
        Map<String, MercurySample> mercuryRootSampleMap = new HashMap<>();

                List<LabVessel> labVessels = new ArrayList<>();
        for (String sampleId : vesselSpreadsheetProcessor.getBroadSampleId()) {
            LabVessel labVessel = mapBarcodeToVessel.get(vesselSpreadsheetProcessor.getBarcodes().get(sampleIndex));
            String rootSampleId = vesselSpreadsheetProcessor.getRootSampleId().get(sampleIndex);

            if (labVessel == null) {
                labVessel = new BarcodedTube(vesselSpreadsheetProcessor.getBarcodes().get(sampleIndex), BarcodedTube.BarcodedTubeType.MatrixTube);
            }

            //Add volume to lab vessel.
            if(getVolume(vesselSpreadsheetProcessor, labVessel.getLabel()) != null) {
                labVessel.setVolume(getVolume(vesselSpreadsheetProcessor, labVessel.getLabel()));
            }

            //Add library size lab metric to to lab vessel.
            addLibrarySize(labVessel,new BigDecimal(vesselSpreadsheetProcessor.getFragmentSize().get(sampleIndex)));

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(vesselSpreadsheetProcessor.getSingleSampleLibraryName().get(sampleIndex));
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
            }

            //Check if the sample is registered in Mercury.
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleId);
            //Check if the Root sample is registered in Mercury.
            MercurySample mercuryRootSample = mercurySampleDao.findBySampleKey(rootSampleId);

            if (mercurySample == null) {
                mercurySample = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
            }

            Set<Metadata>  metadata = collaboratorSampleIdMetadata(sampleIndex, mercurySample);

            if (mercuryRootSample == null) {
                mercuryRootSample = new MercurySample(rootSampleId, MercurySample.MetadataSource.MERCURY);
                mercuryRootSample.addMetadata(metadata);
                mercuryRootSample.addLabVessel(labVessel);

            }
            mercurySample.addMetadata(metadata);
            mercurySample.addLabVessel(labVessel);


            mercurySamples.add(mercurySample);
            if(!mercuryRootSamples.contains(mercuryRootSample)) {
                mercuryRootSamples.add(mercuryRootSample);
            }

            mercurySampleMap.put(mercurySample.getSampleKey(),mercuryRootSample);

            if(!rootSampleId.equals(sampleId)) {
                mercuryRootSample.addLabVessel(labVessel);
                mercuryRootSampleMap.put(mercuryRootSample.getSampleKey(),mercuryRootSample);
            }
            else {
                mercuryRootSampleMap.put(mercuryRootSample.getSampleKey(),mercuryRootSample);
            }

            sampleInstanceEntity.setSampleLibraryName(vesselSpreadsheetProcessor.getSingleSampleLibraryName().get(sampleIndex));
            if(reagents.size() > sampleIndex) {
                sampleInstanceEntity.setReagentDesign(reagents.get(sampleIndex));
            }
            if(molecularIndexSchemes.size() > sampleIndex) {
                sampleInstanceEntity.setMolecularIndexScheme(molecularIndexSchemes.get(sampleIndex));
            }
            if(mercurySamples.size() > sampleIndex) {
                sampleInstanceEntity.setMercurySampleId(mercurySamples.get(sampleIndex));
            }
            if(!StringUtils.isEmpty(vesselSpreadsheetProcessor.getReadLength().get(sampleIndex).trim())) {
                sampleInstanceEntity.setReadLength(Integer.valueOf(vesselSpreadsheetProcessor.getReadLength().get(sampleIndex)));
            }
            if(mercuryRootSamples.size() > sampleIndex) {
                sampleInstanceEntity.setRootSample(mercuryRootSamples.get(sampleIndex));
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

            //labVesselDao.persist(labVessel);
            labVessels.add(labVessel);
            sampleInstanceEntities.add(sampleInstanceEntity);
            mapBarcodeToVessel.put(labVessel.getLabel(),labVessel);
            ++sampleIndex;
        }


        labVesselDao.persistAll(labVessels);

       //This fix the potential problem when both the root and sample are the same and do not already exist.
       for(SampleInstanceEntity sampleInstanceEntity : sampleInstanceEntities) {
            if(sampleInstanceEntity.getMercurySample().getMercurySampleId() == null ||
                   sampleInstanceEntity.getRootSample().getMercurySampleId() == null)  {
               if(sampleInstanceEntity.getMercurySample().getSampleKey().equals(
                       sampleInstanceEntity.getRootSample().getSampleKey())) {
                   MercurySample mercurySample = mercurySampleMap.get(sampleInstanceEntity.getMercurySample().getSampleKey());
                   MercurySample mercurySampleRoot = mercuryRootSampleMap.get(sampleInstanceEntity.getRootSample().getSampleKey());
                   sampleInstanceEntity.setMercurySampleId(mercurySample);
                   sampleInstanceEntity.setRootSample(mercurySampleRoot);
               }
            }
         }

         sampleInstanceEntityDao.persistAll(sampleInstanceEntities);


        if (sampleIndex > 0) {
            messageCollection.addInfo("Spreadsheet with " + String.valueOf(sampleIndex) + " rows successfully uploaded!");
        } else {
            messageCollection.addError("No valid data found.");
        }

    }


    /**
     * Verify the spreadsheet contents before attempting to persist data.
     */
    public void verifySpreadSheet(VesselPooledTubesProcessor vesselSpreadsheetProcessor, MessageCollection messageCollection, boolean overWriteFlag) {
        mapBarcodeToVessel = labVesselDao.findByBarcodes( vesselSpreadsheetProcessor.getBarcodes());
        //Is the sample library name unique to the spreadsheet??
        Map<String, String> map = new HashMap<String, String>();
        int mapIndex = 0;
        for (String libraryName : vesselSpreadsheetProcessor.getSingleSampleLibraryName()) {
            map.put(libraryName, libraryName);
            mapIndex++;

            if (mapIndex > map.size()) {
                messageCollection.addError("Single sample library name : " + libraryName + " at Row: " + (mapIndex + 1)
                        + " Column: " + VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText() + " must be unique");
            } else {
                sampleLibraryName.add(libraryName);
            }

            if (sampleInstanceEntityDao.findByName(libraryName) != null && !overWriteFlag) {
                messageCollection.addError("Single sample library name : " + libraryName + " at Row: " + (mapIndex + 1) + " Column: "
                        + VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()
                        + " exists in the database. Please choose the overwrite previous upload option.");
            }
        }

        //Are Tubes registered.
        int barcodeIndex = 0;
        for (String barcode : vesselSpreadsheetProcessor.getBarcodes()) {
            if (barcode == null) {
                messageCollection.addError("Barcode not found: " + barcode.toString() + " At Row: " + (barcodeIndex + rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText());
            } else if (mapBarcodeToVessel.get(barcode) != null && !overWriteFlag) {
                messageCollection.addError("Barcode already registered: " + barcode.toString() + " At Row: " + (barcodeIndex + rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText());
            } else {
                this.barcode.add(barcode);
            }
            barcodeIndex++;
        }

        //Does molecular index scheme exist.
        int molecularIndexSchemeIndex = 0;
        for (String molecularIndexScheme : vesselSpreadsheetProcessor.getMolecularIndexingScheme()) {
            MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(molecularIndexScheme);
            if (molecularIndexingScheme == null) {
                messageCollection.addError("Molecular Indexing Scheme not found: " + molecularIndexScheme.toString()
                        + " At Row: " + (molecularIndexSchemeIndex + rowOffset) + " Column: "
                        + VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText());
            } else {
                this.molecularIndexSchemes.add(molecularIndexingScheme);
            }
            molecularIndexSchemeIndex++;
        }

        //Add root samples
        for (String rootSampleId : vesselSpreadsheetProcessor.getRootSampleId()) {
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(rootSampleId);
            if (mercurySample != null) {
                this.mercuryRootSamples.add(mercurySample);
            }
        }

        //Was both bait and cat specified.
        int baitCatIndex = 0;
        for (String bait : vesselSpreadsheetProcessor.getBait()) {
            ReagentDesign reagentDesignBait = reagentDesignDao.findByBusinessKey(bait);
            String cat = vesselSpreadsheetProcessor.getCat().get(baitCatIndex);
            ReagentDesign reagentDesignCat = reagentDesignDao.findByBusinessKey(cat);

            if (!bait.isEmpty() && !cat.isEmpty()) {
                messageCollection.addError("Found both Bait and CAT on same line. Bait: " + bait + " CAT: " + cat
                        + " At Row: " + (baitCatIndex + rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.BAIT.getText()
                        + " & " + VesselPooledTubesProcessor.Headers.CAT.getText());
            }
            if ((!bait.isEmpty() && cat.isEmpty()) && reagentDesignBait == null) {
                messageCollection.addError("Bait: " + bait + " is not registered. At Row: " + (baitCatIndex + rowOffset)
                        + " Column: " + VesselPooledTubesProcessor.Headers.BAIT.getText());
            }
            if (reagentDesignBait != null) {
                reagents.add(reagentDesignBait);
            }
            if ((!cat.isEmpty() && bait.isEmpty()) && reagentDesignCat == null) {
                messageCollection.addError("Cat: " + cat + " is not registered. At Row: " + (baitCatIndex + rowOffset)
                        + " Column: " + VesselPooledTubesProcessor.Headers.CAT.getText());
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
            if (jiraIssue == null) {
                messageCollection.addError("Dev ticket not found for Experiment: " + experiment + " At Row: " + experimentIndex
                        + " Column: " + VesselPooledTubesProcessor.Headers.EXPERIMENT.getText());
            } else {
                List<String> jiraSubTasks = jiraIssue.getSubTaskKeys();
                if (jiraSubTasks != null && devConditions.size() > 0) {
                    subTaskList = new ArrayList<String>(devCondition.values());
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
                            messageCollection.addError("Condition / Sub Task: " + subTask + " not found for Experiment: "
                                    + experiment + " At Row: " + experimentIndex + " Column: " + VesselPooledTubesProcessor.Headers.CONDITIONS.getText());
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
            if ((mercurySample == null)) {
                checkForOptionalHeaders(vesselSpreadsheetProcessor.getRootSampleId().get(sampleIndex), VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID, sampleIndex, messageCollection);
                this.collaboratorSampleId.add(vesselSpreadsheetProcessor.getCollaboratorSampleId().get(sampleIndex));
                checkForOptionalHeaders(this.collaboratorSampleId.get(sampleIndex), VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID, sampleIndex, messageCollection);
                this.collaboratorParticipantId.add(vesselSpreadsheetProcessor.getCollaboratorParticipantId().get(sampleIndex));
                checkForOptionalHeaders(this.collaboratorParticipantId.get(sampleIndex), VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID, sampleIndex, messageCollection);
                this.broadParticipantId.add(vesselSpreadsheetProcessor.getBroadParticipantId().get(sampleIndex));
                checkForOptionalHeaders(this.broadParticipantId.get(sampleIndex), VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID, sampleIndex, messageCollection);
                this.gender.add(vesselSpreadsheetProcessor.getGender().get(sampleIndex));
                checkForOptionalHeaders(this.gender.get(sampleIndex), VesselPooledTubesProcessor.Headers.GENDER, sampleIndex, messageCollection);
                this.species.add(vesselSpreadsheetProcessor.getSpecies().get(sampleIndex));
                checkForOptionalHeaders(this.species.get(sampleIndex), VesselPooledTubesProcessor.Headers.SPECIES, sampleIndex, messageCollection);
                this.lsid.add(vesselSpreadsheetProcessor.getLsid().get(sampleIndex));
                checkForOptionalHeaders(this.lsid.get(sampleIndex), VesselPooledTubesProcessor.Headers.LSID, sampleIndex, messageCollection);
                sampleRegistrationFlag.add(false);
            } else {
                this.collaboratorSampleId.add(vesselSpreadsheetProcessor.getCollaboratorSampleId().get(sampleIndex));
                this.collaboratorParticipantId.add(vesselSpreadsheetProcessor.getCollaboratorParticipantId().get(sampleIndex));
                this.broadParticipantId.add(vesselSpreadsheetProcessor.getBroadParticipantId().get(sampleIndex));
                this.gender.add(vesselSpreadsheetProcessor.getGender().get(sampleIndex));
                this.species.add(vesselSpreadsheetProcessor.getSpecies().get(sampleIndex));
                this.lsid.add(vesselSpreadsheetProcessor.getLsid().get(sampleIndex));
                mercurySamples.add(mercurySample);
                sampleRegistrationFlag.add(true);
            }

            //Validate if Fragment and Read Length exist that they are actual numbers.
            validateNumber( vesselSpreadsheetProcessor.getFragmentSize().get(sampleIndex),
                    VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE,sampleIndex,messageCollection, true);

            validateNumber(vesselSpreadsheetProcessor.getReadLength().get(sampleIndex),
                    VesselPooledTubesProcessor.Headers.READ_LENGTH,sampleIndex,messageCollection, false);

            ++sampleIndex;
        }

        /**
         * If there are no errors attempt save the data to the database.
         */
        if (!messageCollection.hasErrors()) {
            persistResults(vesselSpreadsheetProcessor, messageCollection);
        }
    }

    /**
     * Method to create errors when no Broad ID was supplied and some/all collaborator information is missing.
     */
    private void checkForOptionalHeaders(String value, VesselPooledTubesProcessor.Headers headers, int index, MessageCollection messageCollection) {
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
     * This intercepts the Jira Issue exception handler and keeps it from preempting the global validation errors
     */
    private JiraIssue getIssueInfoNoException(String key, String... fields) {
        try {
            return jiraService.getIssueInfo(key, fields);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *  Return the first non empty volume for a given barcode(s)
     */
    private BigDecimal getVolume(VesselPooledTubesProcessor vesselSpreadsheetProcessor, String barcode)
    {
        int index = 0;
        BigDecimal volume = BigDecimal.ZERO;
        for (String volumeBarcode : vesselSpreadsheetProcessor.getBarcodes()) {
            if(volumeBarcode.equals(barcode)){
                if(vesselSpreadsheetProcessor.getVolume().get(index).trim().length() > 0) {
                    volume = BigDecimal.valueOf(Float.parseFloat((vesselSpreadsheetProcessor.getVolume().get(index))));
                }
                if (volume.compareTo(BigDecimal.ZERO) > 0) {
                    return volume;
                }
            }
            ++index;
        }
        return null;
    }

    /**
     *  Add library size metric to the given lab vessel.
     */
    private void addLibrarySize(LabVessel labVessel, BigDecimal librarySize)
    {

        //Don't continue to add the same value if it already exists for the that vessel.
        for(LabMetric labMetric: labVessel.getMetrics()) {
            if(labMetric.getName() == LabMetric.MetricType.FINAL_LIBRARY_SIZE && labMetric.getValue().equals(librarySize)) {
                return;
            }
        }
        LabMetric finalLibrarySizeMetric =
                new LabMetric(librarySize, LabMetric.MetricType.FINAL_LIBRARY_SIZE, LabMetric.LabUnit.UG_PER_ML,
                        null, new Date());
        labVessel.addMetric(finalLibrarySizeMetric);
    }

    /**
     *  Check for missing / invalid numeric fields.
     */
    private void validateNumber(String input, VesselPooledTubesProcessor.Headers headers, int index, MessageCollection messageCollection, Boolean required) {

        //Check for fields that are not required, but if they are there, they must contain a valid number.
        if(!required && StringUtils.isEmpty(input.trim())) {
            return;
        }
        if(!required && !StringUtils.isEmpty(input.trim())) {
            if(StringUtils.isNumeric(input)) {
                return;
            }
            else {
                messageCollection.addError("Invalid number:   " + input + " at column: " + headers.getText() + " at row: " + (index + 2));
                return;
            }
        }

        //Check for required fields
        if(StringUtils.isNumeric(input)) {
            return;
        }
        else {
            messageCollection.addError("Invalid or missing number:   " + input + " at column: " + headers.getText() + " at row: " + (index + 2));
        }
    }
}