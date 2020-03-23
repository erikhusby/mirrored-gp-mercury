/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.infrastructure.parsers.AccessioningColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This enum holds header information for sample metadata manifests.
 */
public enum ManifestHeader implements AccessioningColumnHeader {
    SPECIMEN_NUMBER("Specimen_Number", Metadata.Key.SAMPLE_ID),
    SEX("Sex", Metadata.Key.GENDER),
    PATIENT_ID("Patient_ID", Metadata.Key.PATIENT_ID),
    COLLECTION_DATE("Collection_Date", Metadata.Key.BUICK_COLLECTION_DATE),
    VISIT("Visit", Metadata.Key.BUICK_VISIT),
    TUMOR_OR_NORMAL("SAMPLE_TYPE", Metadata.Key.TUMOR_NORMAL),
    MATERIAL_TYPE("Material Type", Metadata.Key.MATERIAL_TYPE);

    private final String columnName;
    private final Metadata.Key metadataKey;

    ManifestHeader(String columnName, Metadata.Key metadataKey) {
        this.columnName = columnName;
        this.metadataKey = metadataKey;
    }

    @Override
    public Metadata.Key getMetadataKey() {
        return metadataKey;
    }

    @Override
    public String getText() {
        return getColumnName();
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public boolean isRequiredHeader() {
        return true;
    }

    @Override
    public boolean isRequiredValue() {
        return ColumnHeader.REQUIRED_VALUE;
    }

    /**
     * We only hold string values, even for dates.
     */
    @Override
    public boolean isDateColumn() {
        return false;
    }

    /**
     * Since we only hold string values of data, always return true.
     */
    @Override
    public boolean isStringColumn() {
        return true;
    }

    @Override
    public boolean isIgnoreColumn() {
        return false;
    }

    /**
     * Lookup all ColumnHeaders matching column names.
     *
     * @param columnNames column names to find enum values for.
     *
     * @return Collection of ColumnHeaders for the columnNames
     */
    static Collection<ManifestHeader> fromColumnName(List<String> errors, String... columnNames) {
        List<ManifestHeader> matches = new ArrayList<>();
        for (String columnName : columnNames) {
            try {
                matches.add(ManifestHeader.fromColumnName(columnName));
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
    public static ManifestHeader fromColumnName(String columnHeader) {
        for (ManifestHeader manifestHeader : ManifestHeader.values()) {
            if (manifestHeader.getColumnName().equals(columnHeader)) {
                return manifestHeader;
            }
        }
        throw new IllegalArgumentException(AccessioningColumnHeader.NO_MANIFEST_HEADER_FOUND_FOR_COLUMN + columnHeader);
    }

    /**
     * Look up the ManifestHeader for given Metadata.Key.
     *
     * @param key Metadata.Key to search for.
     *
     * @return ManifestHeader with Metadata.Key matching key
     *
     * @throws EnumConstantNotPresentException if enum does not exist for key.
     */

    public static ManifestHeader fromMetadataKey(Metadata.Key key) {
        for (ManifestHeader manifestHeader : ManifestHeader.values()) {
            if (manifestHeader.getMetadataKey() == key) {
                return manifestHeader;
            }
        }
        throw new EnumConstantNotPresentException(ManifestHeader.class, key.name());
    }

    /**
     * Create an array of Metadata for the the data row.
     *
     * @param dataRow key-value mapping of header to row value.
     */
    public static Metadata[] toMetadata(Map<String, String> dataRow) {
        List<Metadata> metadata = new ArrayList<>(dataRow.size());
        for (Map.Entry<String, String> columnEntry : dataRow.entrySet()) {
            ManifestHeader header = ManifestHeader.fromColumnName(columnEntry.getKey());
            metadata.add(new Metadata(header.getMetadataKey(), columnEntry.getValue()));
        }
        return metadata.toArray(new Metadata[metadata.size()]);
    }
}
