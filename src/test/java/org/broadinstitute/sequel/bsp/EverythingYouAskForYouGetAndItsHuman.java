package org.broadinstitute.sequel.bsp;

import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Mock {@link BSPSampleSearchService} that finds everything
 * you ask for, but returns bogus columns except for {@link BSPSampleSearchColumn#SPECIES},
 * which will always be "sapiens"
 */
@Alternative
public class EverythingYouAskForYouGetAndItsHuman implements BSPSampleSearchService {
    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {
        final List<String[]> samples = new ArrayList<String[]>();
        for (String sampleID : sampleIDs) {
            samples.add(new String[] {
                    "1","2","3","4","5","6","7","sapiens"
            });
        }
        return samples;
    }

    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, List<BSPSampleSearchColumn> resultColumns) {
        return null;
    }
}