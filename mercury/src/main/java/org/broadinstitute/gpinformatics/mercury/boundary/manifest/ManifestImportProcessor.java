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

import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for parsing manifest files which are used when importing sample metadata.
 * <p/>
 * <br/>See:<ul>
 * <li><a href="https://labopsconfluence.broadinstitute.org/display/GPI/Sample+Receipt+and+Accessioning">
 * Sample Receipt and Accessioning</a></li>
 * </ul>
 */
public class ManifestImportProcessor extends TableProcessor {
    public static final String UNKNOWN_HEADER_FORMAT = "Unknown header(s) '%s'.";
    private static final int ALLOWABLE_NUMBER_OF_SHEETS = 1;
    private ColumnHeader[] columnHeaders;
    // This should be a list of Metadata, but that's on a different branch...
    private Collection<ManifestRecord> manifestRecords=new ArrayList<>();

    protected ManifestImportProcessor() {
        super(null);
    }

    @Override
    public List<String> getHeaderNames() {
        return ManifestHeader.headerNames(columnHeaders);
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        List<String> errors = new ArrayList<>();
        Collection<? extends ColumnHeader> foundHeaders =
                ManifestHeader.fromText(errors, headers.toArray(new String[headers.size()]));
        columnHeaders = foundHeaders.toArray(new ColumnHeader[foundHeaders.size()]);
        if (!errors.isEmpty()) {
            addDataMessage(String.format(UNKNOWN_HEADER_FORMAT, errors), row);
        }
    }


    /**
     * Iterate through the data and add it to the manifestList.
     */
    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        ManifestRecord manifestRecord = new ManifestRecord();
        for (Map.Entry<String, String> columnEntry : dataRow.entrySet()) {
            ManifestHeader header = ManifestHeader.fromText(columnEntry.getKey());
            Metadata metadata = new Metadata(header.getMetadataKey(), columnEntry.getValue());
            manifestRecord.getMetadata().add(metadata);
        }
        manifestRecords.add(manifestRecord);
    }

    public Collection<ManifestRecord> getManifestRecords() {
        return manifestRecords;
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return ManifestHeader.values();
    }

    @Override
    public void close() {

    }

    @Override
    public void validateNumberOfWorksheets(int actualNumberOfSheets) throws ValidationException {
        if (actualNumberOfSheets != ALLOWABLE_NUMBER_OF_SHEETS) {
            String errorMessage = String.format("Expected %d Worksheets, but workbook has %d", ALLOWABLE_NUMBER_OF_SHEETS,
                    actualNumberOfSheets);
            throw new ValidationException(errorMessage, getMessages());
        }
    }

}
