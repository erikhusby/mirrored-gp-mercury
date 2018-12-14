package org.broadinstitute.gpinformatics.mocks;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.*;

/**
 * Returns a list which contains the columns from @link{BSPSampleSearchColumn#PDO_SEARCH_COLUMNS}
 * but keeps all values null.  Given n sample ids, n "empty" maps are returned.
 */
@Alternative
@Dependent
public class NullValuesBSPSampleSearchService implements BSPSampleSearchService {

    public NullValuesBSPSampleSearchService(){}

    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {
        List<Map<BSPSampleSearchColumn,String>> emptyResults = new ArrayList<>();
        Map<BSPSampleSearchColumn,String> emptyColumnMap = new HashMap<>();

        for (BSPSampleSearchColumn searchColumn : BSPSampleSearchColumn.PDO_SEARCH_COLUMNS) {
            emptyColumnMap.put(searchColumn, null);
        }

        for (String sampleID : sampleIDs) {
            emptyResults.add(emptyColumnMap);
        }

        return emptyResults;
    }
}
