package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * dbfree unit test of AbandonVessel and AbandonVesselPosition etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ArrayProcessFlowEtlDbFreeTest {

    private String etlDateStr;
    private Date[] eventDates = new Date[6];
    private Long pdoId = 999L;
    private String batchName = "ARRAY-1";
    private String lcsetSampleName = "SM-ALIQUOT";
    private String sampleName = "SM-STOCK";
    private Set<SampleInstanceV2> sampleInstances = new HashSet<>();


    private ArrayProcessFlowEtl arrayProcessFlowEtl;

    private final LabEventDao dao = EasyMock.createMock(LabEventDao.class);

    // Set up sample instance and internal dependencies
    private SampleInstanceV2 sampleInstance = EasyMock.createMock(SampleInstanceV2.class);
    private BucketEntry bucketEntry = EasyMock.createMock(BucketEntry.class);
    private ProductOrder pdo = EasyMock.createMock(ProductOrder.class);
    private LabBatch labBatch = EasyMock.createMock(LabBatch.class);
    // Set up the DNA plate
    private StaticPlate dnaPlate = new StaticPlate("dna_barcode", StaticPlate.PlateType.Plate96Well200PCR, "dna_name");
    private PlateWell dnaWell = EasyMock.createMockBuilder(PlateWell.class).withConstructor(StaticPlate.class,VesselPosition.class).withArgs(dnaPlate,VesselPosition.A01).addMockedMethods("getTransfersTo","getTransfersFrom","getVesselPosition","getSampleInstancesV2").createMock();

    private final Object[] mocks = new Object[]{dao, sampleInstance, bucketEntry, pdo, labBatch, dnaWell };

    public ArrayProcessFlowEtlDbFreeTest(){
        Calendar calendar = Calendar.getInstance();
        etlDateStr = ExtractTransform.formatTimestamp(calendar.getTime());
        for( int i = eventDates.length - 1; i >= 0; i-- ) {
            calendar.add(Calendar.MINUTE, -2);
            eventDates[i] = calendar.getTime();
        }
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        EasyMock.reset(mocks);
        sampleInstances.clear();

        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo).anyTimes();
        EasyMock.expect(pdo.getProductOrderId()).andReturn( pdoId ).anyTimes();
        EasyMock.expect(sampleInstance.getSingleBucketEntry()).andReturn(bucketEntry).anyTimes();
        EasyMock.expect(labBatch.getBatchName()).andReturn(batchName).anyTimes();
        EasyMock.expect(sampleInstance.getSingleBatch()).andReturn(labBatch).anyTimes();
        EasyMock.expect(sampleInstance.getNearestMercurySampleName()).andReturn(lcsetSampleName).anyTimes();
        EasyMock.expect(sampleInstance.getEarliestMercurySampleName()).andReturn(sampleName).anyTimes();
        sampleInstances.add(sampleInstance);

        arrayProcessFlowEtl = new ArrayProcessFlowEtl(dao);
    }

    public void testCantMakeEtlRecords() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, -1L)).andReturn(null);
        EasyMock.replay(mocks);

        Assert.assertEquals(arrayProcessFlowEtl.dataRecords(etlDateStr, false, -1L).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testNotAnInfiniumEvent() throws Exception {
        LabEvent notAnInfiniumEvent = new LabEvent(LabEventType.A_BASE, eventDates[0], "OZ", 1l, 1L, "Python Deck");
        EasyMock.expect(dao.findById(LabEvent.class, 1L)).andReturn(notAnInfiniumEvent);
        EasyMock.replay(mocks);

        Assert.assertEquals(arrayProcessFlowEtl.dataRecords(etlDateStr, false, 1L).size(), 0);
        EasyMock.verify(mocks);
    }

    // Gets downstream and upstream events from an Infinium bucketed DNA plate well
    public void testBucketEvent() throws NoSuchMethodException {

        // Event #1: Plating
        LabEvent dilutionEvent = new LabEvent(LabEventType.ARRAY_PLATING_DILUTION, eventDates[0], "Plating Station", 1L, 1L, "Plating Program");
        Set<LabEvent> dilutionEvents = new HashSet<>();
        dilutionEvents.add(dilutionEvent);
        EasyMock.expect(dnaWell.getTransfersTo()).andReturn(dilutionEvents).anyTimes();
        LabEvent bucketEvent = new LabEvent(LabEventType.INFINIUM_BUCKET, eventDates[1], "BSP", 1L, 1L, "BSP");
        bucketEvent.setInPlaceLabVessel(dnaWell);

        EasyMock.expect(dnaWell.getVesselPosition()).andReturn(VesselPosition.A01).anyTimes();
        EasyMock.expect(dnaWell.getSampleInstancesV2()).andReturn( sampleInstances ).anyTimes();

        // Event #2:  Amplification
        LabEvent ampEvent = new LabEvent(LabEventType.INFINIUM_AMPLIFICATION, eventDates[2], "Amp Station", -1l, 1L, "Amp Program");
        StaticPlate ampPlate = new StaticPlate("amp_barcode", StaticPlate.PlateType.NinetySixDeepWell, "amp_name");
        VesselContainer<PlateWell> ampContainer= new VesselContainer<>(ampPlate);
        SectionTransfer ampXfer = new SectionTransfer(dnaPlate.getContainerRole(), SBSSection.A01, ampPlate, ampContainer, SBSSection.A01, null, ampEvent);
        ampEvent.getSectionTransfers().add(ampXfer);

        EasyMock.expect(dnaWell.getTransfersFrom()).andReturn(Collections.singleton(ampEvent)).anyTimes();

        // Event #3: Resuspension
        LabEvent inPlaceEvent = new LabEvent(LabEventType.INFINIUM_RESUSPENSION, eventDates[3], "Resusp Station", 1l, 1L, "Resusp Program");
        ampPlate.getInPlaceLabEvents().add(inPlaceEvent);
        inPlaceEvent.setInPlaceLabVessel(ampContainer.getEmbedder());

        // Event #4:  Hyb
        LabEvent hybEvent = new LabEvent(LabEventType.INFINIUM_HYBRIDIZATION, eventDates[4], "Hyb Station", -1l, 1L, "Hyb Program");
        StaticPlate hybChip = new StaticPlate("chip_barcode", StaticPlate.PlateType.InfiniumChip24, "chip_name");
        VesselContainer<PlateWell> chipContainer= new VesselContainer<>(hybChip);
        CherryPickTransfer chipXfer = new CherryPickTransfer(ampContainer, VesselPosition.A01, null, chipContainer, VesselPosition.R01C01, null, hybEvent);
        hybEvent.getCherryPickTransfers().add(chipXfer);
        ampPlate.getContainerRole().getCherryPickTransfersFrom().add(chipXfer);

        // Event #5: Xstain
        inPlaceEvent = new LabEvent(LabEventType.INFINIUM_XSTAIN, eventDates[3], "XStain Station", 1l, 1L, "XStain Program");
        hybChip.getInPlaceLabEvents().add(inPlaceEvent);
        inPlaceEvent.setInPlaceLabVessel(chipContainer.getEmbedder());

        EasyMock.expect(dao.findById(LabEvent.class, 1L)).andReturn(bucketEvent);
        EasyMock.replay(mocks);

        Collection<String> records = arrayProcessFlowEtl.dataRecords(etlDateStr, false, 1L);
        Assert.assertEquals(records.size(), 5);

        String[] parts;
        Iterator<String> iter = records.iterator();

        // Plating
        parts = iter.next().split(",",13);
        Assert.assertEquals( parts[0], etlDateStr);
        Assert.assertEquals( parts[1], "F");
        Assert.assertEquals( parts[2], pdoId.toString());
        Assert.assertEquals( parts[3], batchName);
        Assert.assertEquals( parts[4], lcsetSampleName);
        Assert.assertEquals( parts[5], sampleName);
        // Ignore event ID
        Assert.assertEquals( parts[7], LabEventType.ARRAY_PLATING_DILUTION.getName());
        Assert.assertEquals( parts[8], "Plating Station");
        // Ignore event date
        Assert.assertEquals( parts[10], "dna_barcode"+ VesselPosition.A01.toString());
        Assert.assertEquals( parts[11], "dna_name");
        Assert.assertEquals( parts[12], VesselPosition.A01.toString());

        // Amp
        parts = iter.next().split(",",13);
        Assert.assertEquals( parts[2], pdoId.toString());
        Assert.assertEquals( parts[3], batchName);
        Assert.assertEquals( parts[4], lcsetSampleName);
        Assert.assertEquals( parts[5], sampleName);
        // Ignore event ID
        Assert.assertEquals( parts[7], LabEventType.INFINIUM_AMPLIFICATION.getName());
        Assert.assertEquals( parts[8], "Amp Station");
        // Ignore event date
        Assert.assertEquals( parts[10], "amp_barcode");
        Assert.assertEquals( parts[11], "amp_name");
        Assert.assertEquals( parts[12], VesselPosition.A01.toString());

        // In place resusp on Amp
        parts = iter.next().split(",",13);
        Assert.assertEquals( parts[2], pdoId.toString());
        Assert.assertEquals( parts[3], batchName);
        Assert.assertEquals( parts[4], lcsetSampleName);
        Assert.assertEquals( parts[5], sampleName);
        // Ignore event ID
        Assert.assertEquals( parts[7], LabEventType.INFINIUM_RESUSPENSION.getName());
        Assert.assertEquals( parts[8], "Resusp Station");
        // Ignore event date
        Assert.assertEquals( parts[10], "amp_barcode");
        Assert.assertEquals( parts[11], "amp_name");
        Assert.assertEquals( parts[12], VesselPosition.A01.toString());

        // Chip hyb
        parts = iter.next().split(",",13);
        Assert.assertEquals( parts[2], pdoId.toString());
        Assert.assertEquals( parts[3], batchName);
        Assert.assertEquals( parts[4], lcsetSampleName);
        Assert.assertEquals( parts[5], sampleName);
        // Ignore event ID
        Assert.assertEquals( parts[7], LabEventType.INFINIUM_HYBRIDIZATION.getName());
        Assert.assertEquals( parts[8], "Hyb Station");
        // Ignore event date
        Assert.assertEquals( parts[10], "chip_barcode");
        Assert.assertEquals( parts[11], "chip_name");
        Assert.assertEquals( parts[12], VesselPosition.R01C01.toString());

        // In place on chip
        parts = iter.next().split(",",13);
        Assert.assertEquals( parts[2], pdoId.toString());
        Assert.assertEquals( parts[3], batchName);
        Assert.assertEquals( parts[4], lcsetSampleName);
        Assert.assertEquals( parts[5], sampleName);
        // Ignore event ID
        Assert.assertEquals( parts[7], LabEventType.INFINIUM_XSTAIN.getName());
        Assert.assertEquals( parts[8], "XStain Station");
        // Ignore event date
        Assert.assertEquals( parts[10], "chip_barcode");
        Assert.assertEquals( parts[11], "chip_name");
        Assert.assertEquals( parts[12], VesselPosition.R01C01.toString());


        EasyMock.verify(mocks);
    }

    // Gets single amp event
    @Test(enabled = false)
    public void testAmpXferEvent() throws NoSuchMethodException {

        LabEvent ampEvent = new LabEvent(LabEventType.INFINIUM_AMPLIFICATION, eventDates[2], "Amp Station", -1l, 1L, "Amp Program");
        StaticPlate ampPlate = new StaticPlate("amp_barcode", StaticPlate.PlateType.NinetySixDeepWell, "amp_name");

        VesselContainer<PlateWell> ampContainer = EasyMock.createMockBuilder(VesselContainer.class).withConstructor(LabVessel.class).withArgs(ampPlate).addMockedMethods("getSampleInstancesAtPositionV2", "getTransfersTo").createMock();

        SectionTransfer ampXfer = new SectionTransfer(dnaPlate.getContainerRole(), SBSSection.ALL96, null, ampContainer, SBSSection.ALL96, null, ampEvent);
        ampEvent.getSectionTransfers().add(ampXfer);
        EasyMock.expect(ampContainer.getTransfersTo()).andReturn(Collections.singleton(ampEvent)).anyTimes();

        // ********* TODO JMS Comes up with empty TreeSet every time
        Set<SampleInstanceV2> emptySamples = new HashSet<>();
        EasyMock.expect(ampContainer.getSampleInstancesAtPositionV2(EasyMock.eq(VesselPosition.A01))).andReturn(sampleInstances);
        EasyMock.expect(ampContainer.getSampleInstancesAtPositionV2(EasyMock.not(EasyMock.eq(VesselPosition.A01)))).andReturn(emptySamples).times(95);

        EasyMock.expect(dao.findById(LabEvent.class, 1L)).andReturn(ampEvent);
        EasyMock.replay(mocks);

        Collection<String> records = arrayProcessFlowEtl.dataRecords(etlDateStr, false, 1L);
        Assert.assertEquals(records.size(), 1);

        String[] parts;
        Iterator<String> iter = records.iterator();

        // Amp
        parts = iter.next().split(",",13);
        Assert.assertEquals( parts[2], pdoId.toString());
        Assert.assertEquals( parts[3], batchName);
        Assert.assertEquals( parts[4], lcsetSampleName);
        Assert.assertEquals( parts[5], sampleName);
        // Ignore event ID
        Assert.assertEquals( parts[7], LabEventType.INFINIUM_AMPLIFICATION.getName());
        Assert.assertEquals( parts[8], "Amp Station");
        // Ignore event date
        Assert.assertEquals( parts[10], "amp_barcode");
        Assert.assertEquals( parts[11], "amp_name");
        Assert.assertEquals( parts[12], VesselPosition.A01.toString());

        EasyMock.verify(mocks);
    }

}
