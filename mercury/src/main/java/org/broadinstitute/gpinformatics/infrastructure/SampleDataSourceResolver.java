package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Business logic for determining the sample data source for samples in mercury. Historically, the only place that
 * Mercury would look for sample data was BSP. Later, there came a need to store sample data directly in Mercury. The
 * primary determination is based on {@link MercurySample#metadataSource}. When there is no MercurySample for a sample
 * ID being queried (e.g. because it has been entered in a PDO but has not been introduced to Mercury LIMS), BSP is
 * assumed to preserve the historical behavior.
 * <p>
 * Since SampleDataSourceResolver needs to consult MercurySample, a database query is required when only the sample IDs
 * are provided. Callers that themselves need the MercurySample instances should favor calling
 * {@link SampleDataSourceResolver#resolveSampleDataSources(Collection, Map)}, using the result of
 * {@link MercurySampleDao#findMapIdToMercurySample(Collection)} for the second argument to avoid a redundant database
 * query.
 */
@Dependent
public class SampleDataSourceResolver implements Serializable{

    private final MercurySampleDao mercurySampleDao;

    @Inject
    public SampleDataSourceResolver(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    /**
     * Resolve the sample data source for a collection of sample IDs.
     * <p>
     * Sample IDs can be BSP SM-IDs or any other ID that matches a value stored in {@link MercurySample#sampleKey}.
     * Sample IDs in other formats, specifically LSIDs, will be indicated as BSP samples for historical reasons but
     * will NOT successfully retrieve sample data from BSP when queried from {@link SampleDataFetcher} or
     * {@link BSPSampleDataFetcher}.
     * <p>
     *
     * @param sampleNames    the sample IDs for which to resolve the sample data source
     * @return a map of sample ID to sample data source
     */
    public Map<String, MercurySample.MetadataSource> resolve(Collection<String> sampleNames) {
        Map<String, MercurySample> allMercurySamples =
                mercurySampleDao.findMapIdToMercurySample(sampleNames);
        return resolve(sampleNames, allMercurySamples);
    }

    @DaoFree
    public Map<String, MercurySample.MetadataSource> resolve(Collection<String> sampleNames,
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
                // Assume that BSP is the owner of samples data for sample IDs with no MercurySample.
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

    public void populateSampleDataSources(ProductOrder productOrder) {
        populateSampleDataSources(productOrder.getSamples());
    }

    public void populateSampleDataSources(Collection<ProductOrderSample> productOrderSamples) {
        Set<String> sampleIds = new HashSet<>(ProductOrderSample.getSampleNames(productOrderSamples));

        Map<String, MercurySample.MetadataSource> sampleDataSources =
                ProductOrderSample.getMetadataSourcesForBoundProductOrderSamples(productOrderSamples);
        sampleIds.removeAll(sampleDataSources.keySet());
        sampleDataSources.putAll(resolve(sampleIds));

        for (ProductOrderSample productOrderSample : productOrderSamples) {
            productOrderSample.setMetadataSource(sampleDataSources.get(productOrderSample.getSampleKey()));
        }
    }
}
