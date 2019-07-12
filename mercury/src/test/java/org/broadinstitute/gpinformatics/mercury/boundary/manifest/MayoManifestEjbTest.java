package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.infrastructure.QuarantinedDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoAdminActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoSampleReceiptActionBean;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestImportProcessor.Header;
import static org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined.ItemSource;
import static org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined.ItemType;

@Test(groups = TestGroups.STANDARD)
public class MayoManifestEjbTest extends Arquillian {
    private GoogleBucketDao googleBucketDao = new GoogleBucketDao();
    private static Deployment deployment = Deployment.DEV;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMddHHmmss");
    /** Maps customfield_nnnnn to JIRA_DEFINITION_MAP.key */
    private static final Map<String, String> REVERSE_JIRA_DEFINITION_MAP = MayoManifestEjb.JIRA_DEFINITION_MAP.
            entrySet().stream().
            collect(Collectors.toMap(mapEntry -> mapEntry.getValue().getJiraCustomFieldId(), Map.Entry::getKey));

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    @Inject
    private JiraService jiraService;

    @Inject
    private QuarantinedDao quarantinedDao;

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
    public void testAccessionFailAndSuccess() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());
        String packageId = "PKG-" + testDigits;

        // Seeds the storage bucket with an unreceived manifest file.
        String unreceivedDigits = String.valueOf(Long.parseLong(testDigits) - 1);
        String unreceivedPkg = "PKG-" + unreceivedDigits;
        String unreceivedRack = "Bx-" + unreceivedDigits;
        String unreceivedFilename = "BROAD_" + unreceivedPkg + ".csv";
        googleBucketDao.upload(unreceivedFilename, makeContent(makeCellGrid(unreceivedDigits, unreceivedPkg, "A",
                ImmutableMap.of(unreceivedRack, 4))), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Makes a spreadsheet with a racks having 1 tube, 1 tube, 2 tubes, and 94 tubes.
        String[] barcodes = {
                String.format("Bx-%s%d", testDigits, 1),
                String.format("Bx-%s%d", testDigits, 2),
                String.format("Bx-%s%d", testDigits, 3),
                String.format("Bx-%s%d", testDigits, 4)};

        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS", ImmutableMap.of(
                barcodes[0], 1,
                barcodes[1], 1,
                barcodes[2], 2,
                barcodes[3], 94));
        // Writes the spreadsheet to a manifest file.
        String filename = "BROAD_" + packageId + ".csv";
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Receives the package successfully.
        {
            MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
            pkgBean.setMessageCollection(messageCollection);
            pkgBean.setPackageBarcode(packageId);
            pkgBean.setRackBarcodeString(StringUtils.join(barcodes, " "));
            pkgBean.setRackCount(String.valueOf(barcodes.length));
            messageCollection.clearAll();
            mayoManifestEjb.packageReceiptValidation(pkgBean);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            Assert.assertNull(manifestSessionDao.getSessionByPrefix(packageId));
            // Package receipt page2.
            pkgBean.setShipmentCondition("Pristine.");
            pkgBean.setDeliveryMethod("None");
            pkgBean.setTrackingNumber("TRK" + testDigits);
            mayoManifestEjb.packageReceipt(pkgBean);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            validateManifest(manifestSessionDao.getSessionByPrefix(packageId), cellGrid);
        }

        // Redoing the package receipt is disallowed unless the update flag is set.
        {
            MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
            pkgBean.setMessageCollection(messageCollection);
            pkgBean.setPackageBarcode(packageId);
            pkgBean.setRackBarcodeString(StringUtils.join(barcodes, " "));
            pkgBean.setRackCount(String.valueOf(barcodes.length));
            pkgBean.setAllowUpdate(false);
            messageCollection.clearAll();
            mayoManifestEjb.packageReceiptValidation(pkgBean);
            Assert.assertTrue(messageCollection.getErrors().contains(String.format(MayoManifestEjb.NEEDS_ALLOW_UPDATE,
                    packageId)), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
        }

        {
            // Does a re-receipt and quarantines the 1st rack.
            MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
            pkgBean.setMessageCollection(messageCollection);
            pkgBean.setPackageBarcode(packageId);
            pkgBean.setRackBarcodeString(StringUtils.join(barcodes, " "));
            pkgBean.setRackCount(String.valueOf(barcodes.length));
            pkgBean.setAllowUpdate(true);
            messageCollection.clearAll();
            mayoManifestEjb.packageReceiptValidation(pkgBean);
            Assert.assertFalse(messageCollection.hasErrors(),
                    StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            // Package receipt page2.
            pkgBean.setShipmentCondition("Pretty good.");
            pkgBean.setDeliveryMethod("FedEx");
            pkgBean.setTrackingNumber("TRK2" + testDigits);
            pkgBean.setQuarantineBarcodeAndReason(ImmutableMap.of(barcodes[0],
                    Quarantined.getRackReasons().get(0)));
            messageCollection.clearAll();
            mayoManifestEjb.packageReceipt(pkgBean);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));

            // Checks that the rack is quarantined.
            List<Quarantined> quarantined = quarantinedDao.findItems(ItemSource.MAYO).stream().
                    filter(quarantinedItem -> quarantinedItem.getItem().contains(testDigits)).
                    collect(Collectors.toList());
            Assert.assertEquals(quarantined.size(), 1);
            Assert.assertEquals(quarantined.get(0).getItem(), barcodes[0]);
            Assert.assertEquals(quarantined.get(0).getReason(), Quarantined.getRackReasons().get(0));
        }

        {
            // Does a re-receipt and quarantines the 2nd rack. This should unquarantine the 1st rack.
            MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
            pkgBean.setMessageCollection(messageCollection);
            pkgBean.setPackageBarcode(packageId);
            pkgBean.setRackBarcodeString(StringUtils.join(barcodes, " "));
            pkgBean.setRackCount(String.valueOf(barcodes.length));
            pkgBean.setAllowUpdate(true);
            messageCollection.clearAll();
            mayoManifestEjb.packageReceiptValidation(pkgBean);
            Assert.assertFalse(messageCollection.hasErrors(),
                    StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertEquals(messageCollection.getWarnings().get(0),
                    String.format(MayoManifestEjb.QUARANTINE_DELETES, barcodes[0]),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            // Package receipt page2.
            pkgBean.setShipmentCondition("Fair.");
            pkgBean.setDeliveryMethod("FedEx");
            pkgBean.setTrackingNumber("TRK2" + testDigits);
            pkgBean.setQuarantineBarcodeAndReason(ImmutableMap.of(barcodes[1], Quarantined.getRackReasons().get(0)));
            messageCollection.clearAll();
            mayoManifestEjb.packageReceipt(pkgBean);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));

            // Checks that only the 2nd rack is quarantined.
            List<Quarantined> quarantined = quarantinedDao.findItems(ItemSource.MAYO).stream().
                    filter(quarantinedItem -> quarantinedItem.getItem().contains(testDigits)).
                    collect(Collectors.toList());
            Assert.assertEquals(quarantined.size(), 1);
            Assert.assertEquals(quarantined.get(0).getItem(), barcodes[1]);
            Assert.assertEquals(quarantined.get(0).getReason(), Quarantined.getRackReasons().get(0));
        }

        // Fails to accession when the rack has an additional tube that is not in the manifest.
        // This should cause the rack to be quarantined.
        {
            MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
            bean.setMessageCollection(messageCollection);
            bean.setRackBarcode(barcodes[0]);
            bean.setRackScan(makeRackScan(cellGrid, barcodes[0]));
            bean.getRackScan().put("B02", "123123123");
            messageCollection.clearAll();
            mayoManifestEjb.validateForAccessioning(bean);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            messageCollection.clearAll();
            mayoManifestEjb.accession(bean);
            Assert.assertEquals(messageCollection.getErrors().size(), 1,
                    StringUtils.join(messageCollection.getErrors()));
            Assert.assertTrue(messageCollection.getErrors().contains(
                    String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION, "B02", "123123123", "no tube")),
                    StringUtils.join(messageCollection.getErrors()));
            // There should be no new vessels.
            Assert.assertNull(labVesselDao.findByIdentifier(barcodes[0]));
            Assert.assertNull(labVesselDao.findByIdentifier(bean.getRackScan().values().iterator().next()));
            // The rack should be quarantined
            Assert.assertNotNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.RACK, barcodes[0]));
        }

        // Fails to accession when the rack has the wrong tube.
        {
            MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
            bean.setMessageCollection(messageCollection);
            bean.setRackBarcode(barcodes[0]);
            bean.setRackScan(makeRackScan(cellGrid, barcodes[0]));
            String expectedTube = bean.getRackScan().get("A01");
            bean.getRackScan().put("A01", "123123123");
            messageCollection.clearAll();
            mayoManifestEjb.validateForAccessioning(bean);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            mayoManifestEjb.accession(bean);
            Assert.assertTrue(messageCollection.getErrors().contains(
                    String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION, "A01", "123123123", expectedTube)),
                    StringUtils.join(messageCollection.getErrors()));
            Assert.assertEquals(messageCollection.getErrors().size(), 1,
                    StringUtils.join(messageCollection.getErrors()));
            // The rack should be quarantined
            Assert.assertNotNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.RACK, barcodes[0]));
        }

        // Fails to accession when the tube is in the wrong position.
        {
            MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
            bean.setMessageCollection(messageCollection);
            bean.setRackBarcode(barcodes[2]);
            bean.setRackScan(makeRackScan(cellGrid, barcodes[2]));
            // Swaps the two tubes.
            String[] expectedTube = {bean.getRackScan().get("A01"), bean.getRackScan().get("A02")};
            bean.getRackScan().put("A01", expectedTube[1]);
            bean.getRackScan().put("A02", expectedTube[0]);
            messageCollection.clearAll();
            mayoManifestEjb.validateForAccessioning(bean);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            messageCollection.clearAll();
            mayoManifestEjb.accession(bean);
            Assert.assertTrue(messageCollection.getErrors().contains(
                    String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION, "A01", expectedTube[1], expectedTube[0])),
                    StringUtils.join(messageCollection.getErrors()));
            Assert.assertTrue(messageCollection.getErrors().contains(
                    String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION, "A02", expectedTube[0], expectedTube[1])),
                    StringUtils.join(messageCollection.getErrors()));
            Assert.assertEquals(messageCollection.getErrors().size(), 2,
                    StringUtils.join(messageCollection.getErrors()));
            // The rack should be quarantined
            Assert.assertNotNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.RACK, barcodes[2]));
        }

        // Fails to accession when a tube is missing.
        {
            MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
            bean.setMessageCollection(messageCollection);
            bean.setRackBarcode(barcodes[2]);
            bean.setRackScan(makeRackScan(cellGrid, barcodes[2]));
            String expectedTube = bean.getRackScan().get("A01");
            bean.getRackScan().put("A01", "");
            messageCollection.clearAll();
            mayoManifestEjb.validateForAccessioning(bean);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            messageCollection.clearAll();
            mayoManifestEjb.accession(bean);
            Assert.assertTrue(messageCollection.getErrors().contains(
                    String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION, "A01", "no tube", expectedTube)),
                    StringUtils.join(messageCollection.getErrors()));
            Assert.assertEquals(messageCollection.getErrors().size(), 1,
                    StringUtils.join(messageCollection.getErrors()));
            // The rack should be quarantined
            Assert.assertNotNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.RACK, barcodes[2]));
        }

        // Suppose the rack tubes were rearranged so that all the tube errors got fixed up.
        // The racks should be successfully accessioned.
        for (String barcode : barcodes) {
            MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
            bean.setMessageCollection(messageCollection);
            bean.setRackBarcode(barcode);
            bean.setRackScan(makeRackScan(cellGrid, barcode));
            messageCollection.clearAll();
            mayoManifestEjb.validateForAccessioning(bean);
            Assert.assertFalse(messageCollection.hasErrors(),
                    StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            messageCollection.clearAll();
            mayoManifestEjb.accession(bean);
            Assert.assertFalse(messageCollection.hasErrors(),
                    StringUtils.join(messageCollection.getErrors(), "; "));
            Assert.assertFalse(messageCollection.hasWarnings(),
                    StringUtils.join(messageCollection.getWarnings(), "; "));
            validateEntities(cellGrid, barcode);
        }

        // After a successful accessioning the racks should no longer be quarantined.
        Assert.assertNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.RACK, barcodes[0]));
        Assert.assertNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.RACK, barcodes[1]));
        Assert.assertNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.RACK, barcodes[2]));

        // Tests that a re-accessioning is disallowed.
        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(barcodes[0]);
        bean.setRackScan(makeRackScan(cellGrid, barcodes[0]));
        messageCollection.clearAll();
        String existingVessels = barcodes[0] + " " +
                bean.getRackScan().values().stream().sorted().collect(Collectors.joining(" "));
        messageCollection.clearAll();
        mayoManifestEjb.validateForAccessioning(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.ALREADY_ACCESSIONED, existingVessels)),
                StringUtils.join(messageCollection.getErrors(), "; "));
    }

    @Test
    public void testAmbiguousManifestFile() {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits1 = DATE_FORMAT.format(new Date());
        String testDigits2 = String.valueOf(Long.parseLong(testDigits1) + 1);
        String packageId = "PKG-" + testDigits1;

        // Puts two manifest files in the bucket for the package.
        String file1 = "BROAD_" + packageId + ".csv";
        googleBucketDao.upload(file1, makeContent(makeCellGrid(testDigits1,
                packageId, "A", ImmutableMap.of("Bx-" + testDigits1, 4))), messageCollection);

        String[] barcodes = {String.format("Bx-%s%d", testDigits2, 1), String.format("Bx-%s%d", testDigits2, 2)};
        List<List<String>> cellGrid = makeCellGrid(testDigits2, packageId, "CS", ImmutableMap.of(
                barcodes[0], 2, barcodes[1], 3));
        String file2 = "aFolder/BROAD_" + packageId + ".csv";
        googleBucketDao.upload(file2, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(StringUtils.join(barcodes, " "));
        pkgBean.setRackCount(String.valueOf(barcodes.length));
        messageCollection.clearAll();
        mayoManifestEjb.packageReceiptValidation(pkgBean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getWarnings().contains(
                String.format(MayoManifestEjb.MULTIPLE_FILES, packageId, StringUtils.joinWith(" ", file1, file2))),
                StringUtils.join(messageCollection.getWarnings(), "; "));
        Assert.assertTrue(messageCollection.getWarnings().contains(MayoManifestEjb.NEEDS_MANUAL_LINKING),
                StringUtils.join(messageCollection.getWarnings(), "; "));

        pkgBean.setShipmentCondition("Pretty good.");
        pkgBean.setDeliveryMethod("None");
        pkgBean.setTrackingNumber("TRK" + testDigits2);
        mayoManifestEjb.packageReceipt(pkgBean);

        // Checks the manifest session. There should be no manifest records.
        ManifestSession session = manifestSessionDao.getSessionByPrefix(packageId);
        Assert.assertNotNull(session);
        Assert.assertEquals(session.getVesselLabels().stream().sorted().collect(Collectors.joining(" ")),
                StringUtils.join(barcodes, " "));
        Assert.assertTrue(session.getRecords().isEmpty());

        // Links the second file to the received package.
        // The ActionBean should only supply a packageId and filename for the link up operation.

        // It should fail if no filename is given.
        MayoPackageReceiptActionBean pkgBean2 = new MayoPackageReceiptActionBean();
        pkgBean2.setMessageCollection(messageCollection);
        pkgBean2.setPackageBarcode(packageId);
        pkgBean2.setFilename("");
        messageCollection.clearAll();
        mayoManifestEjb.updateManifest(pkgBean2);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.MULTIPLE_FILES, packageId, StringUtils.joinWith(" ", file1, file2))),
                StringUtils.join(messageCollection.getErrors(), "; "));
        // Tries again with the correct filename.
        pkgBean2.setFilename(file2);
        messageCollection.clearAll();
        mayoManifestEjb.updateManifest(pkgBean2);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // The manifestSession should have manifest records.
        ManifestSession session2 = manifestSessionDao.getSessionByPrefix(packageId);
        Assert.assertEquals(session2.getVesselLabels().stream().sorted().collect(Collectors.joining(" ")),
                StringUtils.join(barcodes, " "));
        Assert.assertTrue(CollectionUtils.isNotEmpty(session2.getRecords()));

        // The package should no longer be quarantined.
        Assert.assertNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.PACKAGE, packageId));
    }

    @Test
    public void testReceiptWithoutManifest() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());

        // Makes a spreadsheet of a rack with 4 samples.
        String packageId = "PKG-" + testDigits;
        String[] barcodes = {"Bx-" + testDigits};
        String[] alternativeBarcodes = {"SU-" + testDigits};

        // Receives the package before the manifest is written, using the wrong rack barcode.
        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(StringUtils.join(alternativeBarcodes, " "));
        pkgBean.setRackCount(String.valueOf(alternativeBarcodes.length));
        messageCollection.clearAll();
        mayoManifestEjb.packageReceiptValidation(pkgBean);
        Assert.assertTrue(StringUtils.isBlank(pkgBean.getFilename()));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getWarnings().contains(MayoManifestEjb.NEEDS_MANUAL_LINKING),
                StringUtils.join(messageCollection.getWarnings()));
        // Package receipt page2.
        pkgBean.setShipmentCondition("A-OK.");
        pkgBean.setDeliveryMethod("FedEx");
        pkgBean.setTrackingNumber("TRK" + testDigits);
        messageCollection.clearAll();
        mayoManifestEjb.packageReceipt(pkgBean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getWarnings().contains(
                String.format(MayoManifestEjb.QUARANTINED, pkgBean.getPackageBarcode(), Quarantined.MISSING_MANIFEST)),
                StringUtils.join(messageCollection.getWarnings(), "; "));

        // There should be a manifestSession, linked to an RCT, but with no manifestFile nor manifestRecords.
        final ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        Assert.assertNotNull(manifestSession);
        Assert.assertEquals(manifestSession.getVesselLabels().size(), 1);
        Assert.assertEquals(manifestSession.getVesselLabels().iterator().next(), alternativeBarcodes[0]);
        final String rctTicket = manifestSession.getReceiptTicket();
        Assert.assertTrue(StringUtils.isNotBlank(rctTicket));
        Assert.assertTrue(StringUtils.isNotBlank(pkgBean.getRctUrl()));
        Assert.assertTrue(StringUtils.isBlank(manifestSession.getManifestFilename()));
        Assert.assertTrue(CollectionUtils.isEmpty(manifestSession.getRecords()));

        // The package should have been quarantined for the missing manifest file.
        Quarantined quarantined = quarantinedDao.findItem(ItemSource.MAYO, ItemType.PACKAGE, packageId);
        Assert.assertNotNull(quarantined);
        Assert.assertEquals(quarantined.getReason(), Quarantined.MISSING_MANIFEST);

        // Re-receives the package to fixup the wrong rack barcode.
        pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(StringUtils.join(barcodes, " "));
        pkgBean.setRackCount(String.valueOf(barcodes.length));
        pkgBean.setAllowUpdate(true);
        messageCollection.clearAll();
        mayoManifestEjb.packageReceiptValidation(pkgBean);
        Assert.assertTrue(StringUtils.isBlank(pkgBean.getFilename()));
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getWarnings().contains(MayoManifestEjb.NEEDS_MANUAL_LINKING),
                StringUtils.join(messageCollection.getWarnings()));
        Assert.assertTrue(messageCollection.getWarnings().contains(
                String.format(MayoManifestEjb.QUARANTINE_DELETES, packageId)),
                StringUtils.join(messageCollection.getWarnings()));
        // Package receipt page2.
        pkgBean.setShipmentCondition("A-OK.");
        pkgBean.setDeliveryMethod("FedEx");
        pkgBean.setTrackingNumber("TRK" + testDigits);
        messageCollection.clearAll();
        mayoManifestEjb.packageReceipt(pkgBean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getWarnings().contains(
                String.format(MayoManifestEjb.QUARANTINED, pkgBean.getPackageBarcode(), Quarantined.MISSING_MANIFEST)),
                StringUtils.join(messageCollection.getWarnings(), "; "));
        // The session for the packageId should have the correct vessel and have the same RCT ticket (it gets updated).
        ManifestSession manifestSession2 = manifestSessionDao.getSessionByPrefix(packageId);
        Assert.assertTrue(StringUtils.isNotBlank(pkgBean.getRctUrl()));
        Assert.assertNotNull(manifestSession2);
        Assert.assertEquals(manifestSession2.getVesselLabels().size(), 1);
        Assert.assertEquals(manifestSession2.getVesselLabels().iterator().next(), barcodes[0]);
        Assert.assertEquals(manifestSession2.getReceiptTicket(), rctTicket);
        // There should be no manifest records.
        Assert.assertTrue(CollectionUtils.isEmpty(manifestSession2.getRecords()));

        int sampleCount = 4;
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS",
                ImmutableMap.of(barcodes[0], sampleCount));

        // Fails to accession a rack since there is no manifest.
        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(barcodes[0]);
        bean.setRackScan(makeRackScan(cellGrid, barcodes[0]));
        messageCollection.clearAll();
        mayoManifestEjb.validateForAccessioning(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.NOT_LINKED, packageId)),
                StringUtils.join(messageCollection.getErrors()));
        // There should be no new vessels.
        Assert.assertNull(labVesselDao.findByIdentifier(barcodes[0]));
        Assert.assertNull(labVesselDao.findByIdentifier(bean.getRackScan().values().iterator().next()));

        // Writes the manifest file.
        String filename = packageId + ".csv";
        messageCollection.clearAll();
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        manifestSessionDao.flush();

        // Links the new manifest file to the recieved package.
        // The ActionBean should only supply a packageId and an empty filename for the link up operation.
        MayoPackageReceiptActionBean pkgBean2 = new MayoPackageReceiptActionBean();
        pkgBean2.setMessageCollection(messageCollection);
        pkgBean2.setPackageBarcode(packageId);
        pkgBean2.setFilename("");
        messageCollection.clearAll();
        mayoManifestEjb.updateManifest(pkgBean2);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        ManifestSession manifestSession3 = manifestSessionDao.getSessionByPrefix(packageId);
        // The manifestSession should still have the info put there by the package receipt.
        Assert.assertEquals(manifestSession3.getVesselLabels().stream().sorted().collect(Collectors.joining(" ")),
                StringUtils.join(barcodes, " "));
        Assert.assertEquals(manifestSession3.getReceiptTicket(), rctTicket);
        // The package should no longer be quarantined.
        Assert.assertNull(quarantinedDao.findItem(ItemSource.MAYO, ItemType.PACKAGE, packageId));
        // And there should be manifest records.
        Assert.assertTrue(CollectionUtils.isNotEmpty(manifestSession3.getRecords()));

        // Accessions a rack and validates the entity info matches the manifest file's spreadsheet.
        MayoSampleReceiptActionBean bean2 = new MayoSampleReceiptActionBean();
        bean2.setMessageCollection(messageCollection);
        bean2.setRackBarcode(barcodes[0]);
        bean2.setRackScan(makeRackScan(cellGrid, barcodes[0]));
        messageCollection.clearAll();
        mayoManifestEjb.validateForAccessioning(bean2);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();
        mayoManifestEjb.accession(bean2);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        validateEntities(cellGrid, barcodes[0]);
    }

    @Test
    public void testRackMismatch() {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());
        String packageId = "PKG-" + testDigits;

        // Writes a manifest file with two racks having 1 tube each.
        String[] barcodes = {String.format("Bx-%s%d", testDigits, 1), String.format("Bx-%s%d", testDigits, 2)};
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS", ImmutableMap.of(
                barcodes[0], 1, barcodes[1], 1));
        String filename = "subdir1/subdir2/BROAD_" + packageId + ".csv";
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Receives the package but it contains the wrong barcode. The UI warning is ignored.
        String[] wrongBarcodes = {String.format("SU-%s%d", testDigits, 1), String.format("SU-%s%d", testDigits, 2)};
        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(StringUtils.join(wrongBarcodes, " "));
        pkgBean.setRackCount(String.valueOf(wrongBarcodes.length));
        messageCollection.clearAll();
        mayoManifestEjb.packageReceiptValidation(pkgBean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.hasWarnings());
        // Package receipt page2.
        pkgBean.setShipmentCondition("Unsure.");
        pkgBean.setDeliveryMethod("None");
        pkgBean.setTrackingNumber("TRK" + testDigits);
        mayoManifestEjb.packageReceipt(pkgBean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getWarnings().contains(
                String.format(MayoManifestEjb.NOT_IN_MANIFEST, "rack barcode", StringUtils.join(wrongBarcodes, " "))),
                StringUtils.join(messageCollection.getWarnings(), "; "));
        ManifestSession session = manifestSessionDao.getSessionByPrefix(packageId);
        Assert.assertNotNull(session);
        Assert.assertEquals(session.getVesselLabels().stream().sorted().collect(Collectors.joining(" ")),
                StringUtils.join(wrongBarcodes, " "));

        // Fails to accession the received racks.
        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(wrongBarcodes[0]);
        bean.setRackScan(new LinkedHashMap<>());
        bean.getRackScan().put("B02", "123123123");
        messageCollection.clearAll();
        mayoManifestEjb.validateForAccessioning(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(String.format(MayoManifestEjb.NOT_LINKED, packageId)),
                StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Fails to accession the manifest racks.
        bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(barcodes[0]);
        bean.setRackScan(makeRackScan(cellGrid, barcodes[0]));
        messageCollection.clearAll();
        mayoManifestEjb.validateForAccessioning(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.RACK_NOT_RECEIVED, barcodes[0])),
                StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
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
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS", ImmutableMap.of(rackBarcode, 1));
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

    private void validateManifest(ManifestSession manifestSession, List<List<String>> cellGrid) {
        Set<String> tubeBarcodes = new HashSet<>();
        manifestSession.getRecords().forEach(manifestRecord -> {
            String tubeBarcode = manifestRecord.getMetadataByKey(Metadata.Key.BROAD_2D_BARCODE).getValue();
            tubeBarcodes.add(tubeBarcode);
            Assert.assertTrue(StringUtils.isNotBlank(tubeBarcode));
            List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
            int tubeIndex = headers.indexOf(Header.MATRIX_ID);

            // Finds the spreadsheet row that corresponds with this manifest record.
            List<String> expectedValues = cellGrid.subList(1, cellGrid.size()).stream().
                    filter(row -> tubeBarcode.equals(row.get(tubeIndex))).
                    findFirst().orElse(null);
            Assert.assertNotNull(expectedValues, "No row for tube " + tubeBarcode);
            for (int i = 0; i < expectedValues.size(); ++i) {
                Header header = headers.get(i);
                if (header != null && !header.isIgnored()) {
                    String rowValue = expectedValues.get(i);
                    if (header.hasUnits() && StringUtils.isNotBlank(rowValue)) {
                        rowValue = (new BigDecimal(rowValue)).setScale(2, BigDecimal.ROUND_UNNECESSARY).toPlainString();
                    }
                    Metadata.Key key = header.getMetadataKey();
                    String manifestValue = manifestRecord.getMetadataByKey(key) == null ? "" :
                            manifestRecord.getMetadataByKey(key).getValue();
                    Assert.assertEquals(manifestValue, rowValue, "Tube " + tubeBarcode + " " + key.getDisplayName());
                }
            }
        });
        Assert.assertEquals(tubeBarcodes.size(), cellGrid.size() - 1);
    }

    private void validateEntities(List<List<String>> cellGrid, String rackBarcode) throws Exception {
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int boxIndex = headers.indexOf(Header.BOX_ID);
        int tubeIndex = headers.indexOf(Header.MATRIX_ID);
        int sampleIndex = headers.indexOf(Header.MATRIX_ID);
        int collabSampleIndex = headers.indexOf(Header.BIOBANK_SAMPLE_ID);
        int collabParticipantIndex = headers.indexOf(Header.BIOBANK_ID);
        int sexIndex = headers.indexOf(Header.SEX);
        int materialTypeIndex = headers.indexOf(Header.SAMPLE_TYPE);
        int origMaterialTypeIndex = headers.indexOf(Header.SAMPLE_SOURCE);

        List<BarcodedTube> tubes = new ArrayList<>();
        TubeFormation tubeFormation = null;
        RackOfTubes rack;

        for (List<String> row : cellGrid.subList(1, cellGrid.size())) {
            if (rackBarcode.equals(row.get(boxIndex))) {
                // The tube must exist.
                LabVessel tubeVessel = labVesselDao.findByIdentifier(row.get(tubeIndex));
                Assert.assertTrue(OrmUtil.proxySafeIsInstance(tubeVessel, BarcodedTube.class),
                        "Tube " + row.get(tubeIndex));
                BarcodedTube tube = OrmUtil.proxySafeCast(tubeVessel, BarcodedTube.class);
                tubes.add(tube);
                // Validates the tube's rack.
                if (tubeFormation == null) {
                    // FYI going from rack -> tubeFormation -> tubes doesn't work here. For some reason
                    // rack.getTubeFormations() is empty.
                    Assert.assertEquals(tube.getContainers().size(), 1, "Tube " + tube.getLabel());
                    tubeFormation = OrmUtil.proxySafeCast(tube.getContainers().iterator().next(), TubeFormation.class);
                    // For this to work the tests must not reuse a rack with different tubes in it.
                    Assert.assertEquals(tubeFormation.getRacksOfTubes().size(), 1, "Tube " + tube.getLabel());
                    rack = tubeFormation.getRacksOfTubes().iterator().next();
                    Assert.assertEquals(rack.getLabel(), rackBarcode);
                } else {
                    Assert.assertTrue(tube.getContainers().contains(tubeFormation), "Tube " + tube.getLabel());
                }
                // Validates the tube's sample.
                Assert.assertEquals(tube.getMercurySamples().size(), 1);
                MercurySample sample = tube.getMercurySamples().iterator().next();
                Assert.assertEquals(sample.getSampleKey(), row.get(sampleIndex));

                // Validates sample metadata.
                SampleData sampleData = sample.getSampleData();
                Assert.assertNotNull(sampleData, "For sample " + sample.getSampleKey());
                Assert.assertEquals(sampleData.getCollaboratorsSampleName(), row.get(collabSampleIndex),
                        "For sample " + sample.getSampleKey());
                Assert.assertEquals(sampleData.getCollaboratorParticipantId(), row.get(collabParticipantIndex),
                        "For sample " + sample.getSampleKey());
                Assert.assertEquals(sampleData.getGender(), row.get(sexIndex),
                        "For sample " + sample.getSampleKey());
                Assert.assertEquals(sampleData.getMaterialType(), row.get(materialTypeIndex),
                        "For sample " + sample.getSampleKey());
                Assert.assertEquals(sampleData.getOriginalMaterialType(), row.get(origMaterialTypeIndex),
                        "For sample " + sample.getSampleKey());
            }
        }
        // All tubes in the rack should have been validated.
        Assert.assertNotNull(tubeFormation.getContainerRole());
        Assert.assertEquals(tubes.size(), tubeFormation.getContainerRole().getContainedVessels().size(),
                "Rack " + rackBarcode);

        // Validates the RCT ticket.
        ManifestSession manifestSession = manifestSessionDao.getSessionByVesselLabel(rackBarcode);
        Assert.assertNotNull(manifestSession);
        String ticketName = manifestSession.getReceiptTicket();
        Assert.assertTrue(StringUtils.isNotBlank(ticketName));
        String packageId = manifestSession.getSessionPrefix();

        JiraIssue jiraIssue = jiraService.getIssue(ticketName);
        Assert.assertNotNull(jiraIssue);
        Assert.assertEquals(jiraIssue.getStatus(), "Received");
        Map<String, String> jiraFields = extractJiraFields(jiraService, jiraIssue, ticketName);
        Assert.assertEquals(jiraIssue.getSummary(), packageId);

        Assert.assertTrue(StringUtils.isNotBlank(jiraFields.get("ShipmentCondition")), " Ticket " + ticketName);
        Assert.assertTrue(jiraFields.get("TrackingNumber").startsWith("TRK"), " Ticket " + ticketName);
        String deliveryMethod = jiraFields.get("KitDeliveryMethod");
        Assert.assertTrue(deliveryMethod == null || deliveryMethod.equals("FedEx") ||
                        deliveryMethod.equals("Local Courier"), " Ticket " + ticketName);
        Assert.assertTrue(jiraFields.get("Racks").contains(rackBarcode));
    }

    /** Extracts the Jira ticket field values and maps them by their JIRA_DEFINITION_MAP.key (not customfield_nnn) */
    private Map<String, String> extractJiraFields(JiraService jiraService, JiraIssue jiraIssue,
            String ticketName) throws IOException {
        Map<String, String> jiraFields = new HashMap<>();
        for (Map.Entry<String, Object> mapEntry :
                jiraService.getIssueFields(jiraIssue.getKey(), MayoManifestEjb.JIRA_DEFINITION_MAP.values()).
                        getFields().entrySet()) {
            String value = null;
            if (mapEntry.getValue() == null) {
                value = null;
            } else if (mapEntry.getValue() instanceof String) {
                value = (String) mapEntry.getValue();
            } else if (mapEntry.getValue() instanceof Map && ((Map) mapEntry.getValue()).containsKey("value")) {
                value = ((Map<String, String>) mapEntry.getValue()).get("value");
            } else if (mapEntry.getValue() instanceof List && ((List) mapEntry.getValue()).get(0) instanceof Map &&
                    ((Map) ((List) mapEntry.getValue()).get(0)).containsKey("value")) {
                // Collects all values from the maps found in the list.
                value = (String) ((List) mapEntry.getValue()).stream().
                        map(item -> ((Map) item).get("value")).
                        collect(Collectors.joining(", "));
            } else {
                Assert.fail("Ticket " + ticketName + " " + mapEntry.getKey() + " is unparsable.");
            }
            jiraFields.put(REVERSE_JIRA_DEFINITION_MAP.get(mapEntry.getKey()), value);
        }
        return jiraFields;
    }

    private byte[] makeContent (List<List<String>> cellGrid) {
        return cellGrid.stream().
                map(list -> String.join(",", list)).
                collect(Collectors.joining("\n")).
                getBytes();
    }

    private List<List<String>> makeCellGrid(String testSuffix, String packageId, String csIdPrefix,
            Map<String, Integer> rackBarcodeToNumberTubes) {
        List<List<String>> cellGrid = new ArrayList<>();
        cellGrid.add(Arrays.asList(
                "PackageId", "BoxId", "MatrixId", "Well_Position", "SampleId", "Parent_SampleId",
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
                        packageId, rackBarcode, "T" + iSuffix, position, csIdPrefix + "-" + iSuffix,
                        "PS-" + iSuffix, "BiS-" + iSuffix, "02/03/2019", "Bi-" + iSuffix, (i % 2 == 0 ? "M":"F"),
                        "22", "DNA", "", String.valueOf(100 - tubeIndex), (100 + tubeIndex) + ".13",
                        "333", "Followup", "Whole Blood", "StudyXYZ", "Tk-" + testSuffix,
                        "Minnie Me", "mm@none.org", "Dr Evil", "All Tests", (i %2 == 0 ? "Y":"N")));
            }
        }
        return cellGrid;
    }

    /** Returns a map of tube position to tube barcodes relevant to the given rack. */
    private LinkedHashMap<String, String> makeRackScan(List<List<String>> cellGrid, String rackBarcode) {
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int rackIndex = headers.indexOf(Header.BOX_ID);
        int tubeIndex = headers.indexOf(Header.MATRIX_ID);
        int positionIndex = headers.indexOf(Header.WELL_POSITION);
        // Looks at the records that have the given rackBarcode and pulls out the position and tubeBarcode.
        return new LinkedHashMap<>(cellGrid.subList(1, cellGrid.size()).
                stream().
                filter(row -> row.size() > rackIndex && rackBarcode.equals(row.get(rackIndex))).
                collect(Collectors.toMap(row -> row.get(positionIndex), row -> row.get(tubeIndex))));
    }
}
