package org.broadinstitute.gpinformatics.mercury.control.hsa;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.SortedMap;

public class FastQListBuilder {

    private static final String HEADER = "RGID,RGSM,RGLB,Lane,Read1File,Read2File\n";
    private static final String LINE_FORMAT = "%s,%s,%s,%d,%s,%s";

    @Inject
    private DragenConfig dragenConfig;

    // Create Based on Flowcell Lane Index
    public boolean buildSingle(IlluminaSequencingRunChamber runChamber, MercurySample mercurySample,
                               File outputFile) throws IOException {
        RunCartridge sampleCartridge = runChamber.getIlluminaSequencingRun().getSampleCartridge();
        VesselPosition vesselPosition = VesselPosition.getByName("LANE" + runChamber.getLaneNumber());
        IlluminaFlowcell illuminaFlowcell = OrmUtil.proxySafeCast(sampleCartridge, IlluminaFlowcell.class);
        boolean foundSample = false;
        StringBuilder sb = new StringBuilder(HEADER);
        for (SampleInstanceV2 sampleInstanceV2: illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition)) {
            if (sampleInstanceV2.getRootOrEarliestMercurySampleName().equals(mercurySample.getSampleKey())) {
                foundSample = true;
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

                DragenFolderUtil folderUtil = new DragenFolderUtil(dragenConfig, runChamber.getIlluminaSequencingRun());
                File r1Fastq = folderUtil.getReadOneFastQ(mercurySample.getSampleKey(), runChamber.getLaneNumber());
                File r2Fastq = folderUtil.getReadTwoFastQ(mercurySample.getSampleKey(), runChamber.getLaneNumber());

                String rgLine = String.format(LINE_FORMAT, rgId, mercurySample.getSampleKey(), "UnknownLibrary",
                        runChamber.getLaneNumber(), r1Fastq.getPath(), r2Fastq.getPath());
                sb.append(rgLine);
                break;
            }
        }

        FileUtils.writeStringToFile(outputFile, sb.toString());

        return foundSample;
    }

    // For Testing
    public void setDragenConfig(DragenConfig dragenConfig) {
        this.dragenConfig = dragenConfig;
    }
}
