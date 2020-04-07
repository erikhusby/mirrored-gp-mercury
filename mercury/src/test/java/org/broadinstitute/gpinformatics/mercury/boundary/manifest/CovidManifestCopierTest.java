package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;


public class CovidManifestCopierTest extends Arquillian {
    @Inject
    private CovidManifestCopier covidManifestCopier;
    @Inject
    private GoogleBucketDao googleBucketDao;
    @Inject
    private Deployment deployment;

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(Deployment.DEV);
    }

    @BeforeTest(groups = TestGroups.STANDARD)
    public void beforeTest() {
        if (deployment != null) {
            googleBucketDao.setConfigGoogleStorageConfig((CovidManifestBucketConfig) MercuryConfiguration.getInstance().
                    getConfig(CovidManifestBucketConfig.class, deployment));
        }
    }

    /** Tests writing a csv file to the Google bucket. */
    @Test
    public void testCsvToGoogleBucket() {
        String filename = "test-" + System.currentTimeMillis() + ".csv";
        // Writes a manifest that has 3 rows, 3 columns.
        byte[] upload = "header 1,header 2,header 3\n1-1,1-2,1-3\n2-1,2-2,2-3\n".getBytes();
        covidManifestCopier.copyContentToBucket(filename, upload);
        // Reads the file back from the Google Bucket. There should be no errors and identical file content,
        // after removing the csv field quotes added by OpenCsv
        MessageCollection messageCollection = new MessageCollection();
        byte[] download = googleBucketDao.download(filename, messageCollection);
        String downloadErrors = StringUtils.join(messageCollection.getErrors(), "; ");
        // Removes the test file from the bucket.
        googleBucketDao.delete(filename, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        Assert.assertEquals(downloadErrors, "");
        Assert.assertEquals(new String(download).replaceAll("\"", ""), new String(upload).replaceAll("\"", ""));
    }

    /** Tests writing a xlsx file to the Google bucket (as a csv file). */
    @Test
    public void testXlsxToGoogleBucket() throws IOException {
        String resourceFilename = "test_1584995285.xlsx";
        byte[] upload = IOUtils.toByteArray(VarioskanParserTest.getTestResource(resourceFilename));
        String xlsxFilename = "test-" + System.currentTimeMillis() + ".xlsx";
        String csvFilename = xlsxFilename.replaceAll(".xlsx", ".csv");
        covidManifestCopier.copyContentToBucket("/make/a/test/path/" + xlsxFilename, upload);
        // Reads the file back from the Google Bucket. There should be no errors and identical file content,
        // after removing the csv field quotes added by OpenCsv
        MessageCollection messageCollection = new MessageCollection();
        byte[] download = googleBucketDao.download(csvFilename, messageCollection);
        String downloadErrors = StringUtils.join(messageCollection.getErrors(), "; ");
        // The xlsx should not be in the bucket.
        messageCollection.clearAll();
        boolean xlsInBucket = googleBucketDao.exists(xlsxFilename, messageCollection);
        String existErrors = StringUtils.join(messageCollection.getErrors(), "; ");
        // Removes the test file from the bucket.
        messageCollection.clearAll();
        googleBucketDao.delete(csvFilename, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        Assert.assertEquals(downloadErrors, "");
        Assert.assertEquals(existErrors, "");
        Assert.assertFalse(xlsInBucket);
        Assert.assertEquals(new String(download).replaceAll("\"", ""),
                "header 1,header 2,header 3\n1-1,1-2,1-3\n2-1,2-2,2-3\n");
    }
}
