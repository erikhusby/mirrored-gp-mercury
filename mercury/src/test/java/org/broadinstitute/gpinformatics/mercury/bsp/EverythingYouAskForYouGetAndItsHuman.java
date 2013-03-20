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
            samples.add(new String[] {"Bill the Cat","2",sampleID,"4","5","6","7",
                    "8","9","10","11","12","13","14","15","16","17","18","19","20"});
        }
        return samples;
    }
}