package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Handles spreadsheet uploads for "Pooled Tube" external DEV samples.
 */
public class VesselPooledTubesProcessor extends ExternalLibraryProcessor {
    private List<String> broadSampleId = new ArrayList<>();
    private List<String> rootSampleId = new ArrayList<>();
    private List<String> bait = new ArrayList<>();
    private List<String> cat = new ArrayList<>();
    private List<String> experiment = new ArrayList<>();
    // conditions is a per-row list of one or more DEV ticket ids.
    private List<List<String>> conditions = new ArrayList<>();
    private List<String> collaboratorParticipantId = new ArrayList<>();
    private List<String> broadParticipantId = new ArrayList<>();
    private List<String> lsid = new ArrayList<>();
    private List<String> fragmentSize = new ArrayList<>();
    private List<Boolean> requiredValuesPresent = new ArrayList<>();

    public VesselPooledTubesProcessor(String sheetName) {
        super(sheetName);
        headerValueNames.clear();
    }

    @Override
    public HeaderValueRow[] getHeaderValueRows() {
        return new HeaderValueRow[0];
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowNumber, boolean requiredValuesPresent) {
        bait.add(getFromRow(dataRow, Headers.BAIT));
        barcodes.add(getFromRow(dataRow, Headers.TUBE_BARCODE));
        broadParticipantId.add(getFromRow(dataRow, Headers.BROAD_PARTICIPANT_ID));
        broadSampleId.add(getFromRow(dataRow, Headers.BROAD_SAMPLE_ID));
        cat.add(getFromRow(dataRow, Headers.CAT));
        collaboratorParticipantId.add(getFromRow(dataRow, Headers.COLLABORATOR_PARTICIPANT_ID));
        collaboratorSampleId.add(getFromRow(dataRow, Headers.COLLABORATOR_SAMPLE_ID));
        conditions.add(Arrays.asList(StringUtils.stripAll(getFromRow(dataRow, Headers.CONDITIONS).split(","))));
        experiment.add(getFromRow(dataRow, Headers.EXPERIMENT));
        fragmentSize.add(getFromRow(dataRow, Headers.FRAGMENT_SIZE));
        sex.add(getFromRow(dataRow, Headers.GENDER));
        lsid.add(getFromRow(dataRow, Headers.LSID));
        molecularBarcodeName.add(getFromRow(dataRow, Headers.MOLECULAR_INDEXING_SCHEME));
        readLength.add(getFromRow(dataRow, Headers.READ_LENGTH));
        rootSampleId.add(getFromRow(dataRow, Headers.ROOT_SAMPLE_ID));
        libraryName.add(getFromRow(dataRow, Headers.LIBRARY_NAME));
        organism.add(getFromRow(dataRow, Headers.SPECIES));
        volume.add(getFromRow(dataRow, Headers.VOLUME));

        this.requiredValuesPresent.add(requiredValuesPresent);
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return Headers.values();
    }

    public enum Headers implements ColumnHeader, ColumnHeader.Ignorable {
        // Required header and data.
        TUBE_BARCODE("Tube Barcode", ColumnHeader.REQUIRED_VALUE, true),
        LIBRARY_NAME("Library Name", ColumnHeader.REQUIRED_VALUE, true),
        BROAD_SAMPLE_ID("Broad Sample Id", ColumnHeader.REQUIRED_VALUE, true),
        CONDITIONS("Conditions", ColumnHeader.REQUIRED_VALUE, true),
        EXPERIMENT("Experiment", ColumnHeader.REQUIRED_VALUE, true),
        MOLECULAR_INDEXING_SCHEME("Molecular Indexing Scheme", ColumnHeader.REQUIRED_VALUE, true),

        // Optional header and data.
        ROOT_SAMPLE_ID("Root Sample Id", ColumnHeader.OPTIONAL_VALUE, true),
        BROAD_PARTICIPANT_ID("Broad Participant Id", ColumnHeader.OPTIONAL_VALUE, true),
        COLLABORATOR_PARTICIPANT_ID("Collaborator Participant Id", ColumnHeader.OPTIONAL_VALUE, true),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample Id", ColumnHeader.OPTIONAL_VALUE, true),
        GENDER("Gender", ColumnHeader.OPTIONAL_VALUE, true),
        LSID("Lsid", ColumnHeader.OPTIONAL_VALUE, true),
        SPECIES("Species", ColumnHeader.OPTIONAL_VALUE, true),
        BAIT("Bait", ColumnHeader.OPTIONAL_VALUE, true),
        CAT("CAT", ColumnHeader.OPTIONAL_VALUE, true),
        FRAGMENT_SIZE("Fragment Size", ColumnHeader.OPTIONAL_VALUE, true),
        READ_LENGTH("Read Length", ColumnHeader.OPTIONAL_VALUE, true),
        VOLUME("Volume", ColumnHeader.OPTIONAL_VALUE, true),
        ;

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
        public boolean isIgnoredValue() {
            return false;
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

    public List<String> getBroadSampleId() {
        return broadSampleId;
    }

    public List<String> getRootSampleId() {
        return rootSampleId;
    }

    public List<String> getBait() {
        return bait;
    }

    public List<String> getCat() {
        return cat;
    }

    public List<String> getExperiment() {
        return experiment;
    }

    public List<List<String>> getConditions() {
        return conditions;
    }

    public List<String> getCollaboratorParticipantId() {
        return collaboratorParticipantId;
    }

    public List<String> getBroadParticipantId() {
        return broadParticipantId;
    }

    public List<String> getLsid() {
        return lsid;
    }

    public List<String> getFragmentSize() {
        return fragmentSize;
    }

    public List<Boolean> getRequiredValuesPresent() {
        return requiredValuesPresent;
    }
}
