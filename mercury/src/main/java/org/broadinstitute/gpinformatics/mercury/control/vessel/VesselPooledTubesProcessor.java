package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class VesselPooledTubesProcessor extends TableProcessor {

    private List<String> headers;
    private List<String> barcodes = new ArrayList<>();
    private List<String> singleSampleLibraryName = new ArrayList<>();
    private List<String> broadSampleId = new ArrayList<>();
    private List<String> rootSampleID = new ArrayList<>();
    private List<String> molecularIndexingScheme = new ArrayList<>();
    private List<String> bait = new ArrayList<>();
    private List<String> cat = new ArrayList<>();
    private List<String> experiment = new ArrayList<>();
    private List<String> conditions = new ArrayList<>();
    private List<String> collaboratorSampleId = new ArrayList<>();
    private List<String> collaboratorParticipantId = new ArrayList<>();
    private List<String> broadParticipantId = new ArrayList<>();
    private List<String> gender = new ArrayList<>();
    private List<String> species = new ArrayList<>();
    private List<String> lsid = new ArrayList<>();

    public VesselPooledTubesProcessor(String sheetName) {
        super(sheetName, TableProcessor.IgnoreTrailingBlankLines.YES);
    }


    public List<String> getHeaderNames() {
        return headers;
    }


    public void processHeader(List<String> headers, int row) {
        this.headers = headers;
    }


    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {

        barcodes.add(dataRow.get(Headers.TUBE_BARCODE.getText()));
        singleSampleLibraryName.add(dataRow.get(Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
        broadSampleId.add(dataRow.get(Headers.BROAD_SAMPLE_ID.getText()));
        rootSampleID.add(dataRow.get(Headers.ROOT_SAMPLE_ID.getText()));
        molecularIndexingScheme.add(dataRow.get(Headers.MOLECULAR_INDEXING_SCHEME.getText()));
        bait.add(dataRow.get(Headers.BAIT.getText()));
        cat.add(dataRow.get(Headers.CAT.getText()));
        experiment.add(dataRow.get(Headers.EXPERIMENT.getText()));
        conditions.add(dataRow.get(Headers.CONDITIONS.getText()));
        collaboratorSampleId.add(dataRow.get(Headers.COLLABORATOR_SAMPLE_ID.getText()));
        collaboratorParticipantId.add(dataRow.get(Headers.COLLABORATOR_PARTICIPANT_ID.getText()));
        broadParticipantId.add(dataRow.get(Headers.BROAD_PARTICIPANT_ID.getText()));
        gender.add(dataRow.get(Headers.GENDER.getText()));
        species.add(dataRow.get(Headers.SPECIES.getText()));
        lsid.add(dataRow.get(Headers.LSID.getText()));

    }

    protected ColumnHeader[] getColumnHeaders() {
        return VesselPooledTubesProcessor.Headers.values();
    }

    public void close() {
    }

    public enum Headers implements ColumnHeader {
        TUBE_BARCODE("Tube barcode", ColumnHeader.OPTIONAL_HEADER, true),
        SINGLE_SAMPLE_LIBRARY_NAME("Single sample library name", ColumnHeader.OPTIONAL_HEADER, true),
        BROAD_SAMPLE_ID("Broad sample ID", ColumnHeader.OPTIONAL_HEADER, true),
        ROOT_SAMPLE_ID("Root Sample ID", ColumnHeader.OPTIONAL_HEADER, true),
        MOLECULAR_INDEXING_SCHEME("Molecular indexing scheme", ColumnHeader.OPTIONAL_HEADER, true),
        BAIT("Bait", ColumnHeader.OPTIONAL_HEADER, true),
        CAT("CAT", ColumnHeader.OPTIONAL_HEADER, true),
        EXPERIMENT("Experiment", ColumnHeader.OPTIONAL_HEADER, true),
        CONDITIONS("Conditions", ColumnHeader.OPTIONAL_HEADER, true),
        COLLABORATOR_SAMPLE_ID("Collaborator sample ID", ColumnHeader.OPTIONAL_HEADER, true),
        COLLABORATOR_PARTICIPANT_ID("Collaborator participant ID", ColumnHeader.OPTIONAL_HEADER, true),
        BROAD_PARTICIPANT_ID("Broad participant ID", ColumnHeader.OPTIONAL_HEADER, true),
        GENDER("Gender", ColumnHeader.OPTIONAL_HEADER, true),
        SPECIES("Species", ColumnHeader.OPTIONAL_HEADER, true),
        LSID("Lsid", ColumnHeader.OPTIONAL_HEADER, true);


        private final String text;
        private boolean optionalHeader;
        private boolean isString;

        Headers(String text, boolean optionalHeader, boolean isString) {
            this.text = text;
            this.optionalHeader = optionalHeader;
            this.isString = isString;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return true;
        }

        @Override
        public boolean isRequiredValue() {
            return this.optionalHeader;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return this.isString;
        }
    }

    public List<String> getBarcodes() { return barcodes; }

    public List<String> getSingleSampleLibraryName() { return singleSampleLibraryName;  }

    public List<String> getBroadSampleId() { return broadSampleId; }

    public List<String> getRootSampleId() { return rootSampleID; }

    public List<String> getMolecularIndexingScheme() { return molecularIndexingScheme; }

    public List<String> getBait() { return bait; }

    public List<String> getCat() { return cat; }

    public List<String> getExperiment() { return experiment; }

    public List<Map<String, String>> getConditions() {
        List<Map<String, String>> devConditions = new ArrayList<>();
        if (conditions != null) {
            for (String condition : conditions) {
                String[] devTasks = condition.split(",");
                Map<String, String> map = new HashMap<String, String>();
                for (String devTask : devTasks) {
                    map.put(devTask.trim(), devTask.trim());
                }
                devConditions.add(map);
            }
            return devConditions;
        }
        return null;
    }

    public List<String> getCollaboratorSampleId() { return collaboratorSampleId; }

    public List<String> getBroadParticipantId() { return broadParticipantId; }

    public List<String> getCollaboratorParticipantId() { return collaboratorParticipantId;  }

    public List<String> getGender() { return gender;  }

    public List<String> getSpecies() { return species; }

    public List<String> getLsid() { return lsid; }

}
