package org.broadinstitute.gpinformatics.infrastructure;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleDataFetcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Dependent
public class SampleDataFetcher implements Serializable {

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private MercurySampleDataFetcher mercurySampleDataFetcher;

    @Inject
    private MercurySampleDao mercurySampleDao;

    public SampleDataFetcher() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspSampleDataFetcher fetcher for fetching sample data from BSP
     */
    public SampleDataFetcher(@Nonnull BSPSampleDataFetcher bspSampleDataFetcher) {
        this.bspSampleDataFetcher = bspSampleDataFetcher;
    }

    public SampleDataFetcher(@Nonnull MercurySampleDao mercurySampleDao,
            @Nonnull BSPSampleDataFetcher bspSampleDataFetcher,
            @Nonnull MercurySampleDataFetcher mercurySampleDataFetcher) {
        this.mercurySampleDao = mercurySampleDao;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
        this.mercurySampleDataFetcher = mercurySampleDataFetcher;
    }

    /**
     * Database Free testing only
     */
    public SampleDataFetcher(@Nonnull BSPSampleSearchService service) {
        this(service, null);
    }

    /**
     * Database Free testing only
     */
    public SampleDataFetcher(@Nonnull BSPSampleSearchService service, @Nullable BSPConfig bspConfig) {
        this(new BSPSampleDataFetcherImpl(service, bspConfig));
    }

    /**
     * Fetch the data for the given sample.
     *
     * @param sampleName The sample name.
     *
     * @return The sample DTO that was fetched.
     */
    public SampleData fetchSampleData(String sampleName) {
        return fetchSampleData(Collections.singleton(sampleName)).get(sampleName);
    }

    /**
     * Fetch the data for multiple samples, defaulting BSP sample search to PDO Search Columns.
     *
     * @param sampleNames The sample names, which should be short barcodes such as SM-4FHTK
     *
     * @return Mapping of sample id to its sample data
     */
    public Map<String, SampleData> fetchSampleData(@Nonnull Collection<String> sampleNames) {
        return fetchSampleData(sampleNames, BSPSampleSearchColumn.PDO_SEARCH_COLUMNS);
    }

    public Map<String, SampleData> fetchSampleData(@Nonnull Collection<String> sampleNames,
                                                   BSPSampleSearchColumn[] bspSampleSearchColumns) {
        Pair<Set<MercurySample>, Set<String>> pair = partitionSamples(sampleNames);
        return fetchSampleData(pair.getLeft(), pair.getRight(), Collections.emptyMap(), bspSampleSearchColumns);
    }

    private Map<String, SampleData> fetchSampleData(Collection<MercurySample> mercuryFetches, Set<String> bspFetches,
            Map<String, ProductOrderSample> quantOverrides, BSPSampleSearchColumn[] bspSampleSearchColumns) {

        // Looks for metadata inheritance roots. Mercury samples that have Mercury metadata source and have
        // a metadata ROOT_SAMPLE (not the chain of custody root) in Mercury or BSP will inherit the root's
        // Collaborator Sample Id, Collaborator Participant Id, Sex, and Organism.
        Map<String, String> mercurySampleToInheritanceRoot = new HashMap<>();
        for (MercurySample sample : mercuryFetches) {
            if (sample.getMetadata() != null) {
                sample.getMetadata().stream().
                        filter(metadata -> metadata.getKey() == Metadata.Key.ROOT_SAMPLE).
                        map(Metadata::getValue).
                        filter(value -> !sample.getSampleKey().equals(value)).
                        findFirst().
                        ifPresent(value -> mercurySampleToInheritanceRoot.put(sample.getSampleKey(), value));
            }
        }
        // Fetches the BSP sample metadata.
        Map<String, BspSampleData> bspSampleData = bspSampleDataFetcher.fetchSampleData(
                Stream.concat(bspFetches.stream(), mercurySampleToInheritanceRoot.values().stream()).
                        filter(BSPUtil::isInBspFormatOrBareId).
                        collect(Collectors.toSet()),
                bspSampleSearchColumns);
        // Overrides the BSP quant values if the product indicates it.
        bspSampleData.keySet().stream().
                filter(quantOverrides::containsKey).
                forEach(sampleId ->
                        bspSampleData.get(sampleId).overrideWithMercuryQuants(quantOverrides.get(sampleId)));

        // The remaining inheritance roots that may be in Mercury.
        Set<MercurySample> remainingInheritanceRoots = (Set<MercurySample>)mercurySampleDao.findMapIdToMercurySample(
                CollectionUtils.subtract(mercurySampleToInheritanceRoot.values(), bspSampleData.keySet())).
                values().stream().
                filter(mercurySample -> mercurySample != null).
                collect(Collectors.toSet());

        // Collects sampleData of the Mercury source samples and any Mercury roots.
        Map<String, MercurySampleData> mercurySampleData = mercurySampleDataFetcher.fetchSampleData(
                CollectionUtils.union(mercuryFetches, remainingInheritanceRoots));

        // Merges in metadata for samples inheriting from their root sample.
        mercurySampleData.entrySet().stream().
                forEach(mapEntry -> {
                    String inheritanceRoot = mercurySampleToInheritanceRoot.get(mapEntry.getKey());
                    MercurySampleData sampleData = mapEntry.getValue();
                    if (bspSampleData.get(inheritanceRoot) != null) {
                        sampleData.mergeInheritedMetadata(bspSampleData.get(inheritanceRoot));
                    } else if (mercurySampleData.get(inheritanceRoot) != null) {
                        sampleData.mergeInheritedMetadata(mercurySampleData.get(inheritanceRoot));
                    }
                });

        // Returns a map of the requested Mercury and BSP sample names with any sampleData found (null if not found).
        Map<String, SampleData> sampleData = new HashMap<>();
        mercuryFetches.stream().
                map(MercurySample::getSampleKey).
                forEach(sampleName -> sampleData.put(sampleName, mercurySampleData.get(sampleName)));
        bspFetches.stream().
                forEach(bspName -> sampleData.put(bspName, bspSampleData.get(bspName)));
        return sampleData;
    }

    public void fetchFFPEDerived(@Nonnull Collection<SampleData> sampleDataCollection) {
        /*
         * FFPE is (for now) irrelevant for samples whose sample data is in Mercury. Therefore, only fetch FFPE-derived
         * for BspSampleData.
         */
        Collection<BspSampleData> bspSampleDataCollection = new ArrayList<>();
        for (SampleData sampleData : sampleDataCollection) {
            if (sampleData.getMetadataSource() == MercurySample.MetadataSource.BSP){
                bspSampleDataCollection.add((BspSampleData) sampleData);
            }
        }
        bspSampleDataFetcher.fetchFFPEDerived(bspSampleDataCollection);
    }

    /**
     * Given an aliquot ID, return its stock sample ID.
     */
    public String getStockIdForAliquotId(@Nonnull String aliquotId) {
        Map<String, String> stockIdByAliquotId = getStockIdByAliquotId(Collections.singleton(aliquotId));
        if (!stockIdByAliquotId.isEmpty()) {
            return stockIdByAliquotId.get(aliquotId);
        }
        return null;
    }

    /**
     * Given a list of aliquot IDs, return a map of aliquot IDs to stock IDs.
     */
    public Map<String, String> getStockIdByAliquotId(Collection<String> aliquotIds) {
        Pair<Set<MercurySample>, Set<String>> pair = partitionSamples(aliquotIds);
        Map<String, String> map = new HashMap<>(mercurySampleDataFetcher.getStockIdByAliquotId(pair.getLeft()));
        map.putAll(bspSampleDataFetcher.getStockIdByAliquotId(pair.getRight()));
        return map;
    }

    /** Partitions the sampleNames into MercurySample lookups and BSP sample lookups. */
    private Pair<Set<MercurySample>, Set<String>> partitionSamples (Collection<String> sampleNames) {
        Set<String> bsp = new HashSet<>();
        Set<MercurySample> mercury = new HashSet<>();
        Map<String, MercurySample> sampleMap = mercurySampleDao.findMapIdToMercurySample(sampleNames);
        sampleNames.stream().forEach(sampleName -> {
            MercurySample mercurySample = sampleMap.get(sampleName);
            if (mercurySample == null || mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                bsp.add(sampleName);
            } else {
                mercury.add(mercurySample);
            }
        });
        return Pair.of(mercury, bsp);
    }

    /**
     * Returns a map of barcode to SampleDetails object for each input barcode, which can be a manufacturer barcode
     * or SM-id barcode.
     */
    public Map<String, GetSampleDetails.SampleInfo> fetchSampleDetailsByBarcode(@Nonnull Collection<String> barcodes) {
        return bspSampleDataFetcher.fetchSampleDetailsByBarcode(barcodes);
    }

    /**
     * Fetch the SampleData for multiple product order samples. For cases where only a few pieces of data are needed for
     * each sample, a list of result properties can be specified as a hint to help optimize the data fetch. This is
     * helpful for performance when fetching sample data from BSP.
     *
     * Implementation note: The result properties are currently of type {@link BSPSampleSearchColumn}, which leaks some
     * details of the BSP fetch through the SampleDataFetcher abstraction. This could be made more general to clean up
     * the API. This should also involve an analysis of the overlap between BSPSampleSearchColumn and
     * {@link Metadata.Key}.
     *
     * @param samples                collection of product order sample for which sample data is needed
     * @param bspSampleSearchColumns hint for which columns to return data for, if performance is a factor
     *
     * @return Mapping of either pdoSample name or mercurySample name to corresponding sample data.
     */
    public Map<String, SampleData> fetchSampleDataForSamples(Collection<? extends AbstractSample> samples,
                                                             BSPSampleSearchColumn... bspSampleSearchColumns) {
        Map<String, SampleData> sampleData = new HashMap<>();
        Collection<MercurySample> mercurySamplesWithMercurySource = new ArrayList<>();
        Set<String> bspSourceSampleNames = new HashSet<>(samples.size());
        Map<String, ProductOrderSample> needsQuantOverride = new HashMap<>();

        // Finds the ProductOrderSamples in the input AbstractSamples that don't have a linked
        // MercurySample and links them to a MercurySample having the PDO sample name.
        Map<String, MercurySample> mercurySampleMap = mercurySampleDao.findMapIdToMercurySample(
                samples.stream().
                        filter(sample -> !sample.isHasBspSampleDataBeenInitialized() &&
                                OrmUtil.proxySafeIsInstance(sample, ProductOrderSample.class)).
                        map(sample -> OrmUtil.proxySafeCast(sample, ProductOrderSample.class)).
                        filter(pdoSample -> pdoSample.getMercurySample() == null).
                        map(ProductOrderSample::getName).
                        collect(Collectors.toList()));
        if (!mercurySampleMap.isEmpty()) {
            samples.stream().
                    filter(sample -> mercurySampleMap.containsKey(sample.getSampleKey()) &&
                            OrmUtil.proxySafeIsInstance(sample, ProductOrderSample.class)).
                    forEach(sample ->
                            OrmUtil.proxySafeCast(sample, ProductOrderSample.class).
                                    setMercurySample(mercurySampleMap.get(sample.getSampleKey())));
        }

        for (AbstractSample sample : samples) {
            if (sample.isHasBspSampleDataBeenInitialized()) {
                // sample.getSampleKey() can be a pdo sample name that is a barcode.
                sampleData.put(sample.getSampleKey(), sample.getSampleData());
            } else {
                MercurySample mercurySample;
                ProductOrderSample productOrderSample;
                if (OrmUtil.proxySafeIsInstance(sample, MercurySample.class)) {
                    mercurySample = OrmUtil.proxySafeCast(sample, MercurySample.class);
                    // Uses the most recent pdo sample which should be more correct in the case of
                    // a sample being put into a second PDO that has a different product. But it's best
                    // to just pass this method pdo samples that have a linked mercurySample.
                    productOrderSample = mercurySample.getProductOrderSamples().stream().
                            sorted(Comparator.comparingLong(ProductOrderSample::getProductOrderSampleId).
                                    reversed()).
                            findFirst().orElse(null);
                } else {
                    productOrderSample = OrmUtil.proxySafeCast(sample, ProductOrderSample.class);
                    mercurySample = productOrderSample.getMercurySample();
                }
                // Prefers the mercurySample name since a BSP sample can be put in a PDO using its tube barcode.
                String sampleName = mercurySample == null ? productOrderSample.getName() : mercurySample.getSampleKey();
                if (mercurySample != null &&
                        mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                    mercurySamplesWithMercurySource.add(mercurySample);
                } else {
                    bspSourceSampleNames.add(sampleName);
                }
                // To improve performance, check for Mercury quants only if the product indicates that they're there.
                if (productOrderSample != null && productOrderSample.getProductOrder() != null) {
                    Product product = productOrderSample.getProductOrder().getProduct();
                    if (product != null && product.getExpectInitialQuantInMercury() &&
                            quantColumnRequested(bspSampleSearchColumns)) {
                        // Puts both mercury and pdo sample name.
                        needsQuantOverride.put(sampleName, productOrderSample);
                        needsQuantOverride.put(productOrderSample.getSampleKey(), productOrderSample);
                    }
                }
            }
        }
        sampleData.putAll(fetchSampleData(mercurySamplesWithMercurySource, bspSourceSampleNames,
                needsQuantOverride, bspSampleSearchColumns));
        return sampleData;
    }

    private boolean quantColumnRequested(BSPSampleSearchColumn[] bspSampleSearchColumns) {
        for (BSPSampleSearchColumn bspSampleSearchColumn : bspSampleSearchColumns) {
            if (BSPSampleSearchColumn.isQuantColumn(bspSampleSearchColumn)) {
                return true;
            }
        }
        return false;
    }
}
