package org.broadinstitute.gpinformatics.mercury.control.hsa;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FastQList;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.ReadGroupUtil;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.enterprise.context.Dependent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

@Dependent
public class SampleSheetBuilder {

    public static final String FASTQ_NAME_FORMAT = "%s_S%d_%s_R%d_001.fastq.gz";

    public static List<SampleData> grabDataFromFile(File sampleSheet) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(sampleSheet));
        String line = null;
        while ((line = br.readLine()) != null)
        {
            if (line.contains("[Data]")) {
                break;
            }
        }

        Map<String, String> colToFieldMap = new HashMap<String, String>() {
            {
                put("Sample_ID ", "sampleId");
                put("Sample_Name", "sampleName");
                put("Lane", "lane");
                put("Index", "index");
                put("Index2", "index2");
            }
        };

        return CsvParser.parseCsvStreamToBeanByMapping(
                br, ',', SampleData.class, colToFieldMap, null, 0);
    }

    public SampleSheet makeSampleSheet(IlluminaSequencingRun illuminaRun) {
        return makeSampleSheet(illuminaRun, null, Collections.emptySet());
    }

    /**
     * For a sequencing run filter only the lanes and samples we care about if any.
     */
    public SampleSheet makeSampleSheet(IlluminaSequencingRun illuminaRun, Set<VesselPosition> lanes,
                                       Set<String> filterSampleIds) {
        RunCartridge flowcell = illuminaRun.getSampleCartridge();
        IlluminaFlowcell illuminaFlowcell = OrmUtil.proxySafeCast(flowcell, IlluminaFlowcell.class);
        boolean isReverseComplement = illuminaFlowcell.getFlowcellType().isReverseComplement();

        Data data = new Data();
        Header header = null;
        for (VesselPosition vesselPosition: flowcell.getVesselGeometry().getVesselPositions()) {
            if (lanes != null && !lanes.contains(vesselPosition)) {
                continue;
            }
            int laneNum = Integer.parseInt(vesselPosition.name().substring(vesselPosition.name().length() - 1));
            for (SampleInstanceV2 laneSampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(
                    vesselPosition)) {

                ProductOrderSample productOrderSample = laneSampleInstance.getSingleProductOrderSample();
                MercurySample mercurySample = findLaneMercurySample(laneSampleInstance);

                if (!filterSampleIds.isEmpty() && !filterSampleIds.contains(mercurySample.getSampleKey())) {
                    continue;
                }

                data.addSample(laneSampleInstance, mercurySample, laneNum, isReverseComplement, flowcell.getLabel());
                if (productOrderSample != null) {
                    header = createHeader(laneSampleInstance);
                }
            }
        }

        return new SampleSheet(header, data);
    }

    public static Pair<MercurySample, SampleInstanceV2> findSampleInFlowcellLane(RunCartridge flowcell, VesselPosition lane, MercurySample sample ) {
        for (SampleInstanceV2 sampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(lane)) {
            Pair<MercurySample, SampleInstanceV2> exportedSample = findExportedSample(sample, sampleInstance);
            if (exportedSample != null) {
                return exportedSample;
            } else {
                ProductOrderSample productOrderSample = sampleInstance.getSingleProductOrderSample();
                if (productOrderSample != null) {
                    if (productOrderSample.getMercurySample().equals(sample)) {
                        return Pair.of(productOrderSample.getMercurySample(), sampleInstance);
                    }
                } else if (sampleInstance.getRootOrEarliestMercurySample().equals(sample)) {
                    return Pair.of(sampleInstance.getRootOrEarliestMercurySample(), sampleInstance);
                }
            }
        }

        return null;
    }

    public static Pair<MercurySample, SampleInstanceV2> findExportedSample(MercurySample sample, SampleInstanceV2 sampleInstance) {
        ProductOrderSample productOrderSample = sampleInstance.getSingleProductOrderSample();
        if (sampleInstance.getRootOrEarliestMercurySample().equals(sample) || productOrderSample.getMercurySample().equals(sample)) {
            LabBatchStartingVessel importLbsv =
                    sampleInstance.getSingleBatchVessel(LabBatch.LabBatchType.SAMPLES_IMPORT);
            if (importLbsv != null) {
                Collection<MercurySample> mercurySamples = importLbsv.getLabVessel().getMercurySamples();
                if (!mercurySamples.isEmpty()) {
                    MercurySample mercurySample = mercurySamples.iterator().next();
                    return Pair.of(mercurySample, sampleInstance);
                }
            }
        }

        return null;
    }

    public static MercurySample findLaneMercurySample(SampleInstanceV2 laneSampleInstance) {
        ProductOrderSample productOrderSample = laneSampleInstance.getSingleProductOrderSample();
        MercurySample mercurySample;
        if (productOrderSample != null) {
            mercurySample = productOrderSample.getMercurySample();
        } else {
            // Controls won't have a ProductOrderSample, so use root sample ID.
            mercurySample = laneSampleInstance.getRootOrEarliestMercurySample();
        }

        LabBatchStartingVessel importLbsv = laneSampleInstance.getSingleBatchVessel(LabBatch.LabBatchType.SAMPLES_IMPORT);
        if (importLbsv != null) {
            Collection<MercurySample> mercurySamples = importLbsv.getLabVessel().getMercurySamples();
            if (!mercurySamples.isEmpty()) {
                mercurySample = mercurySamples.iterator().next();
            }
        }
        return mercurySample;
    }

    public SampleSheet makeSampleSheet(IlluminaSequencingRun illuminaRun, VesselPosition vesselPosition, int laneNum) {
        RunCartridge flowcell = illuminaRun.getSampleCartridge();
        IlluminaFlowcell illuminaFlowcell = OrmUtil.proxySafeCast(flowcell, IlluminaFlowcell.class);
        boolean isReverseComplement = illuminaFlowcell.getFlowcellType().isReverseComplement();

        Data data = new Data();
        Header header = null;
        for (SampleInstanceV2 laneSampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(
                vesselPosition)) {
            ProductOrderSample productOrderSample = laneSampleInstance.getSingleProductOrderSample();
            MercurySample mercurySample;
            if (productOrderSample != null) {
                mercurySample = productOrderSample.getMercurySample();
            } else {
                // Controls won't have a ProductOrderSample, so use root sample ID.
                mercurySample = laneSampleInstance.getRootOrEarliestMercurySample();
            }

            data.addSample(laneSampleInstance, mercurySample, laneNum, isReverseComplement, flowcell.getLabel());
            if (productOrderSample != null) {
                header = createHeader(laneSampleInstance);
            }
        }

        return new SampleSheet(header, data);
    }

    private Header createHeader(SampleInstanceV2 laneSampleInstance) {
        ProductOrderSample singleProductOrderSample = laneSampleInstance.getSingleProductOrderSample();
        return new Header().IEMFileVersion(4).
                investigatorName(singleProductOrderSample.getProductOrder().getBusinessKey()).
                experimentName(singleProductOrderSample.getProductOrder().getBusinessKey()).
                date(new Date()).
                workflow("GenerateFASTQ").
                application("NovaSeq FASTQ Only").
                assay("Nextera XT").
                description("").
                chemistry("Amplicon").end();

    }

    public class SampleSheet {
        private Header header;
        private Data data;

        public SampleSheet(Header header, Data data) {
            this.header = header;
            this.data = data;
        }

        public Header getHeader() {
            return header;
        }

        public Data getData() {
            return data;
        }

        public String toCsv() {
            return header.toString() + data.toString();
        }
    }

    public abstract class Section {
        private static final int NUM_COLUMNS = 4;
        private final StringBuilder commandBuilder;

        protected Section(String sectionName) {
            this.commandBuilder = new StringBuilder();
            appendLine(sectionName);
        }

        void appendLine(String... cols) {
            Arrays.stream(cols).forEach(c -> commandBuilder.append(c).append(","));

            for (int i = 0; i < NUM_COLUMNS - cols.length; i++) {
                commandBuilder.append(",");
            }
            commandBuilder.append(System.lineSeparator());
        }

        public String toString() {
            return this.commandBuilder.toString();
        }
    }

    public class Data extends Section {

        private Boolean dualIndex;
        private Map<String, SampleData> mapSampleNameToData = new HashMap<>();
        private Map<String, MercurySample> mapSampleToMercurySample = new HashMap<>();
        private Map<String, SampleInstanceV2> mapSampleToInstance = new HashMap<>();
        private Set<MercurySample> mercurySamples = new HashSet<>();

        public Data() {
            super("[Data]");
        }

        public void createSubHeader() {
            if (dualIndex) {
                appendLine("Sample_ID", "Sample_Name", "Lane", "Index", "Index2");
            } else {
                appendLine(
                        "Sample_ID", "Sample_Name", "Lane", "Index");
            }
        }

        public void addSample(SampleInstanceV2 sampleInstanceV2, MercurySample mercurySample, int lane,
                              boolean isReverseComplement, String flowcell) {
            mercurySamples.add(mercurySample);
            String pdoSampleName = mercurySample.getSampleKey();
            MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
            SortedMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexes =
                    molecularIndexingScheme.getIndexes();

            if (dualIndex == null) {
                dualIndex = indexes.size() == 2;
                createSubHeader();
            }

            SampleData data = null;

            String rgSmId = ReadGroupUtil.createSampleSheetId(flowcell, lane, pdoSampleName);
            MolecularIndex p7 = indexes.get(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
            if (dualIndex) {
                MolecularIndex p5 = indexes.get(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5);
                String p5Seq = p5.getSequence();
                if (isReverseComplement) {
                    p5Seq = DNAUtils.reverseComplement(p5Seq);
                }
                data = new SampleData(rgSmId, pdoSampleName, lane, p7.getSequence(), p5Seq);
                appendLine(rgSmId, pdoSampleName, String.valueOf(lane), p7.getSequence(), p5Seq);
            } else {
                data = new SampleData(rgSmId, pdoSampleName, lane, p7.getSequence(), null);
                appendLine(rgSmId, pdoSampleName, String.valueOf(lane), p7.getSequence());
            }
            mapSampleNameToData.put(pdoSampleName, data);
            mapSampleToMercurySample.put(pdoSampleName, mercurySample);
            mapSampleToInstance.put(pdoSampleName, sampleInstanceV2);
        }

        public Map<String, SampleData> getMapSampleNameToData() {
            return mapSampleNameToData;
        }

        public Set<MercurySample> getMercurySamples() {
            return mercurySamples;
        }

        public Map<String, MercurySample> getMapSampleToMercurySample() {
            return mapSampleToMercurySample;
        }

        public Map<String, SampleInstanceV2> getMapSampleToInstance() {
            return mapSampleToInstance;
        }
    }

    public static class SampleData {
        private String sampleId;
        private String sampleName;
        private int lane;
        private String index;
        private String index2;

        // For CSV Parser
        public SampleData() {
        }

        public SampleData(String sampleId, String sampleName, int lane,
                          String index, String index2) {
            this.sampleId = sampleId;
            this.sampleName = sampleName;
            this.lane = lane;
            this.index = index;
            this.index2 = index2;
        }

        public String getSampleId() {
            return sampleId;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getSampleName() {
            return sampleName;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
        }

        public int getLane() {
            return lane;
        }

        public void setLane(int lane) {
            this.lane = lane;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getIndex2() {
            return index2;
        }

        public void setIndex2(String index2) {
            this.index2 = index2;
        }
    }

    public class Header extends Section {

        public Header() {
            super("[Header]");
        }

        public Header IEMFileVersion(int fileVersion) {
            appendLine("IEMFileVersion", String.valueOf(fileVersion));
            return this;
        }

        public Header investigatorName(String investigator) {
            appendLine("Investigator Name", investigator);
            return this;
        }

        public Header experimentName(String experiment) {
            appendLine("Experiment Name", experiment);
            return this;
        }

        public Header date(Date date) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            appendLine("Date", sdf.format(date));
            return this;
        }

        public Header workflow(String workflow) {
            appendLine("Workflow", workflow);
            return this;
        }

        public Header application(String application) {
            appendLine("Application", application);
            return this;
        }

        public Header assay(String assay) {
            appendLine("Assay", assay);
            return this;
        }

        public Header description(String description) {
            appendLine("Description", description);
            return this;
        }

        public Header chemistry(String chemistry) {
            appendLine("Chemistry", chemistry);
            return this;
        }

        public Header end() {
            appendLine();
            return this;
        }
    }
}
