package org.broadinstitute.gpinformatics.mercury.control.run;

import edu.mit.broad.picard.genotype.fingerprint.DownloadGenotypes;
import edu.mit.broad.picard.genotype.fingerprint.Fingerprints;
import edu.mit.broad.picard.util.Gender;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import htsjdk.samtools.util.SequenceUtil;
import htsjdk.variant.variantcontext.VariantContext;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.jetbrains.annotations.NotNull;
import picard.fingerprint.FingerprintChecker;
import picard.fingerprint.HaplotypeMap;
import picard.fingerprint.MatchResults;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class ConcordanceCalculator {
    private HaplotypeMap haplotypes = new HaplotypeMap(new File(
            "\\\\iodine\\seq_references\\Homo_sapiens_assembly19\\v1\\Homo_sapiens_assembly19.haplotype_database.txt"));
    private File reference = new File(
            "\\\\iodine\\seq_references\\Homo_sapiens_assembly19\\v1\\Homo_sapiens_assembly19.fasta");
    private ReferenceSequenceFile ref = ReferenceSequenceFileFactory.getReferenceSequenceFile(reference);

    public double calculateLodScore(Fingerprint fingerprint1, String sampleKey1, Fingerprint fingerprint2, String sampleKey2) {
        picard.fingerprint.Fingerprint observedFp = getFingerprint(fingerprint1, sampleKey1);
        picard.fingerprint.Fingerprint expectedFp = getFingerprint(fingerprint2, sampleKey2);
        FingerprintChecker fingerprintChecker = new FingerprintChecker(haplotypes);
        Map<String, picard.fingerprint.Fingerprint> mapSampleToObservedFp = fingerprintChecker.loadFingerprints(observedFp.getSource(), observedFp.getSample());
        Map<String, picard.fingerprint.Fingerprint> mapSampleToExpectedFp = fingerprintChecker.loadFingerprints(expectedFp.getSource(), expectedFp.getSample());
        MatchResults matchResults = FingerprintChecker.calculateMatchResults(mapSampleToObservedFp.get(observedFp.getSample()),
                mapSampleToExpectedFp.get(expectedFp.getSample()));
        return matchResults.getLOD();
    }

    @NotNull
    private picard.fingerprint.Fingerprint getFingerprint(Fingerprint fingerprint, String sampleKey1) {
        List<Fingerprints.Fingerprint> fingerprints = new ArrayList<>();
        List<Fingerprints.Call> calls = new ArrayList<>();
        fingerprints.add(new Fingerprints.Fingerprint(fingerprint.getMercurySample().getSampleKey(),
                Fingerprints.Disposition.valueOf(fingerprint.getDisposition().getAbbreviation()),
                fingerprint.getMercurySample().getSampleKey(),
                Fingerprints.Platform.valueOf(fingerprint.getPlatform().name()),
                Fingerprints.GenomeBuild.valueOf(fingerprint.getGenomeBuild().name()),
                fingerprint.getSnpList().getName(),
                fingerprint.getDateGenerated().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                Gender.valueOf(fingerprint.getGender().name()), calls));
        for (FpGenotype fpGenotype : fingerprint.getFpGenotypes()) {
            calls.add(new Fingerprints.Call(fpGenotype.getSnp().getRsId(),
                    fpGenotype.getGenotype().equals("--") ? Fingerprints.Genotype.NO_CALL : Fingerprints.Genotype.valueOf(fpGenotype.getGenotype()),
                    fpGenotype.getCallConfidence().toString(), null,null));
        }

        DownloadGenotypes downloadGenotypes = new DownloadGenotypes();
        List<DownloadGenotypes.SnpGenotype> snpGenotypes = DownloadGenotypes.mercuryResultsToGenotypes(
                fingerprints, haplotypes, 0.0);
        DownloadGenotypes.cleanupGenotypes(snpGenotypes, haplotypes);
        File fpFile;
        try {
            SequenceUtil.assertSequenceDictionariesEqual(ref.getSequenceDictionary(),
                    haplotypes.getHeader().getSequenceDictionary());
            SortedSet<VariantContext> variantContexts = downloadGenotypes.makeVariantContexts(snpGenotypes,
                    haplotypes, ref);
            fpFile = File.createTempFile("Fingerprint", ".vcf");
            downloadGenotypes.OUTPUT = fpFile;
            downloadGenotypes.SAMPLE_ALIAS = sampleKey1;
            downloadGenotypes.writeVcf(variantContexts, Gender.valueOf(fingerprint.getGender().name()),
                    reference, ref.getSequenceDictionary());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new picard.fingerprint.Fingerprint(sampleKey1, fpFile.toPath(), "");
    }

    public void done() {
        try {
            ref.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
