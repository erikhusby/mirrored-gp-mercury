package org.broadinstitute.gpinformatics.mercury.control.hsa;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.parsers.csv.CsvParser;
import org.broadinstitute.gpinformatics.mercury.control.vessel.DBSPuncherFileParser;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.enterprise.context.Dependent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

@Dependent
public class SampleSheetBuilder {

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
        RunCartridge flowcell = illuminaRun.getSampleCartridge();

        Data data = new Data();
        Header header = null;
        for (VesselPosition vesselPosition: flowcell.getVesselGeometry().getVesselPositions()) {
            int laneNum = Integer.parseInt(vesselPosition.name().substring(vesselPosition.name().length() - 1));
            for (SampleInstanceV2 laneSampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(
                    vesselPosition)) {
                data.addSample(laneSampleInstance, laneNum);
                ProductOrderSample productOrderSample = laneSampleInstance.getSingleProductOrderSample();
                if (productOrderSample != null) {
                    header = createHeader(laneSampleInstance);
                }
            }
        }

        return new SampleSheet(header, data);
    }

    public SampleSheet makeSampleSheet(IlluminaSequencingRun illuminaRun, VesselPosition vesselPosition, int laneNum) {
        RunCartridge flowcell = illuminaRun.getSampleCartridge();

        Data data = new Data();
        Header header = null;
        for (SampleInstanceV2 laneSampleInstance : flowcell.getContainerRole().getSampleInstancesAtPositionV2(
                vesselPosition)) {
            data.addSample(laneSampleInstance, laneNum);
            ProductOrderSample productOrderSample = laneSampleInstance.getSingleProductOrderSample();
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

        public void addSample(SampleInstanceV2 sampleInstanceV2, int lane) {
            String pdoSampleName;
            ProductOrderSample productOrderSample = sampleInstanceV2.getSingleProductOrderSample();
            if (productOrderSample != null) {
                pdoSampleName = productOrderSample.getSampleKey();
            } else {
                // Controls won't have a ProductOrderSample, so use root sample ID.
                pdoSampleName = sampleInstanceV2.getMercuryRootSampleName();
            }

            MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
            SortedMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexes =
                    molecularIndexingScheme.getIndexes();

            if (dualIndex == null) {
                dualIndex = indexes.size() == 2;
                createSubHeader();
            }

            Iterator<MolecularIndex> iterator = indexes.values().iterator();
            String index1 = iterator.next().getSequence();
            SampleData data = null;
            if (dualIndex) {
                String index2 = iterator.next().getSequence();
                data = new SampleData(pdoSampleName, pdoSampleName, lane, index1, index2);
                appendLine(pdoSampleName, pdoSampleName, String.valueOf(lane), index1, index2);
            } else {
                data = new SampleData(pdoSampleName, pdoSampleName, lane, index1, null);
                appendLine(pdoSampleName, pdoSampleName, String.valueOf(lane), index1);
            }
            mapSampleNameToData.put(pdoSampleName, data);
        }

        public Map<String, SampleData> getMapSampleNameToData() {
            return mapSampleNameToData;
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

        public SampleData(String sampleId, String sampleName, int lane, String index, String index2) {
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
