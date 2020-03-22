package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.infrastructure.parsers.AccessioningColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public enum CovidHeader implements AccessioningColumnHeader {
    PATIENT_ID("patient_id",Metadata.Key.PATIENT_ID, false),
    REQUESTING_PHYSICIAN("physician", Metadata.Key.REQUESTING_PHYSICIAN,false),
    DATE_COLLECTED("time_collected", Metadata.Key.COLLECTION_DATE, true),
    INSTITUTION_ID("institution_id", Metadata.Key.INSTITUTE_ID, true),
    SAMPLE_ID("sample_id", Metadata.Key.SAMPLE_ID, true),
    ;

    private final String columnName;
    private final Metadata.Key metadataKey;
    private final boolean required;

    CovidHeader(String columnName, Metadata.Key metadataKey, boolean required) {
        this.columnName = columnName;
        this.metadataKey = metadataKey;
        this.required = required;
    }
    public Metadata.Key getMetadataKey() {
        return metadataKey;
    }

    @Override
    public String getText() {
        return getColumnName();
    }
    public String getColumnName() {
        return columnName;
    }

    @Override
    public boolean isRequiredHeader() {
        return true;
    }

    @Override
    public boolean isRequiredValue() {
        return this.required;
    }

    @Override
    public boolean isDateColumn() {
        return false;
    }

    @Override
    public boolean isStringColumn() {
        return true;
    }

    /**
     * Lookup all ColumnHeaders matching column names.
     *
     * @param columnNames column names to find enum values for.
     *
     * @return Collection of ColumnHeaders for the columnNames
     */
    static Collection<CovidHeader> fromColumnName(List<String> errors, String... columnNames) {
        List<CovidHeader> matches = new ArrayList<>();
        for (String columnName : columnNames) {
            try {
                matches.add(CovidHeader.fromColumnName(columnName));
            } catch (IllegalArgumentException e) {

                // If a header cell is not blank.
                if (!columnName.isEmpty()) {
                    errors.add(columnName);
                }
            }
        }
        return matches;
    }

    /**
     * Look up the ManifestHeader for given columnHeader.
     *
     * @param columnHeader column to search for.
     *
     * @return ManifestHeader for given column.
     *
     * @throws IllegalArgumentException if enum does not exist for columnHeader.
     */
    public static CovidHeader fromColumnName(String columnHeader) {
        for (CovidHeader manifestHeader : CovidHeader.values()) {
            if (manifestHeader.getColumnName().equals(columnHeader)) {
                return manifestHeader;
            }
        }
        throw new IllegalArgumentException(AccessioningColumnHeader.NO_MANIFEST_HEADER_FOUND_FOR_COLUMN + columnHeader);
    }

    /**
     * Look up the CovidHeader for given Metadata.Key.
     *
     * @param key Metadata.Key to search for.
     *
     * @return CovidHeader with Metadata.Key matching key
     *
     * @throws EnumConstantNotPresentException if enum does not exist for key.
     */

    public static CovidHeader fromMetadataKey(Metadata.Key key) {
        for (CovidHeader CovidHeader : CovidHeader.values()) {
            if (CovidHeader.getMetadataKey() == key) {
                return CovidHeader;
            }
        }
        throw new EnumConstantNotPresentException(CovidHeader.class, key.name());
    }


    /**
     * Create an array of Metadata for the the data row.
     *
     * @param dataRow key-value mapping of header to row value.
     */
    public static Metadata[] toMetadata(Map<String, String> dataRow) {
        List<Metadata> metadata = new ArrayList<>(dataRow.size());
        for (Map.Entry<String, String> columnEntry : dataRow.entrySet()) {
            CovidHeader header = CovidHeader.fromColumnName(columnEntry.getKey());
            metadata.add(new Metadata(header.getMetadataKey(), columnEntry.getValue()));
        }
        return metadata.toArray(new Metadata[metadata.size()]);
    }
}
