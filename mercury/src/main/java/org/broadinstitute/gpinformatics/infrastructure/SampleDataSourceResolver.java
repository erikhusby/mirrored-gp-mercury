package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 */
public class SampleDataSourceResolver {

    private final MercurySampleDao mercurySampleDao;

    @Inject
    public SampleDataSourceResolver(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    public Map<String, MercurySample.MetadataSource> resolveSampleDataSources(Collection<String> sampleIds) {
        mercurySampleDao.findMapIdToListMercurySample(sampleIds);
        return Collections.singletonMap(sampleIds.iterator().next(), MercurySample.MetadataSource.BSP);
    }
}
