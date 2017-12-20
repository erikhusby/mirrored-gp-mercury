package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class SampleInstanceEjbTest extends Arquillian {
    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Inject
    private SampleInstanceEntityDao sampleInstanceEntityDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    private final Random random = new Random(System.currentTimeMillis());

    @Test
    public void testPooledTubeUpload() throws Exception {
        for (Pair<String, Boolean> pair : Arrays.asList(
                Pair.of("PooledTubeReg.xlsx", true),
                Pair.of("PooledTube_Test-363_case1.xls", true),
                Pair.of("PooledTube_Test-363_case2.xls", true),
                Pair.of("PooledTube_Test-363_case3.xls", false),
                Pair.of("PooledTube_Test-363_case4.xls", true))) {

            String filename = pair.getLeft();
            boolean expectSuccess = pair.getRight();
            boolean generateData = !filename.contains("case");
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
            Assert.assertNotNull(inputStream, "Cannot find " + filename);
            try {
                VesselPooledTubesProcessor processor = new VesselPooledTubesProcessor(null);
                PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
                Assert.assertTrue(processor.getBarcodes().size() > 0, filename);
                int randomNumber = random.nextInt(100000000) * 10; // zero in the ones position.
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    if (generateData) {
                        String randomDigits = String.format("%010d", randomNumber);
                        processor.getBarcodes().set(i, "E" + randomDigits);
                        processor.getSingleSampleLibraryName().set(i, "Library" + randomDigits);
                        processor.getBroadSampleId().set(i, "SM-" + randomDigits);
                        // Root is the same sample for the first barcode, then is slightly different.
                        if (i > 0) {
                            randomNumber++;
                        }
                        processor.getRootSampleId().set(i, "SM-" + String.format("%010d", randomNumber));
                        randomNumber++;
                    }
                }

                MessageCollection messageCollection = new MessageCollection();
                sampleInstanceEjb.verifyAndPersistSpreadsheet(processor, messageCollection, !generateData);

                if (expectSuccess) {
                    Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                            StringUtils.join(messageCollection.getErrors(), "  ;  "));
                    // Should have persisted all rows.
                    for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                        Assert.assertNotNull(labVesselDao.findByIdentifier(processor.getBarcodes().get(i)),
                                filename + " " + processor.getBarcodes().get(i));
                        String libraryName = processor.getSingleSampleLibraryName().get(i);
                        Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName),
                                filename + " " + libraryName);
                        String sampleName = processor.getBroadSampleId().get(i);
                        String msg = filename + " " + sampleName;
                        MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleName);
                        Assert.assertNotNull(mercurySample, msg);
                        SampleData sampleData = mercurySample.getSampleData();
                        String rootSampleName = sampleData.getRootSample();
                        MercurySample rootSample = mercurySampleDao.findBySampleKey(rootSampleName);
                        Assert.assertTrue(StringUtils.isBlank(rootSampleName) || rootSample != null,
                                msg + " " + rootSampleName);
                        SampleData rootSampleData = (rootSample != null) ? rootSample.getSampleData() : null;
                        if (!processor.getBroadParticipantId().get(i).isEmpty()) {
                            Assert.assertEquals((rootSampleData != null ? rootSampleData : sampleData)
                                    .getPatientId(), processor.getBroadParticipantId().get(i), msg);
                        }
                        if (!processor.getCollaboratorSampleId().get(i).isEmpty()) {
                            Assert.assertEquals((rootSampleData != null ? rootSampleData : sampleData)
                                    .getCollaboratorsSampleName(), processor.getCollaboratorSampleId().get(i), msg);
                        }
                        if (!processor.getCollaboratorParticipantId().get(i).isEmpty()) {
                            Assert.assertEquals((rootSampleData != null ? rootSampleData : sampleData)
                                            .getCollaboratorParticipantId(), processor.getCollaboratorParticipantId().get(i),
                                    msg);
                        }
                        if (!processor.getGender().get(i).isEmpty()) {
                            Assert.assertEquals(sampleData.getGender(), processor.getGender().get(i), msg);
                        }
                        if (!processor.getSpecies().get(i).isEmpty()) {
                            Assert.assertEquals(sampleData.getOrganism(), processor.getSpecies().get(i), msg);
                        }
                        if (!processor.getLsid().get(i).isEmpty()) {
                            Assert.assertEquals(sampleData.getSampleLsid(), processor.getLsid().get(i), msg);
                        }
                    }
                } else {
                    // The failing test case should not have persisted any rows.
                    for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                        String libraryName = processor.getSingleSampleLibraryName().get(i);
                        Assert.assertNull(sampleInstanceEntityDao.findByName(libraryName),
                                filename + " " + libraryName);
                    }
                    // Checks the error messages for expected problems.
                    // FYI metadata for SM-748OO is from Mercury and not BSP even though the sample is in BSP.
                    List<String> errors = new ArrayList<>(messageCollection.getErrors());
                    errorIfMissing(errors, filename, "Conflict",
                            VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(),
                            "987654321", "12001-015", "row 2");
                    errorIfMissing(errors, filename, "Conflict", VesselPooledTubesProcessor.Headers.SPECIES.getText(),
                            "canine", "null", "row 2");
                    errorIfMissing(errors, filename, "Conflict", VesselPooledTubesProcessor.Headers.LSID.getText(),
                            "Lsid", "lsid:1", "null", "row 2");
                    errorIfMissing(errors, filename, "Duplicate",
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                            "01509634244", "row 3");
                    errorIfMissing(errors, filename, VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(),
                            "cannot be added because the Broad Sample already exists", "row 3");
                    errorIfMissing(errors, filename, "Conflict", VesselPooledTubesProcessor.Headers.VOLUME.getText(),
                            "61.00", "60.00", "row 7");
                    errorIfMissing(errors, filename, "Conflict",
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "151.00", "150.00", "row 7");
                    errorIfMissing(errors, filename, "both", VesselPooledTubesProcessor.Headers.BAIT.getText(),
                            VesselPooledTubesProcessor.Headers.CAT.getText(), "row 7");
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.NUMBER_MESSAGE,
                            VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(), 8));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.NUMBER_MESSAGE,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), 8));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.VOLUME.getText(), 8));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), 8));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText(), 5));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(), 5));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(), 6));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText(), 7));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), 7));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_MESSAGE,
                            VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(), "JIRA DEV", 5));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_MESSAGE,
                            VesselPooledTubesProcessor.Headers.CONDITIONS.getText(), "sub-tasks of DEV-6796", 7));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_MESSAGE,
                            VesselPooledTubesProcessor.Headers.CONDITIONS.getText(), "sub-tasks of DEV-6796", 8));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_MESSAGE,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury", 8));
                    Assert.assertTrue(errors.isEmpty(), "Found unexpected errors: " + StringUtils.join(errors, "; "));
                }
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    private boolean errorIfMissing(List<String> errors, String filename, String... tokens) {
        for (Iterator<String> iterator = errors.iterator(); iterator.hasNext(); ) {
            String error = iterator.next();
            boolean hasAllTokens = true;
            for (String token : tokens) {
                if (!error.contains(token)) {
                    hasAllTokens = false;
                    break;
                }
            }
            if (hasAllTokens) {
                iterator.remove();
                return true;
            }
        }
        Assert.fail(filename + " missing an error message that contains \"" +
                StringUtils.join(tokens, "\" and \"") + "\"; " +
                "Existing errors are: " + StringUtils.join(errors, ";; "));
        return false;
    }
}
