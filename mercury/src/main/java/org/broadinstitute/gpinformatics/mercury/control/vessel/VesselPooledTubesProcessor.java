package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    // conditions is a per-row list of dev ticket strings.
    private List<List<String>> conditions = new ArrayList<>();
    private List<String> collaboratorSampleId = new ArrayList<>();
    private List<String> collaboratorParticipantId = new ArrayList<>();
    private List<String> broadParticipantId = new ArrayList<>();
    private List<String> gender = new ArrayList<>();
    private List<String> species = new ArrayList<>();
    private List<String> lsid = new ArrayList<>();
    private List<String> volume = new ArrayList<>();
    private List<String> fragmentSize = new ArrayList<>();
    private List<String> readLength = new ArrayList<>();
    private List<Boolean> requiredValuesPresent = new ArrayList<>();

    public VesselPooledTubesProcessor(String sheetName) {
        super(sheetName, TableProcessor.IgnoreTrailingBlankLines.YES);
    }

    public List<String> getHeaderNames() {
        return headers;
    }

    public void processHeader(List<String> headers, int row) {
        this.headers = headers;
    }

    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        String conditionsString = dataRow.get(Headers.CONDITIONS.getText());
        bait.add(dataRow.get(Headers.BAIT.getText()));
        barcodes.add(dataRow.get(Headers.TUBE_BARCODE.getText()));
        broadParticipantId.add(dataRow.get(Headers.BROAD_PARTICIPANT_ID.getText()));
        broadSampleId.add(dataRow.get(Headers.BROAD_SAMPLE_ID.getText()));
        cat.add(dataRow.get(Headers.CAT.getText()));
        collaboratorParticipantId.add(dataRow.get(Headers.COLLABORATOR_PARTICIPANT_ID.getText()));
        collaboratorSampleId.add(dataRow.get(Headers.COLLABORATOR_SAMPLE_ID.getText()));
        conditions.add(Arrays.asList(StringUtils.stripAll(conditionsString.split(","))));
        experiment.add(dataRow.get(Headers.EXPERIMENT.getText()));
        fragmentSize.add(dataRow.get(Headers.FRAGMENT_SIZE.getText()));
        gender.add(dataRow.get(Headers.GENDER.getText()));
        lsid.add(dataRow.get(Headers.LSID.getText()));
        molecularIndexingScheme.add(dataRow.get(Headers.MOLECULAR_INDEXING_SCHEME.getText()));
        readLength.add(dataRow.get(Headers.READ_LENGTH.getText()));
        rootSampleID.add(dataRow.get(Headers.ROOT_SAMPLE_ID.getText()));
        singleSampleLibraryName.add(dataRow.get(Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
        species.add(dataRow.get(Headers.SPECIES.getText()));
        volume.add(dataRow.get(Headers.VOLUME.getText()));

        this.requiredValuesPresent.add(requiredValuesPresent);
    }

    protected ColumnHeader[] getColumnHeaders() {
        return VesselPooledTubesProcessor.Headers.values();
    }

    public void close() {
    }

    public enum Headers implements ColumnHeader {
        BAIT("Bait", ColumnHeader.OPTIONAL_VALUE, true),
        BROAD_PARTICIPANT_ID("Broad participant ID", ColumnHeader.OPTIONAL_VALUE, true),
        BROAD_SAMPLE_ID("Broad sample ID", ColumnHeader.REQUIRED_VALUE, true),
        CAT("CAT", ColumnHeader.OPTIONAL_VALUE, true),
        COLLABORATOR_PARTICIPANT_ID("Collaborator participant ID", ColumnHeader.OPTIONAL_VALUE, true),
        COLLABORATOR_SAMPLE_ID("Collaborator sample ID", ColumnHeader.OPTIONAL_VALUE, true),
        CONDITIONS("Conditions", ColumnHeader.REQUIRED_VALUE, true),
        EXPERIMENT("Experiment", ColumnHeader.REQUIRED_VALUE, true),
        FRAGMENT_SIZE("Fragment Size", ColumnHeader.OPTIONAL_VALUE, true),
        GENDER("Gender", ColumnHeader.OPTIONAL_VALUE, true),
        LSID("Lsid", ColumnHeader.OPTIONAL_VALUE, true),
        MOLECULAR_INDEXING_SCHEME("Molecular indexing scheme", ColumnHeader.REQUIRED_VALUE, true),
        READ_LENGTH("Read Length", ColumnHeader.OPTIONAL_VALUE, true),
        ROOT_SAMPLE_ID("Root Sample ID", ColumnHeader.OPTIONAL_VALUE, true),
        SINGLE_SAMPLE_LIBRARY_NAME("Single sample library name", ColumnHeader.REQUIRED_VALUE, true),
        SPECIES("Species", ColumnHeader.OPTIONAL_VALUE, true),
        TUBE_BARCODE("Tube barcode", ColumnHeader.REQUIRED_VALUE, true),
        VOLUME("Volume", ColumnHeader.OPTIONAL_VALUE, true);

        private final String text;
        private boolean isRequired;
        private boolean isString;

        Headers(String text, boolean isRequired, boolean isString) {
            this.text = text;
            this.isRequired = isRequired;
            this.isString = isString;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return isRequired;
        }

        @Override
        public boolean isRequiredValue() {
            return isRequired;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return isString;
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

    public List<List<String>> getConditions() {
        return conditions;
    }

    public List<String> getCollaboratorSampleId() { return collaboratorSampleId; }

    public List<String> getBroadParticipantId() { return broadParticipantId; }

    public List<String> getCollaboratorParticipantId() { return collaboratorParticipantId;  }

    public List<String> getGender() { return gender;  }

    public List<String> getSpecies() { return species; }

    public List<String> getLsid() { return lsid; }

    public List<String> getVolume() { return volume; }

    public List<String> getFragmentSize() { return fragmentSize; }

    public List<String> getReadLength() { return readLength;  }

    public List<Boolean> getRequiredValuesPresent() {
        return requiredValuesPresent;
    }
}
