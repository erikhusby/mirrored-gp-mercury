package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import static com.javafx.tools.doclets.formats.html.markup.HtmlStyle.bar;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation_.barcode;

@Test(groups = TestGroups.STANDARD)
public class SampleInstanceEjbTest extends Arquillian {
    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

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
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    if (isFirst) {
                        String randomDigits = String.format("%010d", random.nextInt(1000000000));
                        processor.getBarcodes().set(i, "E" + randomDigits);
                        processor.getSingleSampleLibraryName().set(i, "Library" + randomDigits);
                        processor.getBroadSampleId().set(i, "SM-" + randomDigits);
                        processor.getRootSampleId().set(i, "SM-" + randomDigits);
                    }
                    Assert.assertTrue(processor.getMolecularIndexingScheme().get(i).startsWith("Illumina_P5"), filename);
                    Assert.assertTrue(processor.getSingleSampleLibraryName().get(i).contains("Library"), filename);
                }

                MessageCollection messageCollection = new MessageCollection();
                sampleInstanceEjb.verifyAndPersistSpreadsheet(processor, messageCollection, !isFirst);

                if (expectSuccess) {
                    Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                            StringUtils.join(messageCollection.getErrors(), "  ;  "));
                } else {
                    Assert.assertEquals(messageCollection.getErrors().size(), 2);
                    int rowNumber = 3;
                    for (String message : messageCollection.getErrors()) {
                        Assert.assertTrue(message.startsWith("Duplicate Molecular Indexing Scheme") &&
                                message.contains("at row " + rowNumber), "In " + filename + ": " +
                                StringUtils.join(messageCollection.getErrors(), "  ;  "));
                        ++rowNumber;
                    }
                }
                isFirst = false;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }
}
