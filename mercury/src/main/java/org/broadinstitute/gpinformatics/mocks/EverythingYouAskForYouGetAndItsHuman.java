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
 * Mock {@link BSPSampleSearchService} that finds everything
 * you ask for.  Various return bsp columns are hardcoded, in particular
 * species (homo sapiens) and material type (genomic dna).
 *
 * @deprecated Use SampleDataFetcherStub.EverythingYouAskForYouGetAndItsHuman instead.
 */
@Alternative
@Priority(0)
@Deprecated
@Dependent
public class EverythingYouAskForYouGetAndItsHuman implements BSPSampleSearchService {

    public EverythingYouAskForYouGetAndItsHuman(){}

    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {


        final List<Map<BSPSampleSearchColumn, String>> samples = new ArrayList<>();
        for (String sampleId : sampleIDs) {
            samples.add(makeDataMap(sampleId));
        }

        return samples;
    }

    private Map<BSPSampleSearchColumn, String> makeDataMap(final String sampleId) {
        return new HashMap<BSPSampleSearchColumn, String>(){{
                put(BSPSampleSearchColumn.SAMPLE_ID, sampleId);
                put(BSPSampleSearchColumn.PARTICIPANT_ID, "2");
                put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "2");
                put(BSPSampleSearchColumn.MATERIAL_TYPE,"DNA:DNA Genomic");
                put(BSPSampleSearchColumn.SPECIES,"Homo : Homo sapiens");
            }};
    }
}