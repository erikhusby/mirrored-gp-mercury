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
    final static boolean REQUIRED = true;
    final static boolean OPTIONAL = false;

    protected List<String> headerNames = new ArrayList<>();
    protected List<String> headerValueNames = new ArrayList<>();

    // Maps adjusted header name to actual header name.
    protected Map<String, String> adjustedNames = new HashMap<>();

    protected List<String> accessList = new ArrayList<>();
    protected List<String> additionalAssemblyInformation = new ArrayList<>();
    protected List<String> additionalSampleInformation = new ArrayList<>();
    protected List<String> approvedBy = new ArrayList<>();
    protected List<String> barcodes = new ArrayList<>();
    protected List<String> cellLine = new ArrayList<>();
    protected List<String> collaboratorSampleId = new ArrayList<>();
    protected List<String> dataAnalysisType = new ArrayList<>();
    protected List<String> dataSubmission = new ArrayList<>();
    protected List<String> derivedFrom = new ArrayList<>();
    protected List<String> fundingSource = new ArrayList<>();
    protected List<String> gssrOfBaitPool = new ArrayList<>();
    protected List<String> illuminaKitUsed = new ArrayList<>();
    protected List<String> individualName = new ArrayList<>();
    protected List<String> insertSize = new ArrayList<>();
    protected List<String> irbNumber = new ArrayList<>();
    protected List<String> jumpSize = new ArrayList<>();
    protected List<String> librarySize = new ArrayList<>();
    protected List<String> libraryType = new ArrayList<>();
    protected List<String> memberOfPool = new ArrayList<>();
    protected List<String> molecularBarcodeName = new ArrayList<>();
    protected List<String> molecularBarcodePlateID = new ArrayList<>();
    protected List<String> molecularBarcodePlateWellID = new ArrayList<>();
    protected List<String> molecularBarcodeSequence = new ArrayList<>();
    protected List<String> numberOfLanes = new ArrayList<>();
    protected List<String> organism = new ArrayList<>();
    protected List<String> pooled = new ArrayList<>();
    protected List<String> projectTitle = new ArrayList<>();
    protected List<String> readLength = new ArrayList<>();
    protected List<String> referenceSequence = new ArrayList<>();
    protected List<String> requestedCompletionDate = new ArrayList<>();
    protected List<String> requiredControlledAccess = new ArrayList<>();
    protected List<String> restrictionEnzymes = new ArrayList<>();
    protected List<String> sampleNumber = new ArrayList<>();
    protected List<String> sequencingTechnology = new ArrayList<>();
    protected List<String> sex = new ArrayList<>();
    protected List<String> singleDoubleStranded = new ArrayList<>();
    protected List<String> singleSampleLibraryName = new ArrayList<>();
    protected List<String> sourceSampleGSSRId = new ArrayList<>();
    protected List<String> squidProject = new ArrayList<>();
    protected List<String> strain = new ArrayList<>();
    protected List<String> submittedToGSSR = new ArrayList<>();
    protected List<String> tissueType = new ArrayList<>();
    protected List<String> totalLibraryConcentration = new ArrayList<>();
    protected List<String> totalLibraryVolume = new ArrayList<>();
    protected List<String> virtualGSSRId = new ArrayList<>();

    public enum HeaderValueRows implements HeaderValueRow {
        FIRST_NAME("First Name:", REQUIRED),
        LAST_NAME("Last Name:", REQUIRED),
        ORGANIZATION("Organization:", REQUIRED),
        ADDRESS("Address:", OPTIONAL),
        CITY("City:", OPTIONAL),
        STATE("State:", OPTIONAL),
        POSTAL_CODE("Postal Code:", OPTIONAL),
        COMMON_NAME("Common Name:", OPTIONAL),
        COUNTRY("Country:", OPTIONAL),
        PHONE("Phone:", OPTIONAL),
        EMAIL("Email:", OPTIONAL),
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

    @Override
    public List<String> getHeaderValueNames() {
        return headerValueNames;
    }

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

    /**
     * Normalizes the spreadsheet header names.
     * Cuts off after four words, after any parenthesis, at the word "bp.".
     * Lower cases all words. Trims blanks off.
     */
    public String adjustHeaderName(String headerCell) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String word : headerCell.trim().toLowerCase().split(" ")) {
            if (count > 3 || word.startsWith("(") || word.equals("bp.")) {
                break;
            }
            if (StringUtils.isNotBlank(word)) {
                if (count > 0) {
                    builder.append(" ");
                }
                builder.append(word);
                ++count;
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

    public List<String> getAccessList() {
        return accessList;
    }

    public List<String> getAdditionalAssemblyInformation() {
        return additionalAssemblyInformation;
    }

    public List<String> getAdditionalSampleInformation() {
        return additionalSampleInformation;
    }

    public List<String> getApprovedBy() {
        return approvedBy;
    }

    public List<String> getBarcodes() {
        return barcodes;
    }

    public List<String> getCellLine() {
        return cellLine;
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

    public List<String> getDataSubmission() {
        return dataSubmission;
    }

    public List<String> getDerivedFrom() {
        return derivedFrom;
    }

    public List<String> getReadLength() {
        return readLength;
    }

    public List<String> getFundingSource() {
        return fundingSource;
    }

    public List<String> getGssrOfBaitPool() {
        return gssrOfBaitPool;
    }

    public List<String> getIlluminaKitUsed() {
        return illuminaKitUsed;
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

    public List<String> getJumpSize() {
        return jumpSize;
    }

    public List<String> getLibrarySize() {
        return librarySize;
    }

    public List<String> getLibraryType() {
        return libraryType;
    }

    public List<String> getMemberOfPool() {
        return memberOfPool;
    }

    public List<String> getMolecularBarcodeName() {
        return molecularBarcodeName;
    }

    public List<String> getMolecularBarcodePlateID() {
        return molecularBarcodePlateID;
    }

    public List<String> getMolecularBarcodePlateWellID() {
        return molecularBarcodePlateWellID;
    }

    public List<String> getMolecularBarcodeSequence() {
        return molecularBarcodeSequence;
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

    public List<String> getRequestedCompletionDate() {
        return requestedCompletionDate;
    }

    public List<String> getRequiredControlledAccess() {
        return requiredControlledAccess;
    }

    public List<String> getRestrictionEnzymes() {
        return restrictionEnzymes;
    }

    public List<String> getSampleNumber() {
        return sampleNumber;
    }

    public List<String> getSequencingTechnology() {
        return sequencingTechnology;
    }

    public List<String> getSex() {
        return sex;
    }

    public List<String> getSingleDoubleStranded() {
        return singleDoubleStranded;
    }

    public List<String> getSingleSampleLibraryName() {
        return singleSampleLibraryName;
    }

    public List<String> getSourceSampleGSSRId() {
        return sourceSampleGSSRId;
    }

    public List<String> getSquidProject() {
        return squidProject;
    }

    public List<String> getStrain() {
        return strain;
    }

    public List<String> getSubmittedToGSSR() {
        return submittedToGSSR;
    }

    public List<String> getTissueType() {
        return tissueType;
    }

    public List<String> getTotalLibraryConcentration() {
        return totalLibraryConcentration;
    }

    public List<String> getTotalLibraryVolume() {
        return totalLibraryVolume;
    }

    public List<String> getVirtualGSSRId() {
        return virtualGSSRId;
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
