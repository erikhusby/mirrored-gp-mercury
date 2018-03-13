package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRowTableProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExternalLibraryProcessor extends HeaderValueRowTableProcessor {
    final static Boolean REQUIRED = true;
    final static Boolean OPTIONAL = false;
    final static Boolean IGNORED = null;

    protected List<String> headerNames = new ArrayList<>();
    protected List<String> headerValueNames = new ArrayList<>();

    // Maps adjusted header name to actual header name.
    protected Map<String, String> adjustedNames = new HashMap<>();

    protected List<String> additionalAssemblyInformation = new ArrayList<>();
    protected List<String> additionalSampleInformation = new ArrayList<>();
    protected List<String> barcodes = new ArrayList<>();
    protected List<String> collaboratorSampleId = new ArrayList<>();
    protected List<String> dataAnalysisType = new ArrayList<>();
    protected List<String> dataSubmission = new ArrayList<>();
    protected List<String> individualName = new ArrayList<>();
    protected List<String> insertSize = new ArrayList<>();
    protected List<String> irbNumber = new ArrayList<>();
    protected List<String> librarySize = new ArrayList<>();
    protected List<String> libraryType = new ArrayList<>();
    protected List<String> molecularBarcodeName = new ArrayList<>();
    protected List<String> numberOfLanes = new ArrayList<>();
    protected List<String> organism = new ArrayList<>();
    protected List<String> pooled = new ArrayList<>();
    protected List<String> projectTitle = new ArrayList<>();
    protected List<String> readLength = new ArrayList<>();
    protected List<String> referenceSequence = new ArrayList<>();
    protected List<String> sequencingTechnology = new ArrayList<>();
    protected List<String> sex = new ArrayList<>();
    protected List<String> singleDoubleStranded = new ArrayList<>();
    protected List<String> libraryName = new ArrayList<>();
    protected List<String> concentration = new ArrayList<>();
    protected List<String> volume = new ArrayList<>();

    public enum HeaderValueRows implements HeaderValueRow {
        EMAIL("Email:", REQUIRED),
        ORGANIZATION("Organization:", REQUIRED),
        FIRST_NAME("First Name:", OPTIONAL),
        LAST_NAME("Last Name:", OPTIONAL),
        ADDRESS("Address:", OPTIONAL),
        CITY("City:", OPTIONAL),
        STATE("State:", OPTIONAL),
        POSTAL_CODE("Postal Code:", OPTIONAL),
        COUNTRY("Country:", OPTIONAL),
        PHONE("Phone:", OPTIONAL),
        COMMON_NAME("Common Name:", OPTIONAL),
        GENUS("Genus:", OPTIONAL),
        SPECIES("Species:", OPTIONAL),
        IRB_REQUIRED("IRB approval required: (Y/N)", OPTIONAL),
        ;

        private String text;
        private boolean requiredHeader;
        private boolean requiredValue;

        private HeaderValueRows(String text, boolean required) {
            this.text = text;
            this.requiredHeader = required;
            this.requiredValue = required;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isRequiredHeader() {
            return requiredHeader;
        }

        @Override
        public boolean isRequiredValue() {
            return requiredValue;
        }

        @Override
        public boolean isDateColumn() {
            return false;
        }

        @Override
        public boolean isStringColumn() {
            return true;
        }
    }

    public ExternalLibraryProcessor(String sheetName) {
        super(sheetName);

        for (HeaderValueRow headerValueRow : getHeaderValueRows()) {
            headerValueNames.add(headerValueRow.getText());
        }
    }

    @Override
    public HeaderValueRow[] getHeaderValueRows() {
        return HeaderValueRows.values();
    }

    /** Returns the canonical HeaderValueRow header names, not the ones that appeared in the spreadsheet. */
    @Override
    public List<String> getHeaderValueNames() {
        return headerValueNames;
    }

    /** Returns the column header names that appeared in the spreadsheet, not the canonical ones. */
    @Override
    public List<String> getHeaderNames() {
        return headerNames;
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        headerNames.addAll(headers);
        for (String header : headers) {
            adjustedNames.put(adjustHeaderName(header), header);
        }
    }

    @Override
    public void close() {
    }

    public String adjustHeaderName(String headerCell) {
        return ExternalLibraryProcessor.fixupHeaderName(headerCell);
    }

    /**
     * Normalizes the spreadsheet header names.
     * - Trims leading and trailing blanks from each word.
     * - Lower cases all words.
     * - Keeps only the first few words.
     * - Ignores everything after a parenthesis.
     * - Substitutes some of the words.
     */
    public static String fixupHeaderName(String headerCell) {
        final Map<String, String> substitutes = new HashMap<String, String>() {{
            put("molecule", "molecular");
            put("bp", "");
            put("bp.", "");
            put("if", "");
            put("for", "");
            put("requested", "");
            put("desired", "");
            put("no", "");
            put("no.", "number");
            put("total", "");
            put("range", "");
            put("description", "");
        }};
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String word : headerCell.trim().toLowerCase().
                replaceFirst("total library", "").
                replaceFirst("sample collaborator", "collaborator sample").
                replaceFirst("sample volume", "volume").
                replaceFirst("sample concentration", "concentration").
                replaceFirst("if applicable", "").
                replaceFirst("single sample", "").
                split(" ")) {
            ++count;
            if (count > 4 || word.startsWith("(")) {
                break;
            }
            if (substitutes.containsKey(word)) {
                word = substitutes.get(word);
            }
            if (StringUtils.isNotBlank(word)) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(word);
            }
        }
        return builder.toString();
    }

    /** Uses the first non-blank data value for the given headers. */
    protected String getFromRow(Map<String, String> dataRow, ColumnHeader... headers) {
        for (ColumnHeader header : headers) {
            String data = dataRow.get(getAdjustedNames().get(adjustHeaderName(header.getText())));
            if (StringUtils.isNotBlank(data)) {
                return data;
            }
        }
        return "";
    }

    /** Returns a mapping of adjusted header name to actual header name. */
    public Map<String, String> getAdjustedNames() {
        return adjustedNames;
    }

    public List<String> getAdditionalAssemblyInformation() {
        return additionalAssemblyInformation;
    }

    public List<String> getAdditionalSampleInformation() {
        return additionalSampleInformation;
    }

    public List<String> getBarcodes() {
        return barcodes;
    }

    public List<String> getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public List<String> getNumberOfLanes() {
        return numberOfLanes;
    }

    public List<String> getDataAnalysisType() {
        return dataAnalysisType;
    }

    public List<String> getReadLength() {
        return readLength;
    }

    public List<String> getIndividualName() {
        return individualName;
    }

    public List<String> getInsertSize() {
        return insertSize;
    }

    public List<String> getIrbNumber() {
        return irbNumber;
    }

    public List<String> getLibrarySize() {
        return librarySize;
    }

    public List<String> getLibraryType() {
        return libraryType;
    }

    public List<String> getMolecularBarcodeName() {
        return molecularBarcodeName;
    }

    public List<String> getOrganism() {
        return organism;
    }

    public List<String> getPooled() {
        return pooled;
    }

    public List<String> getProjectTitle() {
        return projectTitle;
    }

    public List<String> getReferenceSequence() {
        return referenceSequence;
    }

    public List<String> getSequencingTechnology() {
        return sequencingTechnology;
    }

    public List<String> getSex() {
        return sex;
    }

    public List<String> getLibraryName() {
        return libraryName;
    }

    public List<String> getConcentration() {
        return concentration;
    }

    public List<String> getVolume() {
        return volume;
    }

    public String getAddress() {
        return headerValueMap.get(HeaderValueRows.ADDRESS.getText());
    }

    public String getCity() {
        return headerValueMap.get(HeaderValueRows.CITY.getText());
    }

    public String getCommonName() {
        return headerValueMap.get(HeaderValueRows.COMMON_NAME.getText());
    }

    public String getCountry() {
        return headerValueMap.get(HeaderValueRows.COUNTRY.getText());
    }

    public String getEmail() {
        return headerValueMap.get(HeaderValueRows.EMAIL.getText());
    }

    public String getFirstName() {
        return headerValueMap.get(HeaderValueRows.FIRST_NAME.getText());
    }

    public String getGenus() {
        return headerValueMap.get(HeaderValueRows.GENUS.getText());
    }

    public String getIrbRequired() {
        return headerValueMap.get(HeaderValueRows.IRB_REQUIRED.getText());
    }

    public String getLastName() {
        return headerValueMap.get(HeaderValueRows.LAST_NAME.getText());
    }

    public String getOrganization() {
        return headerValueMap.get(HeaderValueRows.ORGANIZATION.getText());
    }

    public String getPhone() {
        return headerValueMap.get(HeaderValueRows.PHONE.getText());
    }

    public String getSpecies() {
        return headerValueMap.get(HeaderValueRows.SPECIES.getText());
    }

    public String getState() {
        return headerValueMap.get(HeaderValueRows.STATE.getText());
    }

    public String getZip() {
        return headerValueMap.get(HeaderValueRows.POSTAL_CODE.getText());
    }

    public Map<String, String> getHeaderValueMap() {
        return headerValueMap;
    }
}
