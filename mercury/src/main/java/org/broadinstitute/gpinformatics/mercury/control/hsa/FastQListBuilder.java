package org.broadinstitute.gpinformatics.mercury.control.hsa;

import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FastQList;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.enterprise.context.Dependent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class FastQListBuilder {

    private static final String HEADER = "RGID,RGSM,RGPU,RGPL,RGLB,RGCN,Lane,Read1File,Read2File\n";
    private static final String LINE_FORMAT = "%s,%s,%s,%s,%s,%s,%d,%s,%s";

    // Create Based on Flowcell Lane Index
    public void buildSingle(int lane, String rgId, String rgPu, MercurySample mercurySample, MercurySample expectedSample,
                               String library, File outputFile, File fastQListFile) throws IOException {
        StringBuilder sb = new StringBuilder(HEADER);

        File reports = outputFile.getParentFile();

        Map<Integer, Map<String, List<FastQList>>> mapLaneToSample = parseFastQFile(fastQListFile);

        Map<String, List<FastQList>> sampleToFastqs = mapLaneToSample.get(lane);
        List<FastQList> fastQLists = sampleToFastqs.get(expectedSample.getSampleKey());
        if (fastQLists == null) { // Try pdo sample
            fastQLists = sampleToFastqs.get(mercurySample.getSampleKey());
        }
        for (FastQList fastQLine: fastQLists) { // TODO JW This is null
            File r1Fastq = new File(fastQLine.getRead1File());
            File r2Fastq = new File(fastQLine.getRead2File());

            String rgLine = String.format(LINE_FORMAT, rgId, mercurySample.getSampleKey(), rgPu, "ILLUMINA", library,
                    "BI", lane, r1Fastq.getPath(), r2Fastq.getPath());
            sb.append(rgLine);
        }

        if (!reports.exists()) {
            reports.mkdir();
        }

        FileUtils.writeStringToFile(outputFile, sb.toString());
    }

    public void buildAggregation(Set<FastQList> fastQLists, MercurySample mercurySample, File outputFile) throws IOException {
        FileWriter writer = new FileWriter(outputFile);
        writer.write(HEADER);
        for (FastQList fastQList: fastQLists) {
            writer.write(String.format(LINE_FORMAT, fastQList.getRgId(), mercurySample.getSampleKey(),
                    fastQList.getRgPu(), fastQList.getRgPl(), fastQList.getRgLb(), "BI", fastQList.getLane(),
                    fastQList.getRead1File(), fastQList.getRead2File()));
            writer.write("\n");
        }
        writer.close();
    }

    public Map<Integer, Map<String, List<FastQList>>> parseFastQFile(File fastQListFile) throws FileNotFoundException {
        Map<Integer, Map<String, List<FastQList>>> mapLaneToSample = new HashMap<>();
        CsvToBean<FastQList> builder = new CsvToBeanBuilder<FastQList>(
                new BufferedReader(new FileReader(fastQListFile))).
                withSeparator(',').
                withType(FastQList.class).
                build();
        List<FastQList> fastQLists = builder.parse();
        for (FastQList fastQList: fastQLists) {
            if (!mapLaneToSample.containsKey(fastQList.getLane())) {
                mapLaneToSample.put(fastQList.getLane(), new HashMap<>());
            }
            String sampleId = fastQList.getRgSm().split("_")[2];
            if (!mapLaneToSample.get(fastQList.getLane()).containsKey(sampleId)) {
                mapLaneToSample.get(fastQList.getLane()).put(sampleId, new ArrayList<>());
            }
            mapLaneToSample.get(fastQList.getLane()).get(sampleId).add(fastQList);
        }

        return mapLaneToSample;
    }

    public List<FastQList> parseFastQFileList(File fastQListFile) throws FileNotFoundException {
        CsvToBean<FastQList> builder = new CsvToBeanBuilder<FastQList>(
                new BufferedReader(new FileReader(fastQListFile))).
                withSeparator(',').
                withType(FastQList.class).
                build();
        return builder.parse();
    }

    public Set<FastQList> findFastQ(File fastQListFile, int lane, String sampleKey) throws FileNotFoundException {
        CsvToBean<FastQList> builder = new CsvToBeanBuilder<FastQList>(
                new BufferedReader(new FileReader(fastQListFile))).
                withSeparator(',').
                withType(FastQList.class).
                build();
        List<FastQList> fastQLists = builder.parse();

        return fastQLists.stream()
                .filter(fastQList -> fastQList.getLane() == lane && fastQList.getRgSm().contains(sampleKey))
                .collect(Collectors.toSet());
    }
}
