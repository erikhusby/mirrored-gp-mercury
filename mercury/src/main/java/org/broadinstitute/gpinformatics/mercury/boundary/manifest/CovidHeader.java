package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.infrastructure.parsers.AccessioningColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CovidHeader implements AccessioningColumnHeader {
    public static CovidHeader PATIENT_ID = new CovidHeader("patient_id",Metadata.Key.PATIENT_ID, false, true);
    public static CovidHeader REQUESTING_PHYSICIAN = new CovidHeader("physician", Metadata.Key.REQUESTING_PHYSICIAN,false, false);
    public static CovidHeader DATE_COLLECTED = new CovidHeader("time_collected", Metadata.Key.COLLECTION_DATE, true, false);
    public static CovidHeader INSTITUTION_ID = new CovidHeader("institution_id", Metadata.Key.INSTITUTE_ID, true, false);
    public static CovidHeader SAMPLE_ID = new CovidHeader("sample_id", Metadata.Key.SAMPLE_ID, true, false);
    public static CovidHeader MATRIX_ID = new CovidHeader("matrix_id", Metadata.Key.BROAD_2D_BARCODE, false, false);
    ;

    public static CovidHeader[] headerValues = Stream.of(PATIENT_ID, REQUESTING_PHYSICIAN, DATE_COLLECTED, INSTITUTION_ID, SAMPLE_ID, MATRIX_ID)
            .toArray(CovidHeader[]::new);

    private final String columnName;
    private final Metadata.Key metadataKey;
    private final boolean required;
    private final boolean ignoreColumn;

    CovidHeader(String columnName, Metadata.Key metadataKey, boolean required, boolean ignoreColumn) {
        this.columnName = columnName;
        this.metadataKey = metadataKey;
        this.required = required;
        this.ignoreColumn = ignoreColumn;
    }

    public static CovidHeader[] values() {
        return headerValues;
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
        return this.required;
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

    @Override
    public boolean isIgnoreColumn() {
        return ignoreColumn;
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
        CovidHeader searchResult = null;
        if (columnHeader != null) {
            for (CovidHeader manifestHeader : CovidHeader.values()) {
                if (manifestHeader.getColumnName().equals(columnHeader)) {
                    searchResult = manifestHeader;
                }
            }
            if (searchResult==null) {
                searchResult = new CovidHeader(columnHeader, Metadata.Key.NA, false, true);
            }
        }

        if (searchResult == null) {
            throw new IllegalArgumentException(AccessioningColumnHeader.NO_MANIFEST_HEADER_FOUND_FOR_COLUMN + columnHeader);
        } else {
            return searchResult;
        }
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
        throw new InformaticsServiceException(String.format("Header element %s not found in Covid Headers", key.name()));
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
            if(!header.isIgnoreColumn()) {
                metadata.add(new Metadata(header.getMetadataKey(), columnEntry.getValue()));
            }
        }
        return metadata.toArray(new Metadata[metadata.size()]);
    }
}
