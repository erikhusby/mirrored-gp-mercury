package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import javax.enterprise.context.Dependent;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculates a LOD score for concordance between fingerprints.
 */
@Dependent
public class ConcordanceCalculator {

    private static final String JAR_FILE = "/prodinfo/prodapps/PicardWrapper/PicardWrapper-2.0-SNAPSHOT-jar-with-dependencies.jar";

    public enum Comparison {
        ONE_TO_ONE("OneToOne"),
        MATRIX("Matrix");

        private final String displayName;

        Comparison(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public double calculateAggregationLodScore(String sampleKey, Fingerprint fluidigmFingerprint, String vcfPath,
                                                         String haplotypeDatabase, String fasta) {
        // Spawn a separate process, so temp VCF files get deleted.
        List<String> commands = new ArrayList<>();
        commands.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        commands.add("-cp");
        commands.add(convertFilePaths(JAR_FILE));
        commands.add("org.broadinstitute.gpinformatics.infrastructure.picard.Main");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            Process process = processBuilder.start();
            writeAggregationJson(new OutputStreamWriter(process.getOutputStream()), sampleKey, fluidigmFingerprint,
                    vcfPath, haplotypeDatabase, fasta);
            JSONTokener jsonTokener = new JSONTokener(new InputStreamReader(process.getInputStream()));
            JSONObject jsonObject = new JSONObject(jsonTokener);
            return jsonObject.getDouble("lodScore");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * For a list of observed fingerprints and a list of expected fingerprints, calculates LOD scores, one-to-one
     * or a matrix.
     * @param observedFps
     * @param expectedFps
     * @param comparison
     * @return list of triples of observed sample ID, expected sample ID, LOD score
     */
    public List<Triple<String, String, Double>> calculateLodScores(List<Fingerprint> observedFps,
                                                                   List<Fingerprint> expectedFps, Comparison comparison) {
        // Spawn a separate process, so temp VCF files get deleted.
        List<String> commands = new ArrayList<>();
        commands.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        commands.add("-cp");
        commands.add(convertFilePaths(JAR_FILE));
        commands.add("org.broadinstitute.gpinformatics.infrastructure.picard.Main");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            Process process = processBuilder.start();
            writeJson(new OutputStreamWriter(process.getOutputStream()), observedFps, expectedFps, comparison);
            // todo jmt find a way to monitor stderr without deadlocking on stdin
/*
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = bufferedReader.readLine ();
            if (line!= null) {
                throw new RuntimeException(line);
            }
*/
            return readJson(new JSONTokener(new InputStreamReader(process.getInputStream())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read JSON array of LOD scores.
     * @param jsonTokener
     * @return list of triples of observed sample ID, expected sample ID, LOD score
     */
    private List<Triple<String, String, Double>> readJson(JSONTokener jsonTokener) {
        List<Triple<String, String, Double>> lodScores = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonTokener);
            JSONArray jsonLodScores = jsonObject.getJSONArray("lodScores");
            for (int i = 0; i < jsonLodScores.length(); i++) {
                JSONObject lodScore = (JSONObject) jsonLodScores.get(i);
                lodScores.add(new ImmutableTriple<>((String)lodScore.get("observedSample"),
                        (String)lodScore.get("expectedSample"), lodScore.getDouble("lodScore")));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return lodScores;
    }

    /**
     * Write JSON of arrays of observed fingerprints and expected fingerprints.
     */
    public void writeJson(Writer writer, List<Fingerprint> observedFps, List<Fingerprint> expectedFps,
                           Comparison comparison) {
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);

            jsonWriter.object();
            jsonWriter.key("comparison").value(comparison.getDisplayName());
            jsonWriter.key("function").value("LodScoreCalculator");

            jsonWriter.key("observedFingerprints");
            jsonWriter.array();
            for (Fingerprint fingerprint : observedFps) {
                writeFingerprint(jsonWriter, fingerprint);
            }
            jsonWriter.endArray();

            jsonWriter.key("expectedFingerprints");
            jsonWriter.array();
            for (Fingerprint fingerprint : expectedFps) {
                writeFingerprint(jsonWriter, fingerprint);
            }
            jsonWriter.endArray();

            jsonWriter.endObject();
            writer.flush();
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write JSON of arrays of observed fingerprints and expected fingerprints.
     */
    private void writeAggregationJson(Writer writer, String sampleKey, Fingerprint fluidigmFingerprint, String vcfPath,
                           String haplotypePath, String fastaPath) {
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);

            jsonWriter.object();
            jsonWriter.key("function").value("AggregationLodScoreCalculator");

            jsonWriter.key("observedFingerprints");
            jsonWriter.array();
            writeFingerprint(jsonWriter, fluidigmFingerprint);
            jsonWriter.endArray();

            jsonWriter.key("genotypes").value(vcfPath);
            jsonWriter.key("fasta").value(fastaPath);
            jsonWriter.key("haplotype").value(haplotypePath);
            jsonWriter.key("sampleKey").value(sampleKey);

            jsonWriter.endObject();
            writer.flush();
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write JSON for a fingerprint and its genotypes.
     */
    private void writeFingerprint(JSONWriter jsonWriter, Fingerprint fingerprint) throws JSONException {
        jsonWriter.object();
        jsonWriter.key("sampleId").value(fingerprint.getMercurySample().getSampleKey());
        jsonWriter.key("disposition").value(fingerprint.getDisposition().getAbbreviation());
        jsonWriter.key("platform").value(fingerprint.getPlatform());
        jsonWriter.key("genomeBuild").value(fingerprint.getGenomeBuild());
        jsonWriter.key("snpListName").value(fingerprint.getSnpList().getName());
        jsonWriter.key("gender").value(fingerprint.getGender().getAbbreviation());
        jsonWriter.key("dateGenerated").value(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss").
                format(fingerprint.getDateGenerated()));

        jsonWriter.key("calls").array();
        for (FpGenotype fpGenotype : fingerprint.getFpGenotypesOrdered()) {
            if (fpGenotype != null) {
                jsonWriter.object();
                jsonWriter.key("rsid").value(fpGenotype.getSnp().getRsId());
                jsonWriter.key("genotype").value(fpGenotype.getGenotype());
                jsonWriter.key("callConfidence").value(fpGenotype.getCallConfidence());
                jsonWriter.endObject();
            }
        }

        jsonWriter.endArray();

        jsonWriter.endObject();
    }

    public ConcordanceCalculator() {
    }

    public double calculateLodScore(Fingerprint observedFingerprint, Fingerprint expectedFingerprint) {
        return calculateLodScores(Collections.singletonList(observedFingerprint),
                Collections.singletonList(expectedFingerprint), Comparison.ONE_TO_ONE).get(0).getRight();
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

    /**
     * OS Specific way to grab the necessary files. Mac OS will need to mount the specific server (currently helium)
     */
    public static String convertFilePaths(String path) {
        if (SystemUtils.IS_OS_WINDOWS) {
            path = path.replace("/seq/references", "\\\\helium\\seq_references");
            path = path.replace("/prodinfo/prodapps", "\\\\neon\\prodinfo_prodapps");
            path = path.replace("/seq/lims", "\\\\neon\\seq_lims");
            path = path.replace("/humgen/illumina_data", "\\\\neon\\humgen_illumina_data");
            path = FilenameUtils.separatorsToWindows(path);
        } else if (SystemUtils.IS_OS_MAC) {
            path = path.replace("/seq/references", "/volumes/seq_references");
            path = path.replace("/prodinfo/prodapps", "/volumes/prodinfo_prodapps");
            path = path.replace("/seq/lims", "/volumes/seq_lims");
            path = path.replace("/humgen/illumina_data", "/volumes/humgen_illumina_data");
        }
        return path;
    }
}
