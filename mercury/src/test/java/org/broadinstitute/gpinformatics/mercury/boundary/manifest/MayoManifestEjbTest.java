package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoAdminActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

    /** Tests the Mayo Admin page's bucket access and the Mayo Package Receipt page's download. */
    @Test
    public void testBucketAccessAndDownload() {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        MayoAdminActionBean bean = new MayoAdminActionBean();
        bean.setMessageCollection(messageCollection);

        // Puts two .csv files in the bucket. They have the same filename but in different "folders".
        String testDigits = DATE_FORMAT.format(new Date());
        String packageId = "PKG-" + testDigits;
        String rackBarcode = "Bx-" + testDigits;
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, ImmutableMap.of(rackBarcode, 3));
        String filename = String.format("test_%s_1.csv", testDigits);
        String file1 = testDigits + "a/" + filename;
        String file2 = testDigits + "b/" + filename;
        googleBucketDao.upload(file1, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        googleBucketDao.upload(file2, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Tests access and obtains a file listing. The list should include the files just written.
        messageCollection.clearAll();
        mayoManifestEjb.testAccess(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        Assert.assertTrue(messageCollection.hasInfos());
        Assert.assertTrue(bean.getBucketList().contains(file1));
        Assert.assertTrue(bean.getBucketList().contains(file2));

        // Using the Package Receipt action bean, reads and download the file.
        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setFilename(file1);
        pkgBean.setMessageCollection(messageCollection);
        messageCollection.clearAll();
        // Reads by full filename.
        Assert.assertEquals(mayoManifestEjb.searchForFile(pkgBean), file1);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        // Reads by partial filename (without the .csv extension).
        pkgBean.setFilename(file1.substring(0, file1.length() - 4));
        byte[] bytes = mayoManifestEjb.download(pkgBean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        Assert.assertTrue(bytes.length > 1);
        // Compares the downloaded content to the spreadsheet.
        String byteString = new String(bytes).replaceAll("\n", ";");
        String cellGridString = cellGrid.stream().
                map(row -> StringUtils.join(row, ",")).
                collect(Collectors.joining(";"));
        Assert.assertEquals(byteString,  cellGridString);

        // Search of nonexistent file should fail.
        pkgBean.setFilename("pSm4Q1Q#fgDzgzTBG7vqSv-URQbS2fypm_nH0nDEoPX");
        Assert.assertEquals(mayoManifestEjb.searchForFile(pkgBean), "");
        Assert.assertEquals(messageCollection.getErrors().get(0),
                "Cannot find a file for " + pkgBean.getFilename() + ".");
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();

        // Search should return multiple matches when just the base filename is used.
        pkgBean.setFilename(filename);
        Assert.assertEquals(mayoManifestEjb.searchForFile(pkgBean), "");
        Assert.assertTrue(messageCollection.getErrors().get(0).contains("Found multiple files for " + filename));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();
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
