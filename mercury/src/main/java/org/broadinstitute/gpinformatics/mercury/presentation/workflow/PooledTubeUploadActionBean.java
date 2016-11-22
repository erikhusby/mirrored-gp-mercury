package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselSpreadsheetProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceSubTasks;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(value = "/workflow/PooledTubeUpload.action")
public class PooledTubeUploadActionBean extends CoreActionBean {

    public static final String UPLOAD_TUBES = "uploadpooledTubes";
    private static final String SESSION_LIST_PAGE = "/workflow/pooledTubeUpload.jsp";
    private boolean overWriteFlag = false;
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
    private List<String> broadSampleId = new ArrayList<>();
    private List<String> rootSampleId = new ArrayList<>();
    private List<ReagentDesign> reagents = new ArrayList<>();
    private List<MercurySample> mercurySamples = new ArrayList<>();
    private List<Boolean> sampleRegistrationFlag = new ArrayList<>();
    private int rowOffset = 2;


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @Inject
    private VesselEjb vesselEjb;

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
    private SampleInstanceDao sampleInstanceDao;

    @Validate(required = true, on = UPLOAD_TUBES)
    private FileBean pooledTubesSpreadsheet;

    /**
     * Entry point for initial upload of spreadsheet.
     */
    @HandlesEvent(UPLOAD_TUBES)
    public Resolution uploadTubes() {
        try {

            VesselSpreadsheetProcessor vesselSpreadsheetProcessor = null;
            vesselSpreadsheetProcessor = new VesselPooledTubesProcessor("Sheet1");

            PoiSpreadsheetParser.processSingleWorksheet(pooledTubesSpreadsheet.getInputStream(), vesselSpreadsheetProcessor);

            verifySpreadSheet(vesselSpreadsheetProcessor);

            if (getValidationErrors().size() <= 0) {
                persistResults(vesselSpreadsheetProcessor);
            }

        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * Build and return the collaborator metadata for the samples. (If supplied)
     */
    private Set<Metadata> collaboratorSampleIdMetadata(final int index) {
        return new HashSet<Metadata>() {{
            add(new Metadata(Metadata.Key.SAMPLE_ID, collaboratorSampleId.get(index)));
            add(new Metadata(Metadata.Key.BROAD_SAMPLE_ID, broadParticipantId.get(index)));
            add(new Metadata(Metadata.Key.PATIENT_ID, collaboratorParticipantId.get(index)));
            add(new Metadata(Metadata.Key.GENDER, gender.get(index)));
            add(new Metadata(Metadata.Key.LSID, lsid.get(index)));
            add(new Metadata(Metadata.Key.SPECIES, species.get(index)));
        }};
    }

    /**
     * Build and return the library name metadata for the samples.
     */
    private Set<Metadata> addLibraryMetaData(final int index) {
        return new HashSet<Metadata>() {{
            add(new Metadata(Metadata.Key.SINGLE_LIBRARY_SAMPLE_NAME, sampleLibraryName.get(index)));
        }};
    }

    /**
     * Save uploaded data after it hs been verified by verifySpreadSheet();
     */
    private void persistResults(VesselSpreadsheetProcessor vesselSpreadsheetProcessor) {
        int sampleIndex = 0;
        for (String sampleId : vesselSpreadsheetProcessor.getBroadSampleId()) {
            LabVessel labVessel = labVesselDao.findByIdentifier(vesselSpreadsheetProcessor.getBarcodes().get(sampleIndex));

            if (labVessel == null) {
                labVessel = new BarcodedTube(vesselSpreadsheetProcessor.getBarcodes().get(sampleIndex),BarcodedTube.BarcodedTubeType.MatrixTube);
            }

            SampleInstance sampleInstance = sampleInstanceDao.findByName(vesselSpreadsheetProcessor.getSingleSampleLibraryName().get(sampleIndex));
            if (sampleInstance == null) {
                sampleInstance = new SampleInstance();
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
            } else //Just add the library name to the metadata for user defined search.
            {
                if (mercurySample == null) {
                    mercurySample = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
                }
                mercurySample.addMetadata(addLibraryMetaData(sampleIndex));
            }

            mercurySample.addLabVessel(labVessel);
            mercurySampleDao.persist(mercurySample);
            mercurySampleDao.flush();
            mercurySamples.add(mercurySample);

            sampleInstance.setSampleLibraryName(vesselSpreadsheetProcessor.getSingleSampleLibraryName().get(sampleIndex));
            sampleInstance.setReagentId(reagents.get(sampleIndex).getReagentDesignId());
            sampleInstance.setMolecularIndexId(molecularIndexSchemes.get(sampleIndex).getMolecularIndexingSchemeId());
            sampleInstance.setMercurySampleId(mercurySamples.get(sampleIndex));
            sampleInstance.setRootSampleId(vesselSpreadsheetProcessor.getRootSampleId().get(sampleIndex));

            sampleInstance.setLabVessel(labVessel);
            sampleInstance.setUploadDate();

            //Persist the dev sub-tasks in the order they where provided.
            for (List<String> subTaskList : jiraSubTaskList) {
                for (String subTask : subTaskList) {
                    SampleInstanceSubTasks sampleInstanceSubTasks = new SampleInstanceSubTasks();
                    sampleInstanceSubTasks.setSubTask(subTask);
                    sampleInstance.addSubTasks(sampleInstanceSubTasks);
                }
            }

            sampleInstanceDao.persist(sampleInstance);
            sampleInstanceDao.flush();

            ++sampleIndex;
        }

        if (sampleIndex > 0) {
            addMessage("Spreadsheet with " + String.valueOf(sampleIndex) + " rows successfully uploaded!");
        } else {
            addGlobalValidationError("No valid data found.");
        }
    }

    /**
     * Verify the spreadsheet contents before attempting to persist data.
     */
    private void verifySpreadSheet(VesselSpreadsheetProcessor vesselSpreadsheetProcessor) {
        //Is the sample library name unique to the spreadsheet??
        Map<String, String> map = new HashMap<String, String>();
        int mapIndex = 0;
        for (String libraryName : vesselSpreadsheetProcessor.getSingleSampleLibraryName()) {
            map.put(libraryName, libraryName);
            mapIndex++;

            if (mapIndex > map.size()) {
                addGlobalValidationError("Single sample library name : " + libraryName + " at Row: " + (mapIndex + 1) + " Column: " + VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText() + " must be unique");
            } else {
                sampleLibraryName.add(libraryName);
            }

            if (sampleInstanceDao.findByName(libraryName) != null) {
                if (sampleInstanceDao.findByName(libraryName).getSampleLibraryName() != null && !overWriteFlag) {
                    addGlobalValidationError("Single sample library name : " + libraryName + " at Row: " + (mapIndex + 1) + " Column: " + VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText() + " exists in the database. Please choose the overwrite previous upload option.");
                }
            }
        }

        //Are Tubes registered.
        int barcodeIndex = 0;
        for (String barcode : vesselSpreadsheetProcessor.getBarcodes()) {
            if (barcode == null) {
                addGlobalValidationError("Barcode not found: " + barcode.toString() + " At Row: " + (barcodeIndex + rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText());
            } else if (labVesselDao.findByIdentifier(barcode) != null && !overWriteFlag) {
                addGlobalValidationError("Barcode already registered: " + barcode.toString() + " At Row: " + (barcodeIndex + rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText());
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
                addGlobalValidationError("Molecular Indexing Scheme not found: " + molecularIndexScheme.toString() + " At Row: " + (molecularIndexSchemeIndex + +rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText());
            } else {
                this.molecularIndexSchemes.add(molecularIndexingScheme);
            }
            molecularIndexSchemeIndex++;
        }

        //Was both bait and cat specified.
        int baitCatIndex = 0;
        for (String bait : vesselSpreadsheetProcessor.getBait()) {
            ReagentDesign reagentDesignBait = reagentDesignDao.findByBusinessKey(bait);
            String cat = vesselSpreadsheetProcessor.getCat().get(baitCatIndex);
            ReagentDesign reagentDesignCat = reagentDesignDao.findByBusinessKey(cat);

            if (bait != "" && cat != "") {
                addGlobalValidationError("Found both Bait and CAT on same line. Bait: " + bait + " CAT: " + cat + " At Row: " + (baitCatIndex + rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.BAIT.getText() + " & " + VesselPooledTubesProcessor.Headers.CAT.getText());
            }
            if ((bait != "" && cat == "") && reagentDesignBait == null) {
                addGlobalValidationError("Bait: " + bait + " is not registered. At Row: " + (baitCatIndex + rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.BAIT.getText());
            }
            if (reagentDesignBait != null) {
                reagents.add(reagentDesignBait);
            }
            if ((cat != "" && bait == "") && reagentDesignCat == null) {
                addGlobalValidationError("Cat: " + cat + " is not registered. At Row: " + (baitCatIndex + rowOffset) + " Column: " + VesselPooledTubesProcessor.Headers.CAT.getText());
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
            {
                String experiment = vesselSpreadsheetProcessor.getExperiment().get(conditionIndex);
                JiraIssue jiraIssue = getIssueInfoNoException(experiment, null);
                List<String> jiraSubTasks = new ArrayList<>();
                if (jiraIssue == null) {
                    addGlobalValidationError("Dev ticket not found for Experiment: " + experiment + " At Row: " + experimentIndex + " Column: " + VesselPooledTubesProcessor.Headers.EXPERIMENT.getText());
                } else {
                    if (jiraIssue != null) {
                        jiraSubTasks = jiraIssue.getSubTasks();
                        if (jiraSubTasks != null && devConditions.size() > 0) {
                            int subTaskIndex = 0;
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
                                    addGlobalValidationError("Condition / Sub Task: " + subTaskList.get(subTaskIndex) + " not found for Experiment: " + experiment + " At Row: " + experimentIndex + " Column: " + VesselPooledTubesProcessor.Headers.EXPERIMENT.getText());
                                }
                                subTaskIndex++;
                            }
                        }
                    }
                }
                jiraSubTaskList.add(subTaskList);
            }
            experimentIndex++;
            conditionIndex++;
        }

        //Is the sample ID registred in Mercury? If not check for optional ID fields.
        int sampleIndex = 0;
        for (String sampleId : vesselSpreadsheetProcessor.getBroadSampleId()) {
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleId);

            //If the sample is missing or not registered check to see if the alternate info was supplied
            if ((sampleId == null || mercurySample == null)) {
                this.collaboratorSampleId.add(vesselSpreadsheetProcessor.getCollaboratorSampleId().get(sampleIndex));
                checkForOptionalHeaders(this.collaboratorSampleId.get(sampleIndex), VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID, sampleIndex);
                this.collaboratorParticipantId.add(vesselSpreadsheetProcessor.getCollaboratorParticipantId().get(sampleIndex));
                checkForOptionalHeaders(this.collaboratorParticipantId.get(sampleIndex), VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID, sampleIndex);
                this.broadParticipantId.add(vesselSpreadsheetProcessor.getBroadParticipantId().get(sampleIndex));
                checkForOptionalHeaders(this.broadParticipantId.get(sampleIndex), VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID, sampleIndex);
                this.gender.add(vesselSpreadsheetProcessor.getGender().get(sampleIndex));
                checkForOptionalHeaders(this.gender.get(sampleIndex), VesselPooledTubesProcessor.Headers.GENDER, sampleIndex);
                this.species.add(vesselSpreadsheetProcessor.getSpecies().get(sampleIndex));
                checkForOptionalHeaders(this.species.get(sampleIndex), VesselPooledTubesProcessor.Headers.SPECIES, sampleIndex);
                this.lsid.add(vesselSpreadsheetProcessor.getLsid().get(sampleIndex));
                checkForOptionalHeaders(this.lsid.get(sampleIndex), VesselPooledTubesProcessor.Headers.LSID, sampleIndex);
                sampleRegistrationFlag.add(false);
            } else {
                mercurySamples.add(mercurySample);
                sampleRegistrationFlag.add(true);
            }
            ++sampleIndex;
        }
    }

    /**
     * Method to create errors when no Broad ID was supplied and some/all collaborator information is missing.
     */
    private void checkForOptionalHeaders(String value, VesselPooledTubesProcessor.Headers headers, int index) {
        if (isFieldEmpty(value)) {
            addGlobalValidationError("No Valid Broad Sample ID found and column " + headers.getText() + " was also missing at Row: " + (index + 2));
        }
    }

    /**
     * Check for empty spreadsheet fields that may have spaces or be null.
     */
    private boolean isFieldEmpty(String field) {
        if (field != null) {
            if (field.trim() == "") {
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

    public void setOverWriteFlag(boolean overWriteFlag) { this.overWriteFlag = overWriteFlag; }

    public void setPooledTubesSpreadsheet(FileBean spreadsheet) { this.pooledTubesSpreadsheet = spreadsheet; }

}


