package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoAdminActionBean;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Test(groups = TestGroups.STANDARD)
public class MayoManifestEjbTest extends Arquillian {
    private GoogleBucketDao googleBucketDao = new GoogleBucketDao();
    private static Deployment deployment = Deployment.DEV;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMddHHmmss");

    @Inject
    MayoManifestEjb mayoManifestEjb;

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

    @Test
    public void testAdminBucketAccessAndViewFileByFilename() {
        String testDigits = DATE_FORMAT.format(new Date());
        String packageId = "PKG-" + testDigits;
        String rackBarcode = "Bx-" + testDigits;

        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        MayoAdminActionBean bean = new MayoAdminActionBean();
        bean.setMessageCollection(messageCollection);

        // Make sure bucket has at least one manifest file to display.
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, ImmutableMap.of(rackBarcode, 1));
        String filename = String.format("test_%s_1.csv", testDigits);
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Tests access and obtains a file listing. The list should include the file that was just written.
        messageCollection.clearAll();
        mayoManifestEjb.testAccess(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        Assert.assertTrue(messageCollection.hasInfos());
        Assert.assertTrue(bean.getBucketList().contains(filename));

        // Reads the file by its filename and compares the displayed content to the spreadsheet.
        bean.setFilename(filename);
        bean.getManifestCellGrid().clear();
        messageCollection.clearAll();
        mayoManifestEjb.readManifestFileCellGrid(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        List<List<String>> displayCellGrid = bean.getManifestCellGrid();
        Assert.assertEquals(displayCellGrid.stream().flatMap(Collection::stream).collect(Collectors.joining(" ")),
                cellGrid.stream().flatMap(Collection::stream).collect(Collectors.joining(" ")));
    }

    private byte[] makeContent (List<List<String>> cellGrid) {
        return cellGrid.stream().
                map(list -> String.join(",", list)).
                collect(Collectors.joining("\n")).
                getBytes();
    }

    private List<List<String>> makeCellGrid(String testSuffix, String packageId,
            Map<String, Integer> rackBarcodeToNumberTubes) {
        List<List<String>> cellGrid = new ArrayList<>();
        cellGrid.add(Arrays.asList(
                "PackageId", "Package StorageUnitId", "MatrixId", "Well_Position", "SampleId", "Parent_SampleId",
                "BiobankId_SampleId", "Collection_Date", "BiobankId", "Sex_At_Birth",
                "Age", "Sample_Type", "Treatments", "Quantity(ul)", "Total_Concentration(ng/ul)",
                "Total_Dna(ng)", "Visit_Description", "Sample_Source", "Study", "Tracking_Number",
                "Contact", "Email", "Requesting_Physician", "Test_Name", "NY_State(Y/N)"));

        int tubeIndex = 0;
        for (String rackBarcode : rackBarcodeToNumberTubes.keySet()) {
            for (int i = 0; i < rackBarcodeToNumberTubes.get(rackBarcode); ++i) {
                String iSuffix = String.format("%s%03d", testSuffix, tubeIndex++);
                String position = String.format("%c%02d", "ABCDEFGH".charAt(i / 12), (i % 12) + 1); // A01 thru H12
                cellGrid.add(Arrays.asList(
                        packageId, rackBarcode, "T" + iSuffix, position, "CS-" + iSuffix,
                        "PS-" + iSuffix, "BiS-" + iSuffix, "02/03/2019", "Bi-" + iSuffix, (i % 2 == 0 ? "M":"F"),
                        "22", "DNA", "", String.valueOf(100 - tubeIndex), (100 + tubeIndex) + ".13",
                        "333", "Followup", "Whole Blood", "StudyXYZ", "Tk-" + testSuffix,
                        "Minnie Me", "mm@none.org", "Dr Evil", "All Tests", (i %2 == 0 ? "Y":"N")));
            }
        }
        return cellGrid;
    }

}
