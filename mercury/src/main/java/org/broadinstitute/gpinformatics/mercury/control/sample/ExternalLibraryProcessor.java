package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRow;
import org.broadinstitute.gpinformatics.infrastructure.parsers.HeaderValueRowTableProcessor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ExternalLibraryProcessor extends HeaderValueRowTableProcessor {
    final static boolean REQUIRED = true;
    final static boolean OPTIONAL = false;

    protected List<String> headerNames = new ArrayList<>();
    protected List<String> headerValueNames = new ArrayList<>();

    protected List<String> accessList = new ArrayList<>();
    protected List<String> additionalAssemblyInformation = new ArrayList<>();
    protected List<String> additionalSampleInformation = new ArrayList<>();
    protected List<String> approvedBy = new ArrayList<>();
    protected List<String> barcodes = new ArrayList<>();
    protected List<String> cellLine = new ArrayList<>();
    protected List<String> collaboratorSampleId = new ArrayList<>();
    protected List<String> coverage = new ArrayList<>();
    protected List<String> dataAnalysisType = new ArrayList<>();
    protected List<String> dataSubmission = new ArrayList<>();
    protected List<String> derivedFrom = new ArrayList<>();
    protected List<String> desiredReadLength = new ArrayList<>();
    protected List<String> fundingSource = new ArrayList<>();
    protected List<String> gssrOfBaitPool = new ArrayList<>();
    protected List<String> illuminaKitUsed = new ArrayList<>();
    protected List<String> individualName = new ArrayList<>();
    protected List<String> insertSizeRangeBp = new ArrayList<>();
    protected List<String> irbNumber = new ArrayList<>();
    protected List<String> jumpSize = new ArrayList<>();
    protected List<String> librarySizeRangeBp = new ArrayList<>();
    protected List<String> libraryType = new ArrayList<>();
    protected List<String> memberOfPool = new ArrayList<>();
    protected List<String> molecularBarcodeName = new ArrayList<>();
    protected List<String> molecularBarcodePlateID = new ArrayList<>();
    protected List<String> molecularBarcodePlateWellID = new ArrayList<>();
    protected List<String> molecularBarcodeSequence = new ArrayList<>();
    protected List<String> organism = new ArrayList<>();
    protected List<String> pooled = new ArrayList<>();
    protected List<String> projectTitle = new ArrayList<>();
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
        this(sheetName, IgnoreTrailingBlankLines.YES);
    }

    public ExternalLibraryProcessor(String sheetName, @Nonnull IgnoreTrailingBlankLines ignoreTrailingBlankLines) {
        super(sheetName, ignoreTrailingBlankLines);

        for (HeaderValueRow headerValueRow : getHeaderValueRows()) {
            headerValueNames.add(headerValueRow.getText());
        }
        for (ColumnHeader columnHeader : getColumnHeaders()) {
            headerNames.add(columnHeader.getText());
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
    }

    @Override
    public void processSubHeaders(List<String> headers) {
    }

    @Override
    public void close() {
    }

    /** Strips parethetical material off and trim blanks off of the header cell before matching it. */
    public String adjustHeaderCell(String headerCell) {
        return StringUtils.substringBefore(headerCell, "(").trim();
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

    public List<String> getCoverage() {
        return coverage;
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

    public List<String> getDesiredReadLength() {
        return desiredReadLength;
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

    public List<String> getInsertSizeRangeBp() {
        return insertSizeRangeBp;
    }

    public List<String> getIrbNumber() {
        return irbNumber;
    }

    public List<String> getJumpSize() {
        return jumpSize;
    }

    public List<String> getLibrarySizeRangeBp() {
        return librarySizeRangeBp;
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
