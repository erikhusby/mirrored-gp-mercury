package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestImportProcessor.Header;

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
    public void testLoadAndReloadManifestFiles() {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());
        String packageId = "PKG-" + testDigits;
        String rackBarcode = "Bx-" + testDigits;

        // Writes a manifest spreadsheet to the storage bucket.
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "A", ImmutableMap.of(rackBarcode, 4));
        String filename = packageId + ".csv";
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Tests Mayo Admin UI "pull all files". Should persist the manifest file just written.
        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Compares manifest records to test data spreadsheet.
        List<ManifestSession> sessions = manifestSessionDao.getSessionsByVesselLabel(rackBarcode);
        Assert.assertEquals(sessions.size(), 1);
        Assert.assertEquals(sessions.iterator().next().getRecords().size(), cellGrid.size() - 1);
        validateManifest(sessions.iterator().next(), cellGrid);

        // Once it's read a file should not be re-read.
        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        Assert.assertEquals(manifestSessionDao.getSessionsByVesselLabel(rackBarcode).size(), 1);

        // Tests Mayo Admin UI "pull one file". A forced reload will make a new manifest session.
        // In this case it will have the same filename and content.
        bean.setFilename(filename);
        mayoManifestEjb.pullOne(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        sessions = manifestSessionDao.getSessionsByVesselLabel(rackBarcode);
        // There should now be another manifest sessions for the rackBarcode.
        Assert.assertEquals(sessions.size(), 2, "For rack " + rackBarcode);
        validateManifest(sessions.iterator().next(), cellGrid);
        long sessionId2 = sessions.iterator().next().getManifestSessionId();

        // Two manifest files with identical content should be treated as two Mercury manifest sessions.
        // In this case it has a different filename but the same content.
        String filename2 = StringUtils.replace(filename, ".csv", "a.csv");
        googleBucketDao.upload(filename2, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        // A lookup by rack barcode should return the latest one, identified by a higher manifestSessionId
        sessions = manifestSessionDao.getSessionsByVesselLabel(rackBarcode);
        Assert.assertEquals(sessions.size(), 3, "For rack " + rackBarcode);
        validateManifest(sessions.iterator().next(), cellGrid);
        long sessionId3 = sessions.iterator().next().getManifestSessionId();
        Assert.assertTrue(sessionId3 > sessionId2, "For rack " + rackBarcode);

        // Makes an identical spreadsheet except for a different collaborator sample id
        // and overwrites an existing storage file.
        // This tests when Mayo folks update a manifest file by writing the same file to the storage bucket.
        List<List<String>> cellGrid2 = makeCellGrid(testDigits, packageId, "B", ImmutableMap.of(rackBarcode, 4));
        googleBucketDao.upload(filename2, makeContent(cellGrid2), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // A force reload is necessary for Mercury to pick up the changes.
        bean.setFilename(filename2);
        mayoManifestEjb.pullOne(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // There should now be another manifest sessions for the rackBarcode.
        sessions = manifestSessionDao.getSessionsByVesselLabel(rackBarcode);
        Assert.assertEquals(sessions.size(), 4, "For rack " + rackBarcode);
        // The head of the list should have the most recent upload.
        validateManifest(sessions.iterator().next(), cellGrid2);

        // Again makes an identical spreadsheet except for a different collaborator sample id
        // and writes it to storage as a new file.
        // This tests when Mayo folks update a manifest file by writing a new file to the storage bucket.
        List<List<String>> cellGrid3 = makeCellGrid(testDigits, packageId, "C", ImmutableMap.of(rackBarcode, 4));
        String filename3 = StringUtils.replace(filename, ".csv", "b.csv");
        googleBucketDao.upload(filename3, makeContent(cellGrid3), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // There should now be another manifest sessions for the rackBarcode.
        sessions = manifestSessionDao.getSessionsByVesselLabel(rackBarcode);
        Assert.assertEquals(sessions.size(), 5, "For rack " + rackBarcode);
        // The head of the list should have the most recent upload.
        validateManifest(sessions.iterator().next(), cellGrid3);
    }

    @Test
    public void testScanValidation() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());
        String packageId = "PKG-" + testDigits;

        // Makes a spreadsheet with 10 racks each having one tube. Writes it to a manifest file.
        List<String> rackBarcodes = IntStream.range(0, 9).mapToObj(i -> String.format("Bx-%s%d", testDigits, i)).
                collect(Collectors.toList());
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS",
                rackBarcodes.stream().collect(Collectors.toMap(Function.identity(), barcode -> 1)));
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int boxIndex = headers.indexOf(Header.BOX_ID);
        int tubeIndex = headers.indexOf(Header.MATRIX_ID);
        int positionIndex = headers.indexOf(Header.WELL_POSITION);

        String filename = packageId + ".csv";
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Receives the package.
        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(StringUtils.join(rackBarcodes, " "));
        pkgBean.setRackCount(String.valueOf(rackBarcodes.size()));
        pkgBean.setShipmentCondition("Pristine.");
        pkgBean.setDeliveryMethod("None");
        pkgBean.setTrackingNumber("TRK" + testDigits);
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(pkgBean);
        mayoManifestEjb.receive(pkgBean);
        Assert.assertNotNull(pkgBean.getManifestSessionId());
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        // Fails to accession when the rack scan has a tube that is not in the manifest.
        // Makes a rack scan from the first row in the manifest and puts two tubes in that rack.
        List<List<String>> rackScanGrid = new ArrayList<>();
        rackScanGrid.add(cellGrid.get(0)); //headers
        rackScanGrid.add(cellGrid.get(1));
        rackScanGrid.add(cellGrid.get(1));
        rackScanGrid.get(2).set(tubeIndex, rackScanGrid.get(2).get(tubeIndex) + "a");
        rackScanGrid.get(2).set(positionIndex, "G10");
        bean.setMessageCollection(messageCollection);
        String rackBarcode = rackScanGrid.get(1).get(boxIndex);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(rackScanGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION,
                "G10", rackScanGrid.get(2).get(tubeIndex),"no tube")),
                StringUtils.join(messageCollection.getErrors()));
        // Checks that the rack was received.
        validateEntities(rackScanGrid, rackBarcode, true);

        // A re-receipt is allowed with the same result, and no change to the entities.
        Long rackId = labVesselDao.findByIdentifier(rackBarcode).getLabVesselId();
        Long tubeId = labVesselDao.findByIdentifier(rackScanGrid.get(1).get(tubeIndex)).getLabVesselId();
        Long tube2Id = labVesselDao.findByIdentifier(rackScanGrid.get(2).get(tubeIndex)).getLabVesselId();
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION,
                "G10", rackScanGrid.get(2).get(tubeIndex),"no tube")),
                StringUtils.join(messageCollection.getErrors()));
        validateEntities(rackScanGrid, rackBarcode, true);
        Assert.assertEquals(labVesselDao.findByIdentifier(rackBarcode).getLabVesselId(), rackId);
        Assert.assertEquals(labVesselDao.findByIdentifier(rackScanGrid.get(1).get(tubeIndex)).getLabVesselId(), tubeId);
        Assert.assertEquals(labVesselDao.findByIdentifier(rackScanGrid.get(2).get(tubeIndex)).getLabVesselId(), tube2Id);

        // Fails to accession when the rack scan is missing a tube that is in the manifest.
        bean.setMessageCollection(messageCollection);
        rackScanGrid = new ArrayList<>();
        rackScanGrid.add(cellGrid.get(0)); //headers
        rackScanGrid.add(cellGrid.get(2));
        String expectedTube = rackScanGrid.get(1).get(tubeIndex);
        rackScanGrid.get(1).set(tubeIndex, "123412341234");
        rackBarcode = rackScanGrid.get(1).get(boxIndex);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(rackScanGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION,
                "A01", "123412341234", expectedTube)),
                StringUtils.join(messageCollection.getErrors()));

        // Fails to accession when the tube is in the wrong rack scan position.
        rackScanGrid = new ArrayList<>();
        rackScanGrid.add(cellGrid.get(0)); //headers
        rackScanGrid.add(cellGrid.get(3));
        rackScanGrid.get(1).set(positionIndex, "B02");
        rackBarcode = rackScanGrid.get(1).get(boxIndex);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(rackScanGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION,
                "A01", "no tube", rackScanGrid.get(1).get(tubeIndex))),
                StringUtils.join(messageCollection.getErrors()));
        Assert.assertTrue(messageCollection.getErrors().contains(String.format(MayoManifestEjb.WRONG_TUBE_IN_POSITION,
                "B02", rackScanGrid.get(1).get(tubeIndex), "no tube")),
                StringUtils.join(messageCollection.getErrors()));

        // Does a successful accession.
        rackScanGrid = new ArrayList<>();
        rackScanGrid.add(cellGrid.get(0)); //headers
        rackScanGrid.add(cellGrid.get(4));
        rackBarcode = rackScanGrid.get(1).get(boxIndex);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(rackScanGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.validateAndScan(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(bean);
        mayoManifestEjb.accession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        validateEntities(cellGrid, rackBarcode, false);

        // A re-accessioning is disallowed by the action bean.
        messageCollection.clearAll();
        mayoManifestEjb.validateAndScan(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.ALREADY_ACCESSIONED, rackScanGrid.get(1).get(tubeIndex))),
                StringUtils.join(messageCollection.getErrors(), "; "));
        // But re-accessioning from the Mayo Admin UI is ok.
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(bean);
        mayoManifestEjb.accession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        validateEntities(cellGrid, rackBarcode, false);

        // Validation should fail when the rack barcode exists and is not for a rack vessel.
        String badRackBarcode = cellGrid.get(1).get(tubeIndex);
        bean.setRackBarcode(badRackBarcode);
        Assert.assertEquals(bean.getRackScan().size(), 1);
        messageCollection.clearAll();
        mayoManifestEjb.validateAndScan(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.NOT_A_RACK, badRackBarcode)),
                StringUtils.join(messageCollection.getErrors(), "; "));

        // Validation should fail when the tube barcode is not for a tube vessel.
        String badTubeBarcode = cellGrid.get(1).get(boxIndex);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(new LinkedHashMap<>());
        bean.getRackScan().put("A01", badTubeBarcode);
        messageCollection.clearAll();
        mayoManifestEjb.validateAndScan(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.NOT_A_TUBE, badTubeBarcode)),
                StringUtils.join(messageCollection.getErrors(), "; "));
    }


    @Test
    public void testAccession() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());

        // Makes a spreadsheet with two racks.
        String packageId = "PKG-" + testDigits;
        String rackBarcodes[] = {"Bx-" + testDigits + "i", "Bx-" + testDigits + "j"};
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS",
                ImmutableMap.of(rackBarcodes[0], 4, rackBarcodes[1], 5));
        // Writes the spreadsheet to the storage bucket.
        String filename = packageId + ".csv";
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        // Receives the package.
        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(StringUtils.join(rackBarcodes, " "));
        pkgBean.setRackCount(String.valueOf(rackBarcodes.length));
        pkgBean.setShipmentCondition("ok");
        pkgBean.setDeliveryMethod("None");
        pkgBean.setTrackingNumber("TRK" + testDigits);
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(pkgBean);
        Assert.assertNotNull(pkgBean.getManifestSessionId());
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();
        mayoManifestEjb.receive(pkgBean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Simulates the user doing accessioning of each rack.
        // Sets the UI fields for the first rack.
        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcodes[0]);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcodes[0]));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(bean);
        mayoManifestEjb.accession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        // Should be in Mercury now as rack, tubes, samples.
        validateEntities(cellGrid, rackBarcodes[0], false);

        // Sets the UI fields for the second rack.
        bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcodes[1]);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcodes[1]));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(bean);
        mayoManifestEjb.accession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        validateEntities(cellGrid, rackBarcodes[1], false);
    }

    @Test
    public void testReceiptThenAccession() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());

        // Makes a spreadsheet with 94 samples.
        String packageId = "PKG-" + testDigits;
        String rackBarcode = "Bx-" + testDigits;
        int sampleCount = 94;
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS",
                ImmutableMap.of(rackBarcode, sampleCount));
        String filename = packageId + ".csv";
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        Assert.assertTrue(CollectionUtils.isEmpty(manifestSessionDao.getSessionsByPrefix(packageId)));
        Assert.assertTrue(CollectionUtils.isEmpty(manifestSessionDao.getSessionsByVesselLabel(rackBarcode)));

        // Rack receipt is done before the package receipt. This represents a lab user mistake.

        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertEquals(findSamplesInRack(bean.getRackBarcode()).size(), 0);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        mayoManifestEjb.lookupManifestSession(bean);
        messageCollection.clearAll();
        mayoManifestEjb.accession(bean);
        // A message about the missing manifest should be given.
        Assert.assertNull(bean.getManifestSessionId());
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getWarnings().contains(
                String.format(MayoManifestEjb.INVALID, "manifest", rackBarcode)));
        validateEntities(cellGrid, rackBarcode, true);

        // Receives the package.
        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(rackBarcode);
        pkgBean.setRackCount("1");
        pkgBean.setShipmentCondition("Acceptable.");
        pkgBean.setDeliveryMethod("FedEx");
        pkgBean.setTrackingNumber("TRK" + testDigits);
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(pkgBean);
        mayoManifestEjb.receive(pkgBean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Accessions the rack.
        bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(bean);
        mayoManifestEjb.accession(bean);
        List<String> samples = findSamplesInRack(bean.getRackBarcode());
        Assert.assertEquals(samples.size(), sampleCount);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        validateEntities(cellGrid, rackBarcode, false);
    }

    private List<String> findSamplesInRack(String rackBarcode) {
        Assert.assertTrue(StringUtils.isNotBlank(rackBarcode));
        LabVessel vessel = labVesselDao.findByIdentifier(rackBarcode);
        Assert.assertTrue(vessel != null && OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class));
        RackOfTubes rack = OrmUtil.proxySafeCast(vessel, RackOfTubes.class);
        Assert.assertTrue(CollectionUtils.isNotEmpty(rack.getTubeFormations()));
        return rack.getTubeFormations().iterator().next().
                getContainerRole().getContainedVessels().stream().
                flatMap(barcodedTube -> barcodedTube.getSampleNames().stream()).
                collect(Collectors.toList());
    }

    @Test
    public void testAccessionFixup() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());

        // Makes a spreadsheet
        String packageId = "PKG-" + testDigits;
        String rackBarcode = "Bx-" + testDigits;
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS", ImmutableMap.of(rackBarcode, 96));
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int tubeIndex = headers.indexOf(Header.MATRIX_ID);
        List<String> tubeBarcodes = cellGrid.subList(1, cellGrid.size()).stream().
                map(row -> row.get(tubeIndex)).
                collect(Collectors.toList());
        // Writes the spreadsheet to the storage bucket.
        String filename = packageId + ".csv";
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Receives the package.
        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(rackBarcode);
        pkgBean.setRackCount("1");
        pkgBean.setShipmentCondition("Looking fine.");
        pkgBean.setDeliveryMethod("FedEx");
        pkgBean.setTrackingNumber("TRK" + testDigits);
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(pkgBean);
        mayoManifestEjb.receive(pkgBean);
        Assert.assertNotNull(pkgBean.getManifestSessionId());
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Accessions the rack.
        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " ; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), " ; "));
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(bean);
        mayoManifestEjb.accession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " ; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), " ; "));
        validateEntities(cellGrid, rackBarcode, false);

        // Does a data fixup by renaming the rack, tubes, and samples with an "X" suffix.
        // This could also be done by deleting them.
        labVesselDao.findByListIdentifiers(tubeBarcodes).
                forEach(tube -> {
                    tube.getMercurySamples().
                            forEach(mercurySample -> mercurySample.setSampleKey(mercurySample.getSampleKey() + "X"));
                    tube.setLabel(tube.getLabel() + "X");
                });
        LabVessel rack = labVesselDao.findByIdentifier(rackBarcode);
        rack.setLabel(rack.getLabel() + "X");
        labVesselDao.flush();

        // Writes out a manifest that has a few tubes removed from the rack.
        int lastRowIndex = cellGrid.size();
        cellGrid.remove(--lastRowIndex);
        cellGrid.remove(--lastRowIndex);
        cellGrid.remove(--lastRowIndex);
        cellGrid.remove(--lastRowIndex);

        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Must explicitly pull the modified file to pick up the manifest changes.
        bean.setFilename(filename);
        messageCollection.clearAll();
        mayoManifestEjb.pullOne(bean);

        // The rack is received and accessioned again.
        bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(bean);
        mayoManifestEjb.accession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        validateEntities(cellGrid, rackBarcode, false);
    }

    @Test
    public void testBucketAccessAndViewFileByFilename() {
        String testDigits = DATE_FORMAT.format(new Date());
        String packageId = "PKG-" + testDigits;
        String rackBarcode = "Bx-" + testDigits;

        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);

        // Make sure bucket has at least one manifest file to display.
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS", ImmutableMap.of(rackBarcode, 1));
        String filename = String.format("test_%s_1.csv", testDigits);
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Tests access and obtains filelist. The list should include the file that was just written.
        messageCollection.clearAll();
        mayoManifestEjb.testAccess(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        Assert.assertTrue(messageCollection.hasInfos());
        Assert.assertTrue(bean.getBucketList().contains(filename));

        // Tests that the file did not fail.
        messageCollection.clearAll();
        mayoManifestEjb.getFailedFiles(bean);
        Assert.assertFalse(bean.getFailedFilesList().contains(filename));

        // Reads the file by its filename.
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

    @Test
    public void testForcedQuarantine() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = DATE_FORMAT.format(new Date());

        // Makes a spreadsheet with 2 samples.
        String packageId = "PKG-" + testDigits;
        String rackBarcode = "Bx-" + testDigits;
        List<List<String>> cellGrid = makeCellGrid(testDigits, packageId, "CS", ImmutableMap.of(rackBarcode, 2));
        String filename = packageId + ".csv";
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        // Receives the package.
        MayoPackageReceiptActionBean pkgBean = new MayoPackageReceiptActionBean();
        pkgBean.setMessageCollection(messageCollection);
        pkgBean.setPackageBarcode(packageId);
        pkgBean.setRackBarcodeString(rackBarcode);
        pkgBean.setRackCount("1");
        pkgBean.setShipmentCondition("Two tubes are cracked and empty.");
        pkgBean.setQuarantineReason("Physical damage");  // This is what makes it quarantined.
        pkgBean.setDeliveryMethod("Local Courier");
        pkgBean.setTrackingNumber("TRK" + testDigits);
        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(pkgBean);
        mayoManifestEjb.receive(pkgBean);
        Assert.assertNotNull(pkgBean.getManifestSessionId());
        Assert.assertEquals(pkgBean.getFilename(), filename);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        // Receives the rack without error but should error when attempting accessioning.
        MayoSampleReceiptActionBean bean = new MayoSampleReceiptActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        messageCollection.clearAll();
        mayoManifestEjb.receive(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));

        messageCollection.clearAll();
        mayoManifestEjb.lookupManifestSession(bean);
        mayoManifestEjb.accession(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(String.format(MayoManifestEjb.INVALID,
                "manifest", packageId)), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        validateEntities(cellGrid, rackBarcode, true);
    }

    private void validateManifest(ManifestSession manifestSession, List<List<String>> cellGrid) {
        manifestSession.getRecords().forEach(manifestRecord -> {
            String tubeBarcode = manifestRecord.getMetadataByKey(Metadata.Key.BROAD_2D_BARCODE).getValue();
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

    }

    private void validateEntities(List<List<String>> cellGrid, String rackBarcode, boolean receiptOnly)
            throws Exception {
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
        List<String> sampleIds = new ArrayList<>();
        List<String> materialTypes = new ArrayList<>();
        TubeFormation tubeFormation = null;
        RackOfTubes rack = null;

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
                if (receiptOnly) {
                    Assert.assertTrue(tube.getMercurySamples().isEmpty(), "Tube " + tube.getLabel() +
                            " should not have a linked MercurySample.");
                } else {
                    // Validates the tube's sample.
                    Assert.assertEquals(tube.getMercurySamples().size(), 1);
                    MercurySample sample = tube.getMercurySamples().iterator().next();
                    Assert.assertEquals(sample.getSampleKey(), row.get(sampleIndex));
                    sampleIds.add(sample.getSampleKey());

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
                    materialTypes.add(sampleData.getMaterialType());
                    Assert.assertEquals(sampleData.getOriginalMaterialType(), row.get(origMaterialTypeIndex),
                            "For sample " + sample.getSampleKey());
                }
            }
        }
        // All tubes in the rack should have been validated.
        Assert.assertNotNull(tubeFormation.getContainerRole());
        Assert.assertEquals(tubes.size(), tubeFormation.getContainerRole().getContainedVessels().size(),
                "Rack " + rackBarcode);

        // Validates the most recent RCT ticket linked to the rack.
        Assert.assertNotNull(rack.getJiraTickets());
        JiraTicket jiraTicket = rack.getJiraTickets().stream().
                filter(t -> t.getTicketName().startsWith(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                max(Comparator.comparing(JiraTicket::getTicketName)).orElse(null);
        Assert.assertNotNull(jiraTicket, "Rack " + rack.getLabel());
        JiraIssue jiraIssue = jiraService.getIssue(jiraTicket.getTicketName());
        Assert.assertNotNull(jiraIssue, "Rack " + rack.getLabel() + " Ticket " + jiraTicket.getTicketName());
        Assert.assertEquals(jiraIssue.getStatus(), receiptOnly ? "Received" : "Accessioned");
        Map<String, String> jiraFields = extractJiraFields(jiraService, jiraIssue, jiraTicket);
        Assert.assertEquals(jiraIssue.getSummary(), rack.getLabel());
        if (receiptOnly) {
            Assert.assertNull(jiraFields.get("Samples"));
        } else {
            String expectedValue = materialTypes.stream().distinct().sorted().
                    map(type -> String.format("%d %s",
                            materialTypes.stream().filter(countedType -> countedType.equals(type)).count(), type)).
                    collect(Collectors.joining(", "));
            Assert.assertEquals(jiraFields.get("MaterialTypeCounts"), expectedValue,
                    " Ticket " + jiraTicket.getTicketName());
            Assert.assertEquals(jiraFields.get("Samples"), sampleIds.stream().sorted().collect(Collectors.joining(" ")),
                    " Ticket " + jiraTicket.getTicketName());
            Assert.assertTrue(StringUtils.isNotBlank(jiraFields.get("ShipmentCondition")),
                    " Ticket " + jiraTicket.getTicketName());
            Assert.assertTrue(jiraFields.get("TrackingNumber").startsWith("Tk-"),
                    " Ticket " + jiraTicket.getTicketName());
            Assert.assertTrue(jiraFields.get("RequestingPhysician").startsWith("Dr "),
                    " Ticket " + jiraTicket.getTicketName());
            String deliveryMethod = jiraFields.get("KitDeliveryMethod");
            Assert.assertTrue(deliveryMethod == null || deliveryMethod.equals("FedEx") ||
                            deliveryMethod.equals("Local Courier"),
                    " Ticket " + jiraTicket.getTicketName() + " KitDeliveryMethod " + deliveryMethod);
            String receiptType = jiraFields.get("ReceiptType");
            Assert.assertTrue(receiptType == null || (receiptType.startsWith("Clinical ") &&
                            (receiptType.endsWith("Genomes") || receiptType.endsWith("Exomes"))),
                    " Ticket " + jiraTicket.getTicketName() + " ReceiptType " + receiptType);
        }
    }

    /** Extracts the Jira ticket field values and maps them by their JIRA_DEFINITION_MAP.key (not customfield_nnn) */
    private Map<String, String> extractJiraFields(JiraService jiraService, JiraIssue jiraIssue,
            JiraTicket jiraTicket) throws IOException {
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
                Assert.fail("Ticket " + jiraTicket.getTicketName() + " " + mapEntry.getKey() + " is unparsable.");
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
        return new LinkedHashMap<>(cellGrid.subList(1, cellGrid.size()).stream().
                filter(row -> row.size() > rackIndex && rackBarcode.equals(row.get(rackIndex))).
                collect(Collectors.toMap(row -> row.get(positionIndex), row -> row.get(tubeIndex))));
    }
}
