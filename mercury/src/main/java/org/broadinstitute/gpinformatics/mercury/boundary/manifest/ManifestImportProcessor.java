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
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    public static final String UNKNOWN_HEADER_FORMAT = "Ignoring unknown header(s) '%s'.";
    private ColumnHeader[] columnHeaders;
    // This should be a list of Metadata, but that's on a different branch...
    private List<Map<String, String>> manifestList = new ArrayList<>();

    protected ManifestImportProcessor() {
        super(null);
    }

    @Override
    public List<String> getHeaderNames() {
        return ManifestHeader.headerNames(columnHeaders);
    }

    @Override
    public void processHeader(List<String> headers, int row) {
        try {
            // how do we deal with unknown headers?
            // how do we deal with hidden headers?.
            Collection<? extends ColumnHeader> foundHeaders =
                    ManifestHeader.fromValues(headers.toArray(new String[headers.size()]));
            columnHeaders = foundHeaders.toArray(new ColumnHeader[foundHeaders.size()]);
        } catch (EnumConstantNotPresentException e) {
            addWarning(String.format(UNKNOWN_HEADER_FORMAT, e.constantName()), row);
        }
    }


    /**
     * Iterate through the data and add it to the manifestList.
     */
    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
        Map<String, String> manifestRow = new HashMap<>();
        for (Map.Entry<String, String> columnEntry : dataRow.entrySet()) {
            manifestRow.put(columnEntry.getKey(), columnEntry.getValue());
            manifestList.add(manifestRow);
        }
    }

    public List<Map<String, String>> getManifestList() {
        return manifestList;
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return ManifestHeader.values();
    }

    @Override
    public void close() {

    }
}
