package org.broadinstitute.gpinformatics.mercury.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock {@link BSPSampleSearchService} that finds everything
 * you ask for, but returns bogus columns except for {@link BSPSampleSearchColumn#SPECIES},
 * which will always be "sapiens"
 */
@Alternative
public class EverythingYouAskForYouGetAndItsHuman implements BSPSampleSearchService {
    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.SAMPLE_ID, "Bill the Cat");
            put(BSPSampleSearchColumn.PARTICIPANT_ID, "2");
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "2");
        }};

        final List<Map<BSPSampleSearchColumn, String>> samples = new ArrayList<Map<BSPSampleSearchColumn, String>>();
        for (String sampleID : sampleIDs) {
            samples.add(dataMap);
        }

        return samples;
    }
}