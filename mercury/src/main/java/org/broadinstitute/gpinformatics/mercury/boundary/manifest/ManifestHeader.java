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

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This enum holds header information for sample metadata manifests.
 */
public enum ManifestHeader implements ColumnHeader {
    STUDY_NUMBER("Study_Number", 0, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    SAMPLE_ID("Sample ID", 1, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    PATIENT_ID("Patient_ID", 2, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    SEX("Sex", 3, ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_VALUE),
    SAMPLE_TYPE("Sample_Type", 4, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    TEST_NAME("TestName", 5, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    COLLECTION_DATE("Collection_Date", 6, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    VISIT("Visit", 7, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    TUMOR_OR_NORMAL("T/N", 8, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE);
    private final String text;
    private final int index;
    private final boolean requiredHeader;
    private final boolean requiredValue;

    ManifestHeader(String text, int index, boolean requiredHeader, boolean requiredValue) {
        this.text = text;
        this.index = index;
        this.requiredHeader = requiredHeader;
        this.requiredValue = requiredValue;
    }

    /**
     * Convenience method for retrieving ColumnHeader text given an list of headers
     *
     * @param columnHeaders Array of ColumnHeaders you would like the text for.
     *
     * @return a List of header names.
     */
    public static List<String> headerNames(ColumnHeader... columnHeaders) {
        List<String> allHeaderNames = new ArrayList<>();
        for (ColumnHeader manifestHeader : columnHeaders) {
            allHeaderNames.add(manifestHeader.getText());
        }
        return allHeaderNames;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean isRequiredHeader() {
        return requiredHeader;
    }

    @Override
    public boolean isRequiredValue() {
        return requiredValue;
    }

    /**
     * We only hold string values, even for dates.
     *
     * @return
     */
    @Override
    public boolean isDateColumn() {
        return false;
    }

    /**
     * Since we only hold string values of data, always return true.
     *
     * @return
     */
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
    static Collection<? extends ColumnHeader> fromValues(String... columnNames) {
        List<ManifestHeader> matches = new ArrayList<>();
        for (String columnName : columnNames) {
            matches.add(ManifestHeader.fromValue(columnName));
        }
        return matches;
    }

    /**
     * Look up the ManifestHeader for given columnName.
     *
     * @param columnName column to search for.
     *
     * @return ManifestHeader for given column.
     *
     * @throws java.lang.EnumConstantNotPresentException if enum does not exist for columnName.
     */
    private static ManifestHeader fromValue(String columnName) {
        for (ManifestHeader manifestHeader : ManifestHeader.values()) {
            if (manifestHeader.getText().equals(columnName)) {
                return manifestHeader;
            }
        }
        throw new EnumConstantNotPresentException(ManifestHeader.class, columnName);
    }
}
