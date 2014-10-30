package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class SampleDataSourceResolver {

    private final MercurySampleDao mercurySampleDao;

    @Inject
    public SampleDataSourceResolver(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    public Map<String, MercurySample.MetadataSource> resolveSampleDataSources(Collection<String> sampleNames) {
        Map<String, MercurySample> allMercurySamples =
                mercurySampleDao.findMapIdToMercurySample(sampleNames);
        return resolveSampleDataSources(sampleNames, allMercurySamples);
    }

    @DaoFree
    public Map<String, MercurySample.MetadataSource> resolveSampleDataSources(Collection<String> sampleNames,
                Map<String, MercurySample> allMercurySamples) {
        Map<String, MercurySample.MetadataSource> results = new HashMap<>();

        for (String sampleName : sampleNames) {
            Multiset<MercurySample.MetadataSource> metadataSources = HashMultiset.create();

            MercurySample mercurySample = allMercurySamples.get(sampleName);
            MercurySample.MetadataSource metadataSource;

            if(mercurySample != null) {
                metadataSources.add(mercurySample.getMetadataSource());
            }

            if (metadataSources.isEmpty()) {
                metadataSource = MercurySample.MetadataSource.BSP;
            } else if (metadataSources.elementSet().size() > 1) {
                String metadataSourceCounts = "";
                for (MercurySample.MetadataSource source : metadataSources) {
                    metadataSourceCounts += String.format("%s: %d ", source, metadataSources.count(source));
                }
                throw new RuntimeException(
                        String.format("There are MercurySamples for %s that disagree on the sample data source: %s",
                                sampleName, metadataSourceCounts));
            } else {
                metadataSource = metadataSources.iterator().next();
            }
            results.put(sampleName, metadataSource);
        }
        return results;
    }

    public void populateSampleDataSources(Collection<ProductOrderSample> productOrderSamples) {
        Set<String> sampleIds = new HashSet<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            sampleIds.add(productOrderSample.getSampleKey());
        }

        final Map<String, MercurySample.MetadataSource> sampleDataSources =
                resolveSampleDataSources(sampleIds);

        for (ProductOrderSample productOrderSample : productOrderSamples) {
            productOrderSample.setMetadataSource(sampleDataSources.get(productOrderSample.getSampleKey()));
        }
    }
}
