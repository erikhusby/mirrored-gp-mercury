package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.withdb.RunTimeAlternatives;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Alternative
@Priority(1)
@Dependent
public class StubbyBSPSampleSearchService implements BSPSampleSearchService {
    @Inject
    private BSPSampleSearchServiceStub defaultService;

    public StubbyBSPSampleSearchService() {
    }

    private BSPSampleSearchService getService() {
        BSPSampleSearchService service = RunTimeAlternatives.getThreadLocalAlternative(BSPSampleSearchService.class);
        return (service == null) ? defaultService : service;
    }

    @Override
    public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(Collection<String> sampleIDs,
            BSPSampleSearchColumn... resultColumns) {
        return getService().runSampleSearch(sampleIDs, resultColumns);
    }
}
