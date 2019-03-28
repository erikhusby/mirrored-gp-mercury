package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Test(groups = TestGroups.STANDARD)
public class MayoManifestEjbTest extends Arquillian {
    private static final String CSV = "filename.csv";
    private Random random = new Random(System.currentTimeMillis());
    private GoogleBucketDao googleBucketDao = new GoogleBucketDao();
    private static Deployment deployment = Deployment.DEV;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    @org.jboss.arquillian.container.test.api.Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(deployment);
    }

    @BeforeTest(groups = TestGroups.STANDARD)
    public void beforeTest() {
        // This config is from the yaml file.
        MayoManifestConfig mayoManifestConfig = (MayoManifestConfig) MercuryConfiguration.getInstance().
                getConfig(MayoManifestConfig.class, deployment);
        googleBucketDao.setConfigGoogleStorageConfig(mayoManifestConfig);
    }

    @Test(groups = TestGroups.STANDARD)
    public void testReuploadManifest() throws Exception {
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = String.format("%09d", random.nextInt(10000000));
        List<List<String>> cellGrid = makeCellGrid(testDigits, 4);
        byte[] content = cellGrid.stream().
                map(list -> list.stream().collect(Collectors.joining(","))).
                collect(Collectors.joining("\n")).
                getBytes();
        String filename = "test_" + testDigits;
        googleBucketDao.upload(filename, content, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Thread.sleep(1000);

        MayoReceivingActionBean bean = new MayoReceivingActionBean();
        bean.setMessageCollection(messageCollection);
        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        Multimap<String, ManifestRecord> records = processor.makeManifestRecords(
                processor.parseAsCellGrid(content, CSV, messageCollection), CSV, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        Assert.assertEquals(records.values().size(), 1);
    }

    private List<List<String>> makeCellGrid(String testSuffix, int numberTubes) {
        List<List<String>> cellGrid = new ArrayList<>();
        cellGrid.add(Arrays.asList(
                "Package Id", "Box Label", "Matrix Id", "Well Position", "Sample Id", "Parent Sample Id",
                "Biobankid_Sampleid", "Collection Date", "Biobank Id", "Sex At Birth",
                "Age", "Sample Type", "Treatments", "Quantity (ul)", "Total Concentration (ng/ul)",
                "Total Dna(ng)", "Visit Description", "Sample Source", "Study", "Tracking Number",
                "Contact", "Email", "Requesting Physician", "Test Name"));

        for (int i = 0; i < numberTubes; ++i) {
            String iSuffix = String.format("%s%03d", testSuffix, i);
            String position = String.format("%c%02d", "ABCDEFGH".charAt(i / 12), (i % 12) + 1); // A01 thru H12
            cellGrid.add(Arrays.asList(
                    "Pk-" + testSuffix, "Bx-" + testSuffix, "T" + iSuffix, position, "S-" + iSuffix,
                    "PS-" + iSuffix, "BiS-" + iSuffix, "02/03/2019", "Bi-" + iSuffix, (i % 2 == 0 ? "M" : "F"),
                    "22", "DNA", "", "111", "222", "333", "Followup", "Whole Blood", "StudyXYZ", "Tk-%$1s",
                    "contact", "email", "Dr Who", "All Tests"));
        }
        return cellGrid;
    }
}
