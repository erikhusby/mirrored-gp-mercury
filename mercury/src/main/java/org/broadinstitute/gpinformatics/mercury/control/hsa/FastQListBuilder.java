package org.broadinstitute.gpinformatics.mercury.control.hsa;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStats;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FastQList;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Dependent
public class FastQListBuilder {

    private static final String HEADER = "RGID,RGSM,RGLB,Lane,Read1File,Read2File\n";
    private static final String LINE_FORMAT = "%s,%s,%s,%d,%s,%s";

    // Create Based on Flowcell Lane Index
    public boolean buildSingle(IlluminaSequencingRunChamber runChamber, SampleInstanceV2 sampleInstanceV2, MercurySample mercurySample,
                               String library, File outputFile) throws IOException {
        RunCartridge sampleCartridge = runChamber.getIlluminaSequencingRun().getSampleCartridge();
        StringBuilder sb = new StringBuilder(HEADER);
        File reports = new File(outputFile.getParentFile(), "Reports");
        File fastQListFile = new File(reports, "fastq_list.csv");
        Map<Integer, Map<String, FastQList>> mapLaneToSample = parseFastQFile(fastQListFile);

        MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
        SortedMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexes =
                molecularIndexingScheme.getIndexes();
        MolecularIndex p7 = indexes.get(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
        String indexScheme = p7.getSequence();
        if (indexes.containsKey(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5)) {
            MolecularIndex p5 = indexes.get(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5);
            indexScheme = indexScheme + "." + p5.getSequence();
        }
        String rgId = String.format("%s.%d.%s", sampleCartridge.getLabel(), runChamber.getLaneNumber(),
                indexScheme);

        File r1Fastq = new File(mapLaneToSample.get(
                runChamber.getLaneNumber()).get(mercurySample.getSampleKey()).getRead1File());
        File r2Fastq = new File(
                mapLaneToSample.get(runChamber.getLaneNumber()).get(mercurySample.getSampleKey()).getRead2File());

        String rgLine = String.format(LINE_FORMAT, rgId, mercurySample.getSampleKey(), library,
                runChamber.getLaneNumber(), r1Fastq.getPath(), r2Fastq.getPath());
        sb.append(rgLine);

        FileUtils.writeStringToFile(outputFile, sb.toString());

        return true;
    }

    public Map<Integer, Map<String, FastQList>> parseFastQFile(File fastQListFile) throws FileNotFoundException {
        Map<Integer, Map<String, FastQList>> mapLaneToSample = new HashMap<>();
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
            String sampleId = fastQList.getRgSm().split("_")[0];
            mapLaneToSample.get(fastQList.getLane()).put(sampleId, fastQList);
        }

        return mapLaneToSample;
    }
}
