package org.broadinstitute.gpinformatics.mercury.control.run;

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

    private static final String REPO = "C:\\java\\m2-repo\\";

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

    public List<Triple<String, String, Double>> calculateLodScores(List<Fingerprint> observedFps,
            List<Fingerprint> expectedFps, Comparison comparison) {
        List<String> commands = new ArrayList<>();
        commands.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
//        commands.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5070");
        commands.add("-cp");
        // todo jmt decide a permanent location for this. \\neon\prodinfo_prodapps /prodinfo/prodapps ?
        commands.add(REPO + "Mercury\\PicardWrapper\\1.0-SNAPSHOT\\PicardWrapper-1.0-SNAPSHOT-jar-with-dependencies.jar");
        commands.add("org.broadinstitute.gpinformatics.infrastructure.picard.LodScoreCalculator");

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

    private void writeJson(Writer writer, List<Fingerprint> observedFps, List<Fingerprint> expectedFps,
            Comparison comparison) {
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);

            jsonWriter.object();
            jsonWriter.key("comparison").value(comparison.getDisplayName());

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

    // todo jmt remove
    public void done() {
    }
}
