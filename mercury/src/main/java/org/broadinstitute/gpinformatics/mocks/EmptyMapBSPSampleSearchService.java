package org.broadinstitute.gpinformatics.mocks;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.*;


/**
 * Returns a completely empty list.
 */
@Alternative
@Dependent
public class EmptyMapBSPSampleSearchService implements BSPSampleSearchService {

    public EmptyMapBSPSampleSearchService(){}

    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {
        return Collections.emptyList();
    }

}
