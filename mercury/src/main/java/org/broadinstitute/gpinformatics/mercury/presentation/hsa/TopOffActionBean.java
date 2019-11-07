package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.analytics.AlignmentMetricsDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationTriageActionBean.findProductOrderSample;

@UrlBinding(TopOffActionBean.ACTION_BEAN_URL)
public class TopOffActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(TopOffActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/workflows/topoff.action";

    private static final String TOPOFF_PAGE = "/hsa/workflows/aggregation/top_offs.jsp";


    private List<TopoffDto> dtos = new ArrayList<>();

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private AlignmentMetricsDao alignmentMetricsDao;

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution view() {
        initData();
        return new ForwardResolution(TOPOFF_PAGE);
    }

    // TODO Just assume its the pooling bucket
    private void initData() {
        Bucket poolingBucket = bucketDao.findByName("Pooling Bucket");
        List<BucketEntry> poolingBucketEntries = bucketEntryDao.findBucketEntries(poolingBucket,
                new ArrayList<>(), new ArrayList<>());

        Set<BucketEntry> bucketEntrySet = new HashSet<>();
        Set<String> sampleIds = new HashSet<>();
        Map<LabVessel, String> mapVesselToSample = new HashMap<>();
        for (BucketEntry bucketEntry: poolingBucketEntries) {
            LabVessel labVessel = bucketEntry.getLabVessel();
            for (SampleInstanceV2 sampleInstanceV2: labVessel.getSampleInstancesV2()) {
                ProductOrderSample productOrderSample = sampleInstanceV2.getProductOrderSampleForSingleBucket();
                if (productOrderSample != null) {
                    String sampleKey = productOrderSample.getSampleKey();
                    sampleIds.add(sampleKey);
                    mapVesselToSample.put(labVessel, sampleKey);
                    bucketEntrySet.add(bucketEntry);
                }
            }
        }

        Map<String, AlignmentMetric> mapSampleToMetrics = alignmentMetricsDao.findMapBySampleAlias(sampleIds);

        // TODO Get the actual coverage
        BigDecimal bigDecimal = new BigDecimal("40");
        for (BucketEntry bucketEntry: bucketEntrySet) {
            LabVessel labVessel = bucketEntry.getLabVessel();
            Set<String> molecularIndexes = new HashSet<>();

            for (SampleInstanceV2 sampleInstanceV2: labVessel.getSampleInstancesV2()) {
                MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
                if (molecularIndexingScheme != null) {
                    molecularIndexes.add(molecularIndexingScheme.getName());
                }
            }
            TopoffDto topoffDto = new TopoffDto();
            topoffDto.setIndex(StringUtils.join(molecularIndexes, ","));
            topoffDto.setPdoSample(mapVesselToSample.get(labVessel));
            topoffDto.setLibrary(labVessel.getLabel());
            topoffDto.setVolume(labVessel.getVolume());
            topoffDto.setStorage(labVessel.getStorageLocationStringify());
            AlignmentMetric alignmentMetric = mapSampleToMetrics.get(mapVesselToSample.get(labVessel));

            BigDecimal xNeeded = bigDecimal.subtract(alignmentMetric.getAverageAlignmentCoverage());
            topoffDto.setxNeeded(xNeeded.floatValue());
            dtos.add(topoffDto);
        }
    }

    public List<TopoffDto> getDtos() {
        return dtos;
    }

    public void setDtos(List<TopoffDto> dtos) {
        this.dtos = dtos;
    }

    public class TopoffDto {
        private String pdoSample;
        private String pdo;
        private String library;
        private float xNeeded;
        private String index;
        private String storage;
        private BigDecimal volume;

        public String getPdoSample() {
            return pdoSample;
        }

        public void setPdoSample(String pdoSample) {
            this.pdoSample = pdoSample;
        }

        public String getPdo() {
            return pdo;
        }

        public void setPdo(String pdo) {
            this.pdo = pdo;
        }

        public String getLibrary() {
            return library;
        }

        public void setLibrary(String library) {
            this.library = library;
        }

        public float getxNeeded() {
            return xNeeded;
        }

        public void setxNeeded(float xNeeded) {
            this.xNeeded = xNeeded;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getStorage() {
            return storage;
        }

        public void setStorage(String storage) {
            this.storage = storage;
        }

        public BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }
    }
}
