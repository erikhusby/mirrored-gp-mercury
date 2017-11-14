package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Arrays;
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
        boolean isFirst = true;
        for (Pair<String, Boolean> pair : Arrays.asList(
                Pair.of("PooledTubeReg.xlsx", true),
                Pair.of("PooledTube_Test-363_case1.xls", true),
                Pair.of("PooledTube_Test-363_case2.xls", true),
                Pair.of("PooledTube_Test-363_case3.xls", false),
                Pair.of("PooledTube_Test-363_case4.xls", true))) {

            String filename = pair.getLeft();
            boolean expectSuccess = pair.getRight();

            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
            Assert.assertNotNull(inputStream, "Cannot find " + filename);
            try {
                VesselPooledTubesProcessor processor = new VesselPooledTubesProcessor("Sheet1");
                PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
                Assert.assertTrue(processor.getBarcodes().size() > 0, filename);
                int randomNumber = random.nextInt(100000000) * 10; // zero in the ones position.
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    if (isFirst) {
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
                sampleInstanceEjb.verifyAndPersistPooledTubeSpreadsheet(processor, messageCollection, !isFirst);

                if (expectSuccess) {
                    Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                            StringUtils.join(messageCollection.getErrors(), "  ;  "));
                    // Should have persisted all rows.
                    for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                        Assert.assertNotNull(labVesselDao.findByIdentifier(processor.getBarcodes().get(i)),
                                processor.getBarcodes().get(i));
                        String libraryName = processor.getSingleSampleLibraryName().get(i);
                        Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName), libraryName);
                        String sampleName = processor.getBroadSampleId().get(i);
                        MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleName);
                        Assert.assertNotNull(mercurySample, sampleName);
                        MercurySample.MetadataSource metadataSource = mercurySample.getMetadataSource();
                        if (metadataSource == MercurySample.MetadataSource.MERCURY) {
                            Assert.assertTrue(CollectionUtils.isNotEmpty(mercurySample.getMetadata()),
                                    "No metadata for " + sampleName);
                            if (StringUtils.isNotBlank(processor.getBroadParticipantId().get(i))) {
                                boolean found = false;
                                for (Metadata metadata : mercurySample.getMetadata()) {
                                    if (metadata.getKey() == Metadata.Key.BROAD_PARTICIPANT_ID) {
                                        found = true;
                                        Assert.assertEquals(metadata.getValue(),
                                                processor.getBroadParticipantId().get(i));
                                        break;
                                    }
                                }
                                Assert.assertTrue(found, "Missing participant id on " + sampleName);
                            }
                        } else {
                            Assert.assertEquals(metadataSource, MercurySample.MetadataSource.BSP, sampleName);
                        }
                        String rootSampleName = processor.getRootSampleId().get(i);
                        if (StringUtils.isNotBlank(rootSampleName)) {
                            Assert.assertNotNull(mercurySampleDao.findBySampleKey(rootSampleName), rootSampleName);
                        }
                    }
                } else {
                    // The failing test case should not have persisted any rows.
                    for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                        String libraryName = processor.getSingleSampleLibraryName().get(i);
                        Assert.assertNull(sampleInstanceEntityDao.findByName(libraryName), libraryName);
                    }
                    // Checks the error messages for expected problems.
                    String diagnostic = "In " + filename + ": " + StringUtils.join(messageCollection.getErrors(), ";;");
                    Assert.assertEquals(messageCollection.getErrors().size(), 20, diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.CONFLICT_MESSAGE, VesselPooledTubesProcessor.Headers.VOLUME.getText(),
                            "61.00", "60.00", "", 7)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.CONFLICT_MESSAGE,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "151.00", "150.00", "", 7)),
                            diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.CONFLICT_MESSAGE, VesselPooledTubesProcessor.Headers.BAIT.getText() +
                                    " and " + VesselPooledTubesProcessor.Headers.CAT.getText(),
                            "both", "only one", "", 7)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.CONFLICT_MESSAGE, VesselPooledTubesProcessor.Headers.LSID.getText(),
                            "lsid:1", "null", "for existing Mercury Sample", 2)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.CONFLICT_MESSAGE,
                            VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(),
                            "987654321", "12001-015", "for existing Mercury Sample", 2)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.CONFLICT_MESSAGE,
                            VesselPooledTubesProcessor.Headers.SPECIES.getText(),
                            "canine", "null", "for existing Mercury Sample", 2)), diagnostic);

                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.DUPLICATE_MESSAGE,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                            "in tube 01509634244", 3)), diagnostic);

                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.NUMBER_MESSAGE, VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(),
                            8)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.NUMBER_MESSAGE,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), 8)), diagnostic);

                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.MISSING_MESSAGE, VesselPooledTubesProcessor.Headers.VOLUME.getText(),
                            8)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), 8)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText(), 5)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.MISSING_MESSAGE, VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(),
                            5)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(), 6)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText(), 7)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), 7)), diagnostic);

                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.UNKNOWN_MESSAGE, VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(),
                            "JIRA DEV", 5)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.UNKNOWN_MESSAGE, VesselPooledTubesProcessor.Headers.CONDITIONS.getText(),
                            "sub-tasks of DEV-6796", 7)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.UNKNOWN_MESSAGE, VesselPooledTubesProcessor.Headers.CONDITIONS.getText(),
                            "sub-tasks of DEV-6796", 8)), diagnostic);
                    Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                            SampleInstanceEjb.UNKNOWN_MESSAGE,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                            "Mercury", 8)), diagnostic);
                }
                isFirst = false;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }
}
