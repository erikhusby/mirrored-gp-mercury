package org.broadinstitute.gpinformatics.mocks;

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
 * you ask for.  Various return bsp columns are hardcoded, in particular
 * species (homo sapiens) and material type (genomic dna).
 */
@Alternative
public class EverythingYouAskForYouGetAndItsHuman implements BSPSampleSearchService {
    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... resultColumns) {

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.SAMPLE_ID, "Bill the Cat");
            put(BSPSampleSearchColumn.PARTICIPANT_ID, "2");
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "2");
            put(BSPSampleSearchColumn.MATERIAL_TYPE,"DNA:DNA Genomic");
            put(BSPSampleSearchColumn.SPECIES,"Homo : Homo sapiens");
        }};

        final List<Map<BSPSampleSearchColumn, String>> samples = new ArrayList<Map<BSPSampleSearchColumn, String>>();
        for (String sampleID : sampleIDs) {
            samples.add(dataMap);
        }

        return samples;
    }
}