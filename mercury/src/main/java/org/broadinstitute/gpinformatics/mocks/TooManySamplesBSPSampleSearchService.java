package org.broadinstitute.gpinformatics.mocks;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Given n samples to search for, this mock returns n + 1 samples,
 * each of which has "3.14" as a value for each search column.
 */
@Alternative
@Priority(0)
@Dependent
public class TooManySamplesBSPSampleSearchService implements BSPSampleSearchService {

    public TooManySamplesBSPSampleSearchService(){}

    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {
        List<Map<BSPSampleSearchColumn,String>> emptyResults = new ArrayList<>();
        Map<BSPSampleSearchColumn,String> emptyColumnMap = new HashMap<>();

        for (BSPSampleSearchColumn searchColumn : BSPSampleSearchColumn.PDO_SEARCH_COLUMNS) {
            emptyColumnMap.put(searchColumn, "3.14");
        }

        int tooManySamples = sampleIDs.size() + 1;
        for (int i = 0; i < tooManySamples; i++) {
            emptyResults.add(emptyColumnMap);
        }

        return emptyResults;
    }
}
