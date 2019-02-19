package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class SampleInstanceEjbTest extends Arquillian {
    final private boolean OVERWRITE = true;
    private Random random = new Random(System.currentTimeMillis());

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

    @Test
    public void testUploadModified() throws Exception {
        final String filename = "PooledTubeReg.xlsx";
        final String base = String.format("%09d", random.nextInt(100000000));

        for (boolean overwrite : new boolean[]{false, true}) {
            MessageCollection messageCollection = new MessageCollection();
            ExternalLibraryProcessor processor = new ExternalLibraryProcessor();
            List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                    VarioskanParserTest.getSpreadsheet(filename), overwrite, processor, messageCollection, () -> {
                        // Modifies the spreadsheet data to make unique barcode, library, sample name, and root.
                        int count = processor.getBarcodes().size();
                        for (int i = 0; i < count; ++i) {
                            processor.getBarcodes().set(i, "E" + base + i);
                            processor.getLibraryNames().set(i, "Library" + base + i);
                            processor.getSampleNames().set(i, "SM-" + base + i);
                            processor.getRootSampleNames().set(i, "SM-" + base + "0");
                            if (overwrite) {
                                // To make metadata differences in the second upload, this updates
                                // sample data & root on the samples from the first test iteration.
                                processor.getCollaboratorSampleIds().set(i, "COLB-100" + i);
                                processor.getCollaboratorParticipantIds().set(i, "COLAB-P-100" + i);
                                if (i == 1) {
                                    processor.getSexes().set(i, "M");
                                    processor.getRootSampleNames().set(i, "SM-" + base + i);
                                }
                            }
                        }
                    });

            Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                    StringUtils.join(messageCollection.getErrors(), "; "));
            // Should have persisted all rows.
            for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                Assert.assertNotNull(labVesselDao.findByIdentifier(processor.getBarcodes().get(i)),
                        processor.getBarcodes().get(i));
                String libraryName = processor.getLibraryNames().get(i);
                Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName),
                        filename + " " + libraryName);

                String sampleName = SampleInstanceEjb.get(processor.getSampleNames(), i);
                String msg = filename + " " + sampleName;
                MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleName);
                Assert.assertNotNull(mercurySample, msg);
                SampleData sampleData = mercurySample.getSampleData();

                String rootSampleName = sampleData.getRootSample();
                MercurySample rootSample = mercurySampleDao.findBySampleKey(rootSampleName);
                Assert.assertTrue(StringUtils.isBlank(rootSampleName) || rootSample != null,
                        msg + " " + rootSampleName);

                Assert.assertEquals(sampleData.getCollaboratorsSampleName(),
                        SampleInstanceEjb.get(processor.getCollaboratorSampleIds(), i), msg);
                Assert.assertEquals(sampleData.getCollaboratorParticipantId(),
                        SampleInstanceEjb.get(processor.getCollaboratorParticipantIds(), i), msg);
                Assert.assertEquals(sampleData.getGender(),
                        SampleInstanceEjb.get(processor.getSexes(), i), msg);
                Assert.assertEquals(sampleData.getOrganism(),
                        SampleInstanceEjb.get(processor.getOrganisms(), i), msg);

                if (sampleData.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                    Set<String> uniqueKeyNames = new HashSet<>();
                    for (Metadata metadata : mercurySample.getMetadata()) {
                        Assert.assertTrue(uniqueKeyNames.add(metadata.getKey().name()),
                                "Duplicate MercurySample metadata key " + metadata.getKey().name());
                    }
                }
            }
        }
    }

    @Test
    public void testUpload() throws Exception {
        for (String filename : Arrays.asList(
                "externalLibArquillian2.xlsx",
                "PooledTube_Test-363_case2.xls",
                "PooledTube_Test-363_case4.xls")) {
            MessageCollection messageCollection = new MessageCollection();
            ExternalLibraryProcessor processor = new ExternalLibraryProcessor();
            List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                    new ByteArrayInputStream(IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename))),
                    true, processor, messageCollection, null);
            // Should be no errors.
            Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                    StringUtils.join(messageCollection.getErrors(), "; "));
            // Should have persisted all rows.
            Assert.assertEquals(entities.size(), processor.getBarcodes().size());
            for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                Assert.assertNotNull(labVesselDao.findByIdentifier(processor.getBarcodes().get(i)),
                        processor.getBarcodes().get(i));
                String libraryName = processor.getLibraryNames().get(i);
                Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName),
                        filename + " " + libraryName);

                String sampleName = SampleInstanceEjb.get(processor.getSampleNames(), i);
                String msg = filename + " " + sampleName;
                MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleName);
                Assert.assertNotNull(mercurySample, msg);
                SampleData sampleData = mercurySample.getSampleData();

                String rootSampleName = sampleData.getRootSample();
                MercurySample rootSample = mercurySampleDao.findBySampleKey(rootSampleName);
                Assert.assertTrue(StringUtils.isBlank(rootSampleName) || rootSample != null,
                        msg + " " + rootSampleName);

                // This test uses sample SM-748OO with metadata in Mercury.
                Assert.assertEquals(sampleData.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
                Assert.assertEquals(sampleData.getPatientId(), "12001-015", msg);
                Assert.assertEquals(sampleData.getCollaboratorsSampleName(), "12102402873", msg);
                Assert.assertEquals(sampleData.getCollaboratorParticipantId(), "12001-015", msg);
                Assert.assertEquals(sampleData.getGender(), "Male", msg);
                Assert.assertTrue(StringUtils.isBlank(sampleData.getOrganism()), msg);
                Assert.assertTrue(StringUtils.isBlank(sampleData.getSampleLsid()), msg);

                Set<String> uniqueKeyNames = new HashSet<>();
                for (Metadata metadata : mercurySample.getMetadata()) {
                    Assert.assertTrue(uniqueKeyNames.add(metadata.getKey().name()),
                            "Duplicate MercurySample metadata key " + metadata.getKey().name());
                }
            }
        }
    }

    @Test
    public void testTubeBarcodeUpdate() throws Exception {
        String base = String.format("%09d", random.nextInt(100000000));

        // First uploads some pooled tubes.
        String filename1 = "PooledTubeReg.xlsx";
        ExternalLibraryProcessor processor1 = new ExternalLibraryProcessor();
        MessageCollection messages1 = new MessageCollection();
        sampleInstanceEjb.doExternalUpload(VarioskanParserTest.getSpreadsheet(filename1), !OVERWRITE,
                processor1, messages1, () -> {
                    // Modifies the spreadsheet data to make unique barcode, library, sample name.
                    int count = processor1.getBarcodes().size();
                    for (int i = 0; i < count; ++i) {
                        processor1.getBarcodes().set(i, "E" + base + i);
                        processor1.getLibraryNames().set(i, "Library" + base + i);
                        processor1.getSampleNames().set(i, "SM-" + base + i);
                        processor1.getRootSampleNames().set(i, "");
                    }
                });
        Assert.assertTrue(messages1.getErrors().isEmpty(), "In " + filename1 + ": " +
                StringUtils.join(messages1.getErrors(), "; "));
        // Should have persisted all rows.
        for (int i = 0; i < processor1.getBarcodes().size(); ++i) {
            Assert.assertNotNull(labVesselDao.findByIdentifier(processor1.getBarcodes().get(i)),
                    processor1.getBarcodes().get(i));
            String libraryName = processor1.getLibraryNames().get(i);
            Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName),
                    filename1 + " " + libraryName);
        }
    }
}
