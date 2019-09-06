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
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jetbrains.annotations.NotNull;
import picard.fingerprint.FingerprintChecker;
import picard.fingerprint.HaplotypeMap;
import picard.fingerprint.MatchResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Calculates a LOD score for concordance between fingerprints.
 */
public class ConcordanceCalculator {
    // todo jmt OS-specific configuration, or require mount on Windows / MacOs?
    private HaplotypeMap haplotypes = new HaplotypeMap(new File(
            "\\\\iodine\\seq_references\\Homo_sapiens_assembly19\\v1\\Homo_sapiens_assembly19.haplotype_database.txt"));
    private File reference = new File(
            "\\\\iodine\\seq_references\\Homo_sapiens_assembly19\\v1\\Homo_sapiens_assembly19.fasta");
    private ReferenceSequenceFile ref = ReferenceSequenceFileFactory.getReferenceSequenceFile(reference);

    public double calculateLodScore(Fingerprint observedFingerprint, Fingerprint expectedFingerprint) {
        picard.fingerprint.Fingerprint observedFp = getFingerprint(observedFingerprint,
                observedFingerprint.getMercurySample().getSampleKey());
        picard.fingerprint.Fingerprint expectedFp = getFingerprint(expectedFingerprint,
                expectedFingerprint.getMercurySample().getSampleKey());

        FingerprintChecker fingerprintChecker = new FingerprintChecker(haplotypes);
        Map<String, picard.fingerprint.Fingerprint> mapSampleToObservedFp =
                fingerprintChecker.loadFingerprints(observedFp.getSource(), observedFp.getSample());
        Map<String, picard.fingerprint.Fingerprint> mapSampleToExpectedFp =
                fingerprintChecker.loadFingerprints(expectedFp.getSource(), expectedFp.getSample());

        picard.fingerprint.Fingerprint observedFp1 = mapSampleToObservedFp.get(observedFp.getSample());
        picard.fingerprint.Fingerprint expectedFp1 = mapSampleToExpectedFp.get(expectedFp.getSample());
        MatchResults matchResults = FingerprintChecker.calculateMatchResults( observedFp1, expectedFp1);
        try {
            Files.delete(observedFp1.getSource());
            Files.delete(expectedFp1.getSource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return matchResults.getLOD();
    }

    public double calculateHapMapConcordance(Fingerprint fingerprint, Control control) {
        MercurySample concordanceMercurySample = control.getConcordanceMercurySample();
        if (concordanceMercurySample == null) {
            throw new RuntimeException("No concordance sample configured for " + control.getCollaboratorParticipantId());
        }
        // todo jmt most recent passed
        Fingerprint controlFp = concordanceMercurySample.getFingerprints().iterator().next();
        return calculateLodScore(fingerprint, controlFp);
    }

    @NotNull
    private picard.fingerprint.Fingerprint getFingerprint(Fingerprint fingerprint, String sampleKey) {
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
        for (FpGenotype fpGenotype : fingerprint.getFpGenotypesOrdered()) {
            calls.add(new Fingerprints.Call(fpGenotype.getSnp().getRsId(),
                    fpGenotype.getGenotype().equals("--") ? Fingerprints.Genotype.NO_CALL : Fingerprints.Genotype.valueOf(fpGenotype.getGenotype()),
                    fpGenotype.getCallConfidence().toString(), null,null));
        }

        try {
            DownloadGenotypes downloadGenotypes = new DownloadGenotypes();
            File fpFile = File.createTempFile("Fingerprint", ".vcf");
            downloadGenotypes.OUTPUT = fpFile;
            downloadGenotypes.SAMPLE_ALIAS = sampleKey;

            List<DownloadGenotypes.SnpGenotype> snpGenotypes = DownloadGenotypes.mercuryResultsToGenotypes( 
                    fingerprints, haplotypes, null, null, 0.0);
            List<DownloadGenotypes.SnpGenotype> consistentGenotypes = DownloadGenotypes.cleanupGenotypes(snpGenotypes,
                    haplotypes);
            SequenceUtil.assertSequenceDictionariesEqual(ref.getSequenceDictionary(),
                    haplotypes.getHeader().getSequenceDictionary());
            SortedSet<VariantContext> variantContexts = downloadGenotypes.makeVariantContexts(consistentGenotypes,
                    haplotypes, ref);
            downloadGenotypes.writeVcf(variantContexts, Gender.valueOf(fingerprint.getGender().name()),
                    reference, ref.getSequenceDictionary());
            return new picard.fingerprint.Fingerprint(sampleKey, fpFile.toPath(), "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void done() {
        try {
            ref.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
