package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
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
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestImportProcessor.Header;

@Test(groups = TestGroups.STANDARD)
public class MayoManifestEjbTest extends Arquillian {
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
    public void testLoadAndReloadManifestFiles() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = String.format("%09d", random.nextInt(10000000));

        // Makes a spreadsheet consisting of mostly random value test data.
        List<List<String>> cellGrid = makeCellGrid(testDigits, ImmutableMap.of("Bx-" + testDigits, 4));
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int boxIndex = headers.indexOf(Header.BOX_ID);
        int collabSampleIndex = headers.indexOf(Header.COLLABORATOR_SAMPLE_ID);
        String rackBarcode = cellGrid.get(1).get(boxIndex);

        // Writes the spreadsheet to the storage bucket.
        String filename = String.format("test_%s.csv", testDigits);
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        // Tests Mayo Admin UI "pull all files". Should persist the manifest file just written.
        MayoReceivingActionBean bean = new MayoReceivingActionBean();
        bean.setMessageCollection(messageCollection);
        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        // Compares manifest records to test data spreadsheet.
        String manifestKey = MayoReceivingActionBean.getManifestKey(rackBarcode);
        List<ManifestSession> sessions = manifestSessionDao.getSessionsByPrefix(manifestKey);
        Assert.assertEquals(sessions.size(), 1);
        Assert.assertEquals(sessions.iterator().next().getRecords().size(), cellGrid.size() - 1);
        validateManifest(sessions.iterator().next(), cellGrid);

        // Once it's read a file should not be re-read.
        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertEquals(manifestSessionDao.getSessionsByPrefix(manifestKey).size(), 1);

        // Tests Mayo Admin UI "pull one file". A forced reload will make a new manifest session.
        // In this case it will have identical content.
        bean.setFilename(filename);
        mayoManifestEjb.pullOne(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        sessions = manifestSessionDao.getSessionsByPrefix(MayoReceivingActionBean.getManifestKey(rackBarcode));
        // There should now be another manifest sessions for the rackBarcode.
        Assert.assertEquals(sessions.size(), 2, "For rack " + rackBarcode);
        validateManifest(sessions.iterator().next(), cellGrid);
        long sessionId2 = sessions.iterator().next().getManifestSessionId();

        // Two manifest files with identical content should be treated as two Mercury manifest sessions.
        String filename2 = StringUtils.replace(filename, ".csv", "a.csv");
        googleBucketDao.upload(filename2, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        // A lookup by rack barcode should return the latest one, identified by a higher manifestSessionId
        sessions = manifestSessionDao.getSessionsByPrefix(MayoReceivingActionBean.getManifestKey(rackBarcode));
        Assert.assertEquals(sessions.size(), 3, "For rack " + rackBarcode);
        validateManifest(sessions.iterator().next(), cellGrid);
        long sessionId3 = sessions.iterator().next().getManifestSessionId();
        Assert.assertTrue(sessionId3 > sessionId2, "For rack " + rackBarcode);

        // Modifies some sample metadata in the spreadsheet and re-uploads it.
        // This tests if Mayo updates a manifest file by replacing it in the storage bucket.
        cellGrid.forEach(row -> {
            for (int i = 0; i < row.size(); ++i) {
                String value = row.get(i);
                if (value.startsWith("Bi")) {
                    row.set(collabSampleIndex, value.replaceFirst("Bi", "VERS"));
                }
            }
        });
        googleBucketDao.upload(filename2, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        // A force reload is necessary for Mercury to pick up the changes.
        bean.setFilename(filename);
        mayoManifestEjb.pullOne(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        // There should now be another manifest sessions for the rackBarcode.
        sessions = manifestSessionDao.getSessionsByPrefix(MayoReceivingActionBean.getManifestKey(rackBarcode));
        Assert.assertEquals(sessions.size(), 4, "For rack " + rackBarcode);
        // The head of the list should have the most recent upload.
        validateManifest(sessions.iterator().next(), cellGrid);

        // Again modifies the sample metadata in the spreadsheet and re-uploads it as a new file.
        // This tests if Mayo updates a manifest file by writing a new file to the storage bucket.
        cellGrid.forEach(row -> {
            for (int i = 0; i < row.size(); ++i) {
                String value = row.get(i);
                if (value.startsWith("VERS")) {
                    row.set(collabSampleIndex, value.replaceFirst("VERS", "UPD"));
                }
            }
        });
        String filename3 = StringUtils.replace(filename, ".csv", "b.csv");
        googleBucketDao.upload(filename3, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        mayoManifestEjb.pullAll(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        // There should now be another manifest sessions for the rackBarcode.
        sessions = manifestSessionDao.getSessionsByPrefix(MayoReceivingActionBean.getManifestKey(rackBarcode));
        Assert.assertEquals(sessions.size(), 5, "For rack " + rackBarcode);
        // The head of the list should have the most recent upload.
        validateManifest(sessions.iterator().next(), cellGrid);
    }

    @Test
    public void testScanValidation() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = String.format("%09d", random.nextInt(100000000));

        // Makes a spreadsheet with 10 racks each having one tube. Writes it to a manifest file.
        List<List<String>> cellGrid = makeCellGrid(testDigits,
                IntStream.range(0, 9).mapToObj(i -> String.format("Bx-%s%d", testDigits, i)).
                        collect(Collectors.toMap(Function.identity(), barcode -> 1)));
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int boxIndex = headers.indexOf(Header.BOX_ID);
        int tubeIndex = headers.indexOf(Header.MATRIX_ID);
        int positionIndex = headers.indexOf(Header.WELL_LOCATION);
        String[] rackBarcode = cellGrid.subList(1, cellGrid.size()).stream().map(row -> row.get(boxIndex)).
                collect(Collectors.toList()).toArray(new String[0]);
        String[] tubeBarcode = cellGrid.subList(1, cellGrid.size()).stream().map(row -> row.get(tubeIndex)).
                collect(Collectors.toList()).toArray(new String[0]);
        String[] tubePosition = cellGrid.subList(1, cellGrid.size()).stream().map(row -> row.get(positionIndex)).
                collect(Collectors.toList()).toArray(new String[0]);
        String filename = String.format("test_%s.csv", testDigits);
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        MayoReceivingActionBean bean = new MayoReceivingActionBean();
        int testIdx = 0;
        // Fails to accession when the rack scan has a tube that is not in the manifest.
        // Fudged cellGrid puts two tubes in the rack scan.
        List<List<String>> fudgedGrid = new ArrayList<>(cellGrid);
        int fudgedRowIdx = testIdx + 2;
        fudgedGrid.get(fudgedRowIdx).set(boxIndex, rackBarcode[testIdx]);
        fudgedGrid.get(fudgedRowIdx).set(positionIndex, "G10");
        messageCollection.clearAll();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode[testIdx]);
        bean.setRackScan(makeRackScan(fudgedGrid, rackBarcode[testIdx]));
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.TUBE_NOT_IN_MANIFEST, tubeBarcode[testIdx + 1])),
                StringUtils.join(messageCollection.getErrors()));
        // Checks that the rack receipt worked (but not accessioned).
        validateEntities(fudgedGrid, rackBarcode[testIdx], true);
        Long rackId = labVesselDao.findByIdentifier(rackBarcode[testIdx]).getLabVesselId();
        Long tubeId = labVesselDao.findByIdentifier(tubeBarcode[testIdx]).getLabVesselId();
        Long tube2Id = labVesselDao.findByIdentifier(tubeBarcode[testIdx + 1]).getLabVesselId();

        // A re-receipt is allowed with the same result, and no change to the entities.
        messageCollection.clearAll();
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertTrue(messageCollection.getErrors().size() == 1 && messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.TUBE_NOT_IN_MANIFEST, tubeBarcode[testIdx + 1])),
                StringUtils.join(messageCollection.getErrors()));
        validateEntities(fudgedGrid, rackBarcode[testIdx], true);
        Assert.assertEquals(labVesselDao.findByIdentifier(rackBarcode[testIdx]).getLabVesselId(), rackId);
        Assert.assertEquals(labVesselDao.findByIdentifier(tubeBarcode[testIdx]).getLabVesselId(), tubeId);
        Assert.assertEquals(labVesselDao.findByIdentifier(tubeBarcode[testIdx + 1]).getLabVesselId(), tube2Id);
        testIdx += 2;

        // Fails to accession when the rack scan is missing a tube that is in the manifest.
        messageCollection.clearAll();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode[testIdx]);
        bean.setRackScan(new LinkedHashMap<>());
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.TUBE_NOT_IN_RACKSCAN, tubeBarcode[testIdx])),
                StringUtils.join(messageCollection.getErrors()));
        ++testIdx;

        // Fails to accession when the tube is in the wrong rack scan position.
        messageCollection.clearAll();
        bean.setRackBarcode(rackBarcode[testIdx]);
        bean.setRackScan(new LinkedHashMap<>());
        bean.getRackScan().put("B01", tubeBarcode[testIdx]);
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.WRONG_POSITION, tubeBarcode[testIdx], "A01", "B01")),
                StringUtils.join(messageCollection.getErrors()));
        ++testIdx;

        // Does a successful accession.
        messageCollection.clearAll();
        bean.setRackBarcode(rackBarcode[testIdx]);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode[testIdx]));
        mayoManifestEjb.validateAndScan(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        validateEntities(cellGrid, rackBarcode[testIdx], false);

        // A re-accessioning is disallowed by the action bean.
        mayoManifestEjb.validateAndScan(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.ALREADY_ACCESSIONED, tubeBarcode[testIdx])),
                StringUtils.join(messageCollection.getErrors()));
        // But re-accessioning from the Mayo Admin UI is ok.
        messageCollection.clearAll();
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        validateEntities(cellGrid, rackBarcode[testIdx], false);
        ++testIdx;

        // Validation should fail when the rack barcode exists and is not for a rack vessel.
        String badRackBarcode = tubeBarcode[0];
        messageCollection.clearAll();
        bean.setRackBarcode(badRackBarcode);
        bean.setRackScan(new LinkedHashMap<>());
        bean.getRackScan().put(tubePosition[testIdx], tubeBarcode[testIdx]);
        mayoManifestEjb.validateAndScan(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.NOT_A_RACK, badRackBarcode)),
                StringUtils.join(messageCollection.getErrors()));
        ++testIdx;

        // Validation should fail when the tube barcode is not for a tube vessel.
        String badTubeBarcode = rackBarcode[0];
        messageCollection.clearAll();
        bean.setRackBarcode(rackBarcode[testIdx]);
        bean.setRackScan(new LinkedHashMap<>());
        bean.getRackScan().put(tubePosition[testIdx], badTubeBarcode);
        mayoManifestEjb.validateAndScan(bean);
        Assert.assertTrue(messageCollection.getErrors().contains(
                String.format(MayoManifestEjb.NOT_A_TUBE, badTubeBarcode)),
                StringUtils.join(messageCollection.getErrors()));
    }


    @Test
    public void testAccession() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = String.format("%09d", random.nextInt(10000000));
        // Makes a spreadsheet with two racks.
        String rackBarcodes[] = {"Bx-" + testDigits + "i", "Bx-" + testDigits + "j"};
        List<List<String>> cellGrid = makeCellGrid(testDigits, ImmutableMap.of(rackBarcodes[0], 4, rackBarcodes[1], 5));
        // Writes the spreadsheet to the storage bucket.
        String filename = String.format("test_%s_ij.csv", testDigits);
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        // Simulates the user doing accessioning of each rack.
        // Sets the UI fields for the first rack.
        MayoReceivingActionBean bean = new MayoReceivingActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcodes[0]);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcodes[0]));
        bean.setShipmentCondition("Two broken tubes.");
        bean.setDeliveryMethod("None");
        bean.setReceiptType("None");
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        // Should be in Mercury now as rack, tubes, samples.
        validateEntities(cellGrid, rackBarcodes[0], false);

        // Sets the UI fields for the second rack.
        bean = new MayoReceivingActionBean();
        messageCollection.clearAll();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcodes[1]);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcodes[1]));
        bean.setShipmentCondition("A-OK.");
        bean.setDeliveryMethod("Local Courier");
        bean.setReceiptType("Clinical Exomes, Clinical Genomes");
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        validateEntities(cellGrid, rackBarcodes[1], false);
    }

    @Test
    public void testReceiptThenAccession() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = String.format("%09d", random.nextInt(10000000));
        // Makes a spreadsheet with 94 samples.
        List<List<String>> cellGrid = makeCellGrid(testDigits, ImmutableMap.of("Bx-" + testDigits, 94));
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int boxIndex = headers.indexOf(Header.BOX_ID);
        String rackBarcode = cellGrid.get(1).get(boxIndex);

        // The manifest file has not yet been written. An attempt to accession the rack should
        // cause it to be only received, with no MercurySamples made.
        MayoReceivingActionBean bean = new MayoReceivingActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        bean.setShipmentCondition("Crushed carton.");
        bean.setDeliveryMethod("Local Courier");
        bean.setReceiptType("None");
        mayoManifestEjb.receiveAndAccession(bean);
        // No errors, but a warning about the missing manifest should be given.
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        Assert.assertTrue(messageCollection.getWarnings().contains(String.format(MayoManifestEjb.INVALID,
                "manifest", rackBarcode)));
        messageCollection.clearAll();
        validateEntities(cellGrid, rackBarcode, true);

        // Writes the manifest file to storage bucket.
        String filename = String.format("test_%s_96.csv", testDigits);
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Accessions the rack.
        bean = new MayoReceivingActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        bean.setShipmentCondition("Missing paperwork but otherwise ok.");
        bean.setDeliveryMethod("FedEx");
        bean.setReceiptType("Clinical Genomes");
        bean.setShippingAcknowledgement("I'm at a loss for words here.");
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        validateEntities(cellGrid, rackBarcode, false);
    }

    @Test
    public void testAccessionFixup() throws Exception {
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        String testDigits = String.format("%09d", random.nextInt(10000000));
        // Makes a spreadsheet
        List<List<String>> cellGrid = makeCellGrid(testDigits, ImmutableMap.of("Bx-" + testDigits, 96));
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int boxIndex = headers.indexOf(Header.BOX_ID);
        int tubeIndex = headers.indexOf(Header.MATRIX_ID);
        String rackBarcode = cellGrid.get(1).get(boxIndex);
        List<String> tubeBarcodes = cellGrid.subList(1, cellGrid.size()).stream().
                map(row -> row.get(tubeIndex)).
                collect(Collectors.toList());

        String filename = String.format("test_%s_96.csv", testDigits);
        // Writes the spreadsheet to the storage bucket.
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Accessions the rack.
        MayoReceivingActionBean bean = new MayoReceivingActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        bean.setShipmentCondition("Looking fine.");
        bean.setDeliveryMethod("FedEx");
        bean.setReceiptType("Clinical Exomes");
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        messageCollection.clearAll();
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
        String filename1 = StringUtils.replace(filename, ".csv", "c.csv");
        googleBucketDao.upload(filename1, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Must explicitly pull all manifests to get the new one into a manifest session.
        // Alternatively, the old manifest could have its manifest key suffixed with an X as part of the fixup.
        bean.setFilename("");
        mayoManifestEjb.pullAll(bean);

        // The rack is accessioned.
        bean = new MayoReceivingActionBean();
        bean.setMessageCollection(messageCollection);
        bean.setRackBarcode(rackBarcode);
        bean.setRackScan(makeRackScan(cellGrid, rackBarcode));
        // If an attempt is made to do a reaccession (i.e. from the Mayo Admin UI) it should fail because the
        // rack would not be found due to the fixup. Plus the reaccession UI doesn't have all of the RCT fields.
        mayoManifestEjb.reaccession(bean);
        Assert.assertTrue(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();
        // A normal accession should work.
        bean.setShipmentCondition("Missing paper manifest, otherwise ok.");
        bean.setDeliveryMethod("FedEx");
        bean.setReceiptType("Clinical Exomes");
        bean.setShippingAcknowledgement("I acknowledge the receipt.");
        mayoManifestEjb.receiveAndAccession(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();
        validateEntities(cellGrid, rackBarcode, false);
    }

    @Test
    public void testBucketAccessAndViewFileByFilename() {
        String testDigits = String.format("%09d", random.nextInt(10000000));
        mayoManifestEjb.getUserBean().loginTestUser();
        MessageCollection messageCollection = new MessageCollection();
        MayoReceivingActionBean bean = new MayoReceivingActionBean();
        bean.setMessageCollection(messageCollection);

        // Make sure bucket has at least one manifest file to display.
        List<List<String>> cellGrid = makeCellGrid(testDigits, ImmutableMap.of("Bx-" + testDigits, 1));
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int boxIndex = headers.indexOf(Header.BOX_ID);
        int tubeIndex = headers.indexOf(Header.MATRIX_ID);
        String filename = String.format("test_%s_1.csv", testDigits);
        googleBucketDao.upload(filename, makeContent(cellGrid), messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        messageCollection.clearAll();

        // Tests access and obtains filelist. The list should include the file that was just written.
        mayoManifestEjb.testAccess(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.hasInfos());
        Assert.assertTrue(bean.getBucketList().contains(filename));

        // Reads the file by its filename.
        messageCollection.clearAll();
        bean.setFilename(filename);
        bean.getManifestCellGrid().clear();
        mayoManifestEjb.readManifestFileCellGrid(bean);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertFalse(messageCollection.hasWarnings(), StringUtils.join(messageCollection.getWarnings(), "; "));
        List<List<String>> displayCellGrid = bean.getManifestCellGrid();
        Assert.assertEquals(displayCellGrid.stream().flatMap(list -> list.stream()).collect(Collectors.joining(" ")),
                cellGrid.stream().flatMap(list -> list.stream()).collect(Collectors.joining(" ")));
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
                if (header != null) {
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
        int sampleIndex = headers.indexOf(Header.BROAD_SAMPLE_ID);
        int collabSampleIndex = headers.indexOf(Header.COLLABORATOR_SAMPLE_ID);
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
                    Assert.assertTrue(tube.getMercurySamples().isEmpty(), "Tube " + tube.getLabel());
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
        Assert.assertEquals(tubes.size(), tubeFormation.getContainerRole().getContainedVessels().size(),
                "Rack " + rackBarcode);

        // Validates the most recent RCT ticket linked to the rack.
        JiraTicket jiraTicket = rack.getJiraTickets().stream().
                filter(t -> t.getTicketName().startsWith(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                sorted(Comparator.comparing(JiraTicket::getTicketName).reversed()).
                findFirst().orElse(null);
        Assert.assertNotNull(jiraTicket, "Rack " + rack.getLabel());
        JiraIssue jiraIssue = jiraService.getIssue(jiraTicket.getTicketId());
        Assert.assertNotNull(jiraIssue, "Rack " + rack.getLabel() + " Ticket " + jiraTicket.getTicketId());
        Assert.assertEquals(jiraIssue.getStatus(), receiptOnly ? "Received" : "Accessioned");
        Map<String, String> jiraFields = extractJiraFields(jiraService, jiraIssue, jiraTicket);
        if (receiptOnly) {
            Assert.assertEquals(jiraIssue.getSummary(),
                    String.format(MayoManifestEjb.QUARANTINED_RCT_TITLE, rack.getLabel()));
            Assert.assertNull(jiraFields.get("Samples"));
        } else {
            Assert.assertEquals(jiraIssue.getSummary(), String.format(MayoManifestEjb.RCT_TITLE, rack.getLabel()));
            String expectedValue = materialTypes.stream().distinct().sorted().
                    map(type -> String.format("%d %s",
                            materialTypes.stream().filter(countedType -> countedType.equals(type)).count(), type)).
                    collect(Collectors.joining(", "));
            Assert.assertEquals(jiraFields.get("MaterialTypeCounts"), expectedValue,
                    " Ticket " + jiraTicket.getTicketId());
            Assert.assertEquals(jiraFields.get("Samples"), sampleIds.stream().sorted().collect(Collectors.joining(" ")),
                    " Ticket " + jiraTicket.getTicketId());
            Assert.assertTrue(StringUtils.isNotBlank(jiraFields.get("ShipmentCondition")),
                    " Ticket " + jiraTicket.getTicketId());
            Assert.assertTrue(jiraFields.get("TrackingNumber").startsWith("Tk-"),
                    " Ticket " + jiraTicket.getTicketId());
            Assert.assertTrue(jiraFields.get("RequestingPhysician").startsWith("Dr "),
                    " Ticket " + jiraTicket.getTicketId());
            String deliveryMethod = jiraFields.get("KitDeliveryMethod");
            Assert.assertTrue(deliveryMethod == null || deliveryMethod.equals("FedEx") ||
                    deliveryMethod.equals("Local Courier"),
                    " Ticket " + jiraTicket.getTicketId() + " KitDeliveryMethod " + deliveryMethod);
            String receiptType = jiraFields.get("ReceiptType");
            Assert.assertTrue(receiptType == null || (receiptType.startsWith("Clinical ") &&
                    (receiptType.endsWith("Genomes") || receiptType.endsWith("Exomes"))),
                    " Ticket " + jiraTicket.getTicketId() + " ReceiptType " + receiptType);
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
                Assert.fail("Ticket " + jiraTicket.getTicketId() + " " + mapEntry.getKey() + " is unparsable.");
            }
            jiraFields.put(MayoManifestEjb.REVERSE_JIRA_DEFINITION_MAP.get(mapEntry.getKey()), value);
        }
        return jiraFields;
    }

    private byte[] makeContent (List<List<String>> cellGrid) {
        return cellGrid.stream().
                map(list -> list.stream().collect(Collectors.joining(","))).
                collect(Collectors.joining("\n")).
                getBytes();
    }

    private List<List<String>> makeCellGrid(String testSuffix, Map<String, Integer> rackBarcodeToNumberTubes) {
        List<List<String>> cellGrid = new ArrayList<>();
        cellGrid.add(Arrays.asList(
                "Package Id", "Box Label", "Matrix Id", "Well Position", "Sample Id", "Parent Sample Id",
                "Biobankid_Sampleid", "Collection Date", "Biobank Id", "Sex At Birth",
                "Age", "Sample Type", "Treatments", "Quantity (ul)", "Total Concentration (ng/ul)",
                "Total Dna(ng)", "Visit Description", "Sample Source", "Study", "Tracking Number",
                "Contact", "Email", "Requesting Physician", "Test Name"));

        int tubeIndex = 0;
        for (String rackBarcode : rackBarcodeToNumberTubes.keySet()) {
            for (int i = 0; i < rackBarcodeToNumberTubes.get(rackBarcode); ++i) {
                String iSuffix = String.format("%s%03d", testSuffix, tubeIndex++);
                String position = String.format("%c%02d", "ABCDEFGH".charAt(i / 12), (i % 12) + 1); // A01 thru H12
                cellGrid.add(Arrays.asList(
                        "Pk-" + testSuffix, rackBarcode, "T" + iSuffix, position, "S-" + iSuffix,
                        "PS-" + iSuffix, "BiS-" + iSuffix, "02/03/2019", "Bi-" + iSuffix, (i % 2 == 0 ? "M" : "F"),
                        "22", "DNA", "", String.valueOf(100 - tubeIndex), String.valueOf(100 + tubeIndex) + ".13",
                        "333", "Followup", "Whole Blood", "StudyXYZ", "Tk-" + testSuffix,
                        "Minnie Me", "mm@none.org", "Dr Evil", "All Tests"));
            }
        }
        return cellGrid;
    }

    /** Returns a map of tube position to tube barcodes relevant to the given rack. */
    private LinkedHashMap<String, String> makeRackScan(List<List<String>> cellGrid, String rackBarcode) {
        List<Header> headers = MayoManifestImportProcessor.extractHeaders(cellGrid.get(0), null, null);
        int rackIndex = headers.indexOf(Header.BOX_ID);
        int tubeIndex = headers.indexOf(Header.MATRIX_ID);
        int positionIndex = headers.indexOf(Header.WELL_LOCATION);
        return new LinkedHashMap<>(cellGrid.subList(1, cellGrid.size()).stream().
                filter(row -> row.size() > rackIndex && rackBarcode.equals(row.get(rackIndex))).
                collect(Collectors.toMap(row -> row.get(positionIndex), row -> row.get(tubeIndex))));
    }
}
