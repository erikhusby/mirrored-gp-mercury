package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.analytics.AlignmentMetricsDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.columns.PickerVesselPlugin;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
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

@UrlBinding(TopOffActionBean.ACTION_BEAN_URL)
public class TopOffActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(TopOffActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/workflows/topoff.action";

    private static final String TOPOFF_PAGE = "/hsa/workflows/aggregation/top_offs.jsp";

    private List<String> selectedSamples;

    private List<HoldForTopoffDto> holdForTopoffDtos = new ArrayList<>();

    private List<TopoffDto> dtos = new ArrayList<>();

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private AlignmentMetricsDao alignmentMetricsDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabVesselDao labVesselDao;

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution view() {
        initData();
        return new ForwardResolution(TOPOFF_PAGE);
    }

    @HandlesEvent("downloadPickList")
    public Resolution downloadPickList() {
        List<TopoffDto> selectedDtos = getDtos().stream()
                .filter(dto -> selectedSamples.contains(dto.getPdoSample()))
                .collect(Collectors.toList());
        MessageCollection messageCollection = new MessageCollection();
        List<LabVessel> missingVessels = new ArrayList<>();
        Set<String> uniqueStorageLocations = new HashSet<>();
        String destinationContainer = "DEST";
        String header = "Source Rack Barcode,Source Well,Tube Barcode,Destination Rack Barcode,Destination Well";
        String rowFormat = "%s,%s,%s,%s,%s";
        List<String> xl20Rows = new ArrayList<>();
        xl20Rows.add(header);

        RackOfTubes.RackType rackType = RackOfTubes.RackType.Matrix96;
        int counter = 0;
        int rackCounter = 1;

        if (selectedDtos.isEmpty()) {
            messageCollection.addError("Please select at least one sample.");
        } else {
            List<String> libraries = selectedDtos.stream().map(TopoffDto::getLibrary).collect(Collectors.toList());
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(libraries);
            Map<String, MercurySample> mapIdToMercurySample =
                    mercurySampleDao.findMapIdToMercurySample(selectedSamples);
            for (Map.Entry<String, LabVessel> entry: mapBarcodeToVessel.entrySet()) {
                if (entry.getValue() == null) {
                    messageCollection.addError("Failed to find lab vessel " + entry.getKey());
                } else {
                    LabVessel labVessel = entry.getValue();
                    Triple<RackOfTubes, VesselPosition, String> triple =
                            PickerVesselPlugin.findStorageContainer(labVessel);
                    if (triple == null) {
                        messageCollection.addError("Failed to find in storage: " + labVessel.getLabel());
                        missingVessels.add(labVessel);
                        continue;
                    }
                    BarcodedTube barcodedTube = OrmUtil.proxySafeCast(labVessel, BarcodedTube.class);
                    if (barcodedTube.getTubeType().getAutomationName().contains("Matrix"));
                    RackOfTubes rack = triple.getLeft();
                    VesselPosition vesselPosition = triple.getMiddle();
                    uniqueStorageLocations.add(triple.getRight());
                    VesselPosition destinationPosition = rackType.getVesselGeometry().getVesselPositions()[counter];
                    xl20Rows.add(String.format(rowFormat, rack.getLabel(), vesselPosition.name(), labVessel.getLabel(),
                            destinationContainer, destinationPosition.name()));

                    counter++;
                    if (counter >= rackType.getVesselGeometry().getCapacity()) {
                        counter = 0;
                        rackCounter++;
                        destinationContainer = "DEST" + rackCounter;
                    }
                }
            }
        }

        messageCollection.addInfo("Unique Racks: " + StringUtils.join(uniqueStorageLocations, ","));

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
            topoffDto.setPdo(bucketEntry.getProductOrder().getBusinessKey());
            topoffDto.setLibrary(labVessel.getLabel());
            topoffDto.setVolume(labVessel.getVolume());
            topoffDto.setStorage(labVessel.getStorageLocationStringify());
            AlignmentMetric alignmentMetric = mapSampleToMetrics.get(mapVesselToSample.get(labVessel));

            BigDecimal xNeeded = bigDecimal.subtract(alignmentMetric.getAverageAlignmentCoverage());
            topoffDto.setxNeeded(xNeeded.floatValue());
            dtos.add(topoffDto);
        }
    }

    public List<String> getSelectedSamples() {
        return selectedSamples;
    }

    public void setSelectedSamples(List<String> selectedSamples) {
        this.selectedSamples = selectedSamples;
    }

    public List<TopoffDto> getDtos() {
        return dtos;
    }

    public void setDtos(List<TopoffDto> dtos) {
        this.dtos = dtos;
    }

    public class HoldForTopoffDto {
        private String pdoSample;
        private String pdo;
        private String library;
        private float xNeeded;
        private String index;

        public HoldForTopoffDto() {
        }

        public HoldForTopoffDto(String pdoSample, String pdo, String library, float xNeeded, String index) {
            this.pdoSample = pdoSample;
            this.pdo = pdo;
            this.library = library;
            this.xNeeded = xNeeded;
            this.index = index;
        }

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
