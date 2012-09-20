package org.broadinstitute.gpinformatics.mercury.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;

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
                    "1","2",sampleID,"4","5","6","7","sapiens", "broadinstitute.org:bsp.prod.sample:1"

            });
        }
        return samples;
    }

    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, List<BSPSampleSearchColumn> resultColumns) {
        return null;
    }
}