package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class SampleInstanceEjbTest extends Arquillian {
    private Random random = new Random(System.currentTimeMillis());
    private final String INDICATOR = "(";

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

    private enum Pooled {Y, N}
    private enum MetadataExists {Y, N}

    private Object[][] testCases = new Object[][]{
            {
                    // Test case 1:
                    // The spreadsheet has only the minimium - required headers and data.
                    // Tube barcodes and library names are generated but not sample names.
                    // The main point is to test that non-required fields may be blank, and
                    // also when the sample name is implied from the library name.
                    "externalLibMinimal.xlsx", Pooled.N, MetadataExists.N},
            {
                    // Test case 2:
                    // The spreadsheet has existing Mercury sample names, two of which are Buick
                    // samples (Mercury metadata source) and two are BSP (BSP metadata source).
                    // Metadata is not given in the spreadsheet.
                    // The main point is to see that metadata is correctly fetched.
                    "externalLibExistingMetadata.xlsx", Pooled.N, MetadataExists.Y},
            {
                    // Test case 3:
                    // An upload and then a redo using the same tube barcode, which should
                    // replace all previous tube content.
                    // The main point is to see that old tube content is removed by an overwrite.
                    //
                    // The first spreadsheet has one pooled tube with 384 libraries and no
                    // sample names given.
                    "externalLibPooled384.xlsx," +
                            // The second spreadsheet has the same tube barcode but only two
                            // libraries and they are different ones.
                            "externalLibPooledReplace.xlsx",
                    Pooled.Y, MetadataExists.N},
            {
                    // Test case 4:
                    // An upload and then two redos using the same tube barcodes, libraries, and samples.
                    // The main point is to test updates to SampleInstanceEntity fields.
                    //
                    // The first spreadsheet supplies a value for every data field, which are all
                    // verified in the resulting entities.
                    "externalLibWide.xlsx," +
                            // The second spreadsheet reuses the same barcodes, libraries, and samples,
                            // and gives updated values for other fields.
                            "externalLibWideUpdate.xlsx," +
                            // The third spreadsheet reuses the same tube barcodes and library names
                            // and deletes the values for all optional fields.
                            "externalLibWideUpdateBlanks.xlsx",
                    Pooled.N, MetadataExists.N}
    };

    /**
     * Each test uploads a spreadsheet with generated randomized tube barcode and library name for uniqueness.
     * The upload is expected to succeed. Entity data is validated by matching to what was in the spreadsheet.
     */
    @Test
    public void testUploadSuccess() throws Exception {
        for (Object[] testParameters : testCases) {
            String[] filenames = ((String) testParameters[0]).split(",");
            Pooled pooled = (Pooled) testParameters[1];
            MetadataExists metadataExists = (MetadataExists) testParameters[2];
            String base = String.format("%06d", random.nextInt(1000000));
            boolean overwrite = false;

            for (String filename : filenames) {
                MessageCollection messageCollection = new MessageCollection();
                final ExternalLibraryProcessor processor = new ExternalLibraryProcessor();
                sampleInstanceEjb.doExternalUpload(VarioskanParserTest.getSpreadsheet(filename), overwrite,
                        processor, messageCollection, () -> {
                            // Supplies randomized value when the spreadsheet's value starts with an indicator.
                            for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                                if (processor.getBarcodes().get(i).startsWith(INDICATOR)) {
                                    // The pooled upload reuses the one barcode on all rows.
                                    processor.getBarcodes().set(i, "LYfwuP" + base + (pooled == Pooled.Y ? "0" : i));
                                }
                                if (processor.getLibraryNames().get(i).startsWith(INDICATOR)) {
                                    processor.getLibraryNames().set(i, "7eBqL" + base + i);
                                }
                                String sampleAndRootName = "w3TGc" + base + i;
                                if (StringUtils.trimToEmpty(processor.getSampleNames().get(i)).startsWith(INDICATOR)) {
                                    processor.getSampleNames().set(i, sampleAndRootName);
                                }
                                if (StringUtils.trimToEmpty(processor.getRootSampleNames().get(i))
                                        .startsWith(INDICATOR)) {
                                    processor.getRootSampleNames().set(i, sampleAndRootName);
                                }
                            }
                        });

                // There should be no error messages.
                Assert.assertTrue(messageCollection.getErrors().isEmpty(),
                        "In " + filename + ": " + StringUtils.join(messageCollection.getErrors(), "; "));

                // The result entities are bulk fetched.
                Multimap<String, SampleInstanceEntity> sampleInstanceEntityMap = HashMultimap.create();
                sampleInstanceEntityDao.findByBarcodes(processor.getBarcodes()).stream().
                        distinct().
                        forEach(sampleInstanceEntity ->
                                sampleInstanceEntityMap.put(sampleInstanceEntity.getLabVessel().getLabel(),
                                        sampleInstanceEntity));

                // Verify that the sampleInstanceEntities for a tube match only what's in the upload,
                // i.e. any old ones have been removed when it's an overwrite upload.
                sampleInstanceEntityMap.keySet().stream().forEach(barcode -> {
                    Set<String> librariesInUpload = IntStream.range(0, processor.getBarcodes().size()).
                            filter(idx -> barcode.equals(processor.getBarcodes().get(idx))).
                            mapToObj(idx -> processor.getLibraryNames().get(idx)).
                            collect(Collectors.toSet());
                    Set<String> librariesInSampleInstanceEntity = sampleInstanceEntityMap.get(barcode).stream().
                            map(SampleInstanceEntity::getSampleLibraryName).collect(Collectors.toSet());
                    Assert.assertEquals(librariesInSampleInstanceEntity, librariesInUpload,
                            "Wrong SampleInstanceEntities in tube " + barcode);
                });

                Set<String> uniqueBarcodes = new HashSet<>();
                // Verifies each spreadsheet row is a SampleInstanceEntity having
                // the correct tube, sample, and other values.
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    String msg = "In " + filename + " Row " + (i + 2) + " ";
                    String barcode = processor.getBarcodes().get(i);
                    boolean firstOccurrence = uniqueBarcodes.add(barcode);
                    Collection<SampleInstanceEntity> sampleInstanceEntities = sampleInstanceEntityMap.get(barcode);
                    String libraryName = processor.getLibraryNames().get(i);
                    SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntities.stream().
                            filter(entity -> entity.getSampleLibraryName().equals(libraryName)).
                            findFirst().orElse(null);
                    Assert.assertNotNull(sampleInstanceEntity, msg + "missing SampleInstanceEntity for " +
                            barcode + ", " + libraryName);

                    String sampleName = SampleInstanceEjb.get(processor.getSampleNames(), i);
                    if (StringUtils.isBlank(sampleName)) {
                        // Expects the library name to be used as the implied sample name.
                        sampleName = libraryName;
                    }
                    MercurySample mercurySample = sampleInstanceEntity.getMercurySample();
                    Assert.assertNotNull(mercurySample, msg + "missing " + sampleName);
                    Assert.assertEquals(mercurySample.getSampleKey(), sampleName, msg + sampleName);

                    SampleData sampleData = mercurySample.getSampleData();
                    if (metadataExists == MetadataExists.Y) {
                        // If the spreadsheet's samples or root samples exist and the the spreadsheet
                        // doesn't give sample metadata, then just test that some metadata is present.
                        Assert.assertTrue(StringUtils.isBlank(
                                SampleInstanceEjb.get(processor.getCollaboratorParticipantIds(), i)) &&
                                StringUtils.isNotBlank(sampleData.getPatientId()), msg + sampleName);
                        Assert.assertTrue(StringUtils.isBlank(
                                SampleInstanceEjb.get(processor.getSexes(), i)) &&
                                StringUtils.isNotBlank(sampleData.getGender()), msg + sampleName);
                    } else {
                        Assert.assertTrue(testEquals(processor.getCollaboratorSampleIds(), i,
                                sampleData.getCollaboratorsSampleName()), msg + sampleName);
                        Assert.assertTrue(testEquals(processor.getCollaboratorParticipantIds(), i,
                                sampleData.getCollaboratorParticipantId()), msg + sampleName);
                        Assert.assertTrue(testEquals(processor.getSexes(), i, sampleData.getGender()),
                                msg + sampleName);
                        Assert.assertTrue(testEquals(processor.getOrganisms(), i, sampleData.getOrganism()),
                                msg + sampleName);
                        // SampleData returns sampleId when asked for rootSampleId and rootSampleId is null.
                        // That makes it impossible to test when root sample id is blank.
                        if (StringUtils.isNotBlank(SampleInstanceEjb.get(processor.getRootSampleNames(), i)) ||
                                !sampleData.getRootSample().equals(sampleData.getSampleId())) {
                            Assert.assertTrue(testEquals(processor.getRootSampleNames(), i,
                                    sampleData.getRootSample()), msg + sampleName);
                        }
                    }
                    Assert.assertTrue(testEquals(processor.getMolecularBarcodeNames(), i,
                            sampleInstanceEntity.getMolecularIndexingScheme() == null ?
                                    null : sampleInstanceEntity.getMolecularIndexingScheme().getName()), msg);
                    Assert.assertTrue(testEquals(processor.getBaits(), i,
                            sampleInstanceEntity.getReagentDesign() == null ?
                                    null : sampleInstanceEntity.getReagentDesign().getDesignName()), msg);
                    Assert.assertTrue(testEquals(processor.getAggregationParticles(), i,
                            sampleInstanceEntity.getAggregationParticle()), msg);
                    Assert.assertTrue(testEquals(processor.getDataAnalysisTypes(), i,
                            sampleInstanceEntity.getAnalysisType().getName()), msg);
                    Assert.assertTrue(testEquals(processor.getAggregationDataTypes(), i,
                            sampleInstanceEntity.getAggregationDataType()), msg);
                    Assert.assertTrue(testEquals(processor.getReadLengths(), i, sampleInstanceEntity.getReadLength()),
                            msg);
                    Assert.assertTrue(testEquals(processor.getUmisPresents(), i,
                            sampleInstanceEntity.getUmisPresent()), msg);
                    // The "once per tube" values only need to match on the first occurrence of the tube
                    // and may be blank after that.
                    if (firstOccurrence || StringUtils.isNotBlank(SampleInstanceEjb.get(processor.getVolumes(), i))) {
                        Assert.assertTrue(testEquals(processor.getVolumes(), i,
                                sampleInstanceEntity.getLabVessel().getVolume()), msg);
                    }
                    if (firstOccurrence || StringUtils.isNotBlank(
                            SampleInstanceEjb.get(processor.getFragmentSizes(), i))) {
                        // There should be only one fragment size.
                        List<LabMetric> metrics = sampleInstanceEntity.getLabVessel().
                                getNearestMetricsOfType(LabMetric.MetricType.FINAL_LIBRARY_SIZE);
                        Assert.assertEquals(metrics.size(), 1, msg);
                        Assert.assertTrue(testEquals(processor.getFragmentSizes(), i, metrics.get(0).getValue()), msg);
                    }
                    if (firstOccurrence || StringUtils
                            .isNotBlank(SampleInstanceEjb.get(processor.getConcentrations(), i))) {
                        Assert.assertTrue(testEquals(processor.getConcentrations(), i,
                                sampleInstanceEntity.getLabVessel().getConcentration()), msg);
                    }
                    Assert.assertTrue(testEquals(processor.getInsertSizes(), i, sampleInstanceEntity.getInsertSize()),
                            msg);
                    Assert.assertTrue(testEquals(processor.getReferenceSequences(), i,
                            sampleInstanceEntity.getReferenceSequence() == null ?
                                    null : sampleInstanceEntity.getReferenceSequence().getName()), msg);
                    Assert.assertTrue(testEquals(processor.getSequencingTechnologies(), i,
                            sampleInstanceEntity.getSequencerModel() == null ?
                                    null : sampleInstanceEntity.getSequencerModel().getTechnology()), msg);
                }
                // Sets overwrite in case there is another upload in the same test case.
                overwrite = true;
            }
        }
    }

    /**
     * Tests that an update of an existing tube fails when overwrite is not set and
     * then passes when overwrite is set.
     */
    @Test
    public void testExistingTubeOverwriteFail() throws Exception {
        String filename = "externalLibMinimal.xlsx";
        String base = String.format("%06d", random.nextInt(1000000));
        // Uploads the tube with a new library (and implied sample).
        // Re-uploads the tube with a new library without overwrite set. It should fail.
        // Re-uploads the tube a 3rd time with a new library with overwrite set. It should pass.
        for (int testNumber = 0; testNumber < 3; ++testNumber) {
            final int finalTestNumber = testNumber;
            boolean overwrite = new boolean[]{false, false, true}[testNumber];
            MessageCollection messageCollection = new MessageCollection();
            final ExternalLibraryProcessor processor = new ExternalLibraryProcessor();
            sampleInstanceEjb.doExternalUpload(VarioskanParserTest.getSpreadsheet(filename), overwrite,
                    processor, messageCollection, () -> {
                        for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                            processor.getBarcodes().set(i, "UyfHg" + base + i);
                            processor.getLibraryNames().set(i, "ipAjF" + finalTestNumber + base + i);
                        }
                    });
            switch (testNumber) {
            case 0:
            case 2:
                Assert.assertTrue(messageCollection.getErrors().isEmpty(), "at " + testNumber);
                break;
            case 1:
                Assert.assertTrue(messageCollection.getErrors().contains(
                        String.format(SampleInstanceEjb.PREXISTING, 2)) &&
                                messageCollection.getErrors().contains(
                                        String.format(SampleInstanceEjb.PREXISTING, 3)), "at " + testNumber);
                break;
            }

        }
    }

    /**
     * 1) Tests that a referencing an existing sample unchanged in a new library does not
     * require overwrite to be set.
     * 2) Tests that an update of an existing sample (i.e. its metadata) fails when overwrite
     * is not set and then passes when overwrite is set.
     */
    @Test
    public void testExistingSampleOverwriteFail() throws Exception {
        String filename = "externalLibMinimal.xlsx";
        String base = String.format("%06d", random.nextInt(1000000));
        String[] csBase = {"CS_HFwK", "CS_HFwK", "CS_Gxi3c", "CS_Gxi3c"};

        // Uploads a sample in a new tube and library.
        // Re-uploads the sample in a new tube and library without overwrite set, it should pass.
        // A third upload of the sample in a new tube and library with a metadata change, without
        // overwrite set, it should fail.
        // A fourth upload with the metadata change and overwrite set should pass.
        for (int testNumber = 0; testNumber < 4; ++testNumber) {
            final int finalTestNumber = testNumber;
            boolean overwrite = new boolean[]{false, false, false, true}[testNumber];
            MessageCollection messageCollection = new MessageCollection();
            final ExternalLibraryProcessor processor = new ExternalLibraryProcessor();
            sampleInstanceEjb.doExternalUpload(VarioskanParserTest.getSpreadsheet(filename), overwrite,
                    processor, messageCollection, () -> {
                        for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                            processor.getBarcodes().set(i, "ea83I" + finalTestNumber + base + i);
                            processor.getLibraryNames().set(i, "kk4bk" + finalTestNumber + base + i);
                            processor.getSampleNames().set(i, "a6M11" + base + i);
                            processor.getCollaboratorSampleIds().set(i, csBase[finalTestNumber] + i);
                        }
                    });
            switch (testNumber) {
            case 0:
            case 1:
            case 3:
                Assert.assertTrue(messageCollection.getErrors().isEmpty(), "at " + testNumber + ": " +
                        StringUtils.join(messageCollection.getErrors(), " ; "));
                break;
            case 2:
                List<String> failStrings = Arrays.asList(
                        String.format(SampleInstanceEjb.MERCURY_METADATA, 2, String.format("%s (=%s)",
                                ExternalLibraryProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), csBase[0] + 0)),
                        String.format(SampleInstanceEjb.MERCURY_METADATA, 3, String.format("%s (=%s)",
                                ExternalLibraryProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), csBase[0] + 1)));
                Assert.assertEquals(messageCollection.getErrors(), failStrings);
                break;
            }

        }
    }

    private boolean testEquals(List<String> spreadsheetValues, int index, String actualValue) {
        String spreadsheetValue = SampleInstanceEjb.get(spreadsheetValues, index);
        // For purposes of comparison, the delete token is equivalent to blank string.
        if (ExternalLibraryProcessor.DELETE_TOKEN.equalsIgnoreCase(spreadsheetValue)) {
            spreadsheetValue = "";
        }
        if (StringUtils.isBlank(spreadsheetValue)) {
            return StringUtils.isBlank(actualValue);
        } else {
            return spreadsheetValue.equals(actualValue);
        }
    }

    private boolean testEquals(List<String> spreadsheetValues, int index, BigDecimal actualValue) {
        String spreadsheetValue = SampleInstanceEjb.get(spreadsheetValues, index);
        if (StringUtils.isBlank(spreadsheetValue)) {
            return actualValue == null || actualValue.equals(BigDecimal.ZERO);
        } else {
            return MathUtils.isSame(actualValue, new BigDecimal(spreadsheetValue));
        }
    }

    private boolean testEquals(List<String> spreadsheetValues, int index, Integer actualValue) {
        String spreadsheetValue = SampleInstanceEjb.get(spreadsheetValues, index);
        if (StringUtils.isBlank(spreadsheetValue)) {
            return actualValue == null || actualValue.equals(0);
        } else {
            return actualValue.equals(new Integer(spreadsheetValue));
        }
    }

    private boolean testEquals(List<String> spreadsheetValues, int index, Boolean actualValue) {
        Boolean spreadsheetValue = ExternalLibraryProcessor.asBoolean(SampleInstanceEjb.get(spreadsheetValues, index));
        return Objects.equals(actualValue, spreadsheetValue);
    }
}
