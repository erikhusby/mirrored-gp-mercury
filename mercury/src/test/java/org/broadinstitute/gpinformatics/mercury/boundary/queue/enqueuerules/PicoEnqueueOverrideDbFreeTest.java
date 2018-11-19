package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.broadinstitute.bsp.client.response.ExomeExpressCheckResponse;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

@Test(groups = DATABASE_FREE)
public class PicoEnqueueOverrideDbFreeTest extends AbstractEnqueueOverrideTest {

    public static final String RANDOM_SAMPLE_BARCODE = "SM-1234";

    @Override
    @BeforeClass
    public void setup() {
        enqueueOverride = new PicoEnqueueOverride(generateBspRestClient(true));
    }

    /**
     * In this particular test I will be adding one of each possibility to the queue, in the result was as expected that they
     * be entered in as the barcode for the mercury sample.
     */
    @Override
    @Test(groups = DATABASE_FREE)
    public void queueSpecificCheckForSpecialPriorityTypeTest() {

        // Change default exome express value to be false for next test
        enqueueOverride = new PicoEnqueueOverride(generateBspRestClient(false));

        // Standard sample that is non-exome express and non-clinical
        MercurySample standardSample = new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.BSP);
        Assert.assertEquals(
                enqueueOverride.checkForSpecialPriorityType(Collections.singletonList(standardSample)),
                QueuePriority.STANDARD);

        // Reset default exomeexpress return value to yes for exex
        setup();

        // Clinical sample
        MercurySample clinicalSample = new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.CRSP_PORTAL);
        Assert.assertEquals(
                enqueueOverride.checkForSpecialPriorityType(Collections.singletonList(clinicalSample)),
                QueuePriority.CLIA);

        // ExEx sample where the sample is from a PDO
        MercurySample exomeExpressSampleFromPdo = generateExExSampleFromPdo();
        Assert.assertEquals(
                enqueueOverride.checkForSpecialPriorityType(Collections.singletonList(exomeExpressSampleFromPdo)),
                QueuePriority.EXOME_EXPRESS);

        // ExomeExpress sample where it is found in the BSP web service
        MercurySample exomeExpressSampleFromWebService = new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.BSP);
        Assert.assertEquals(
                enqueueOverride.checkForSpecialPriorityType(Collections.singletonList(exomeExpressSampleFromWebService)),
                QueuePriority.EXOME_EXPRESS);
    }

    @Override
    @Test(groups = DATABASE_FREE)
    public void determineQueuePriority_MercurySamplesTest() {

        //  STANDARD FIRST
        LabVessel standard = generateStandardLabVessel(2);

        // then EXOME EXPRESS
        LabVessel exomeExpress = generateExomeExpressLabVessel();

        // then CLIA
        LabVessel clia = generateHighestPriorityLevelLabVessel(1);

        QueueGrouping queueGrouping = new QueueGrouping();
        queueGrouping.setQueuedEntities(new ArrayList<>());

        QueueEntity queueEntity = new QueueEntity(queueGrouping, standard);
        QueueEntity queueEntity2 = new QueueEntity(queueGrouping, exomeExpress);
        QueueEntity queueEntity3 = new QueueEntity(queueGrouping, clia);

        queueGrouping.getQueuedEntities().add(queueEntity);
        queueGrouping.getQueuedEntities().add(queueEntity2);
        queueGrouping.getQueuedEntities().add(queueEntity3);

        QueuePriority queuePriority = enqueueOverride.determineQueuePriority(queueGrouping);
        Assert.assertEquals(queuePriority, QueuePriority.CLIA);
    }

    @Override
    @Test(groups = DATABASE_FREE)
    public void determineQueuePriority_SampleInstanceV2Test() {

        //  STANDARD FIRST
        LabVessel standardInner = generateStandardLabVessel(2);
        LabVessel standard = generateSampleInstanceV2LabVessel(standardInner);

        // then EXOME EXPRESS
        LabVessel exomeExpressInner = generateExomeExpressLabVessel();
        LabVessel exomeExpress = generateSampleInstanceV2LabVessel(exomeExpressInner);

        // then CLIA
        LabVessel cliaInner = generateHighestPriorityLevelLabVessel(1);
        LabVessel clia = generateSampleInstanceV2LabVessel(cliaInner);

        QueueGrouping queueGrouping = new QueueGrouping();
        queueGrouping.setQueuedEntities(new ArrayList<>());

        QueueEntity queueEntity = new QueueEntity(queueGrouping, standard);
        QueueEntity queueEntity2 = new QueueEntity(queueGrouping, exomeExpress);
        QueueEntity queueEntity3 = new QueueEntity(queueGrouping, clia);

        queueGrouping.getQueuedEntities().add(queueEntity);
        queueGrouping.getQueuedEntities().add(queueEntity2);
        queueGrouping.getQueuedEntities().add(queueEntity3);

        QueuePriority queuePriority = enqueueOverride.determineQueuePriority(queueGrouping);
        Assert.assertEquals(queuePriority, QueuePriority.CLIA);
    }

    @NotNull
    private LabVessel generateSampleInstanceV2LabVessel(LabVessel standardInner) {
        LabVessel standard = Mockito.mock(LabVessel.class);
        HashSet<MercurySample> empty = new HashSet<>();
        Mockito.when(standard.getMercurySamples()).thenReturn(empty);
        HashSet<SampleInstanceV2> sampleInstanceV2s = new HashSet<>();
        SampleInstanceV2 sampleInstanceV2 = Mockito.mock(SampleInstanceV2.class);
        Set<MercurySample> mercurySamples = standardInner.getMercurySamples();
        Mockito.when(sampleInstanceV2.getRootMercurySamples()).thenReturn(mercurySamples);
        sampleInstanceV2s.add(sampleInstanceV2);
        Mockito.when(standard.getSampleInstancesV2()).thenReturn(sampleInstanceV2s);
        return standard;
    }

    @NotNull
    private LabVessel generateExomeExpressLabVessel() {
        LabVessel exomeExpress = Mockito.mock(LabVessel.class);
        HashSet<MercurySample> exExSamples = new HashSet<>(Collections.singletonList(generateExExSampleFromPdo()));
        Mockito.when(exomeExpress.getMercurySamples()).thenReturn(exExSamples);
        Mockito.when(exomeExpress.getLabVesselId()).thenReturn(3L);
        return exomeExpress;
    }

    private LabVessel generateStandardLabVessel(long labVesselId) {
        LabVessel labVessel = Mockito.mock(LabVessel.class);
        Mockito.when(labVessel.getLabVesselId()).thenReturn(labVesselId);
        MercurySample standard = Mockito.mock(MercurySample.class);

        Mockito.when(standard.isClinicalSample()).thenReturn(false);
        Mockito.when(standard.getMetadataSource()).thenReturn(MercurySample.MetadataSource.MERCURY);
        Mockito.when(standard.getProductOrderSamples()).thenReturn(new HashSet<>());

        Mockito.when(labVessel.getMercurySamples()).thenReturn(new HashSet<>(Collections.singletonList(standard)));
        return labVessel;
    }

    private MercurySample generateExExSampleFromPdo() {
        MercurySample mercurySample = Mockito.mock(MercurySample.class);
        HashSet<ProductOrderSample> pdoSamples = new HashSet<>();
        ProductOrderSample productOrderSample = Mockito.mock(ProductOrderSample.class);
        ProductOrder productOrder = Mockito.mock(ProductOrder.class);
        Product product = Mockito.mock(Product.class);
        Mockito.when(product.isExomeExpress()).thenReturn(true);
        Mockito.when(productOrder.getProduct()).thenReturn(product);
        Mockito.when(productOrderSample.getProductOrder()).thenReturn(productOrder);
        pdoSamples.add(productOrderSample);
        Mockito.when(mercurySample.getProductOrderSamples()).thenReturn(pdoSamples);
        return mercurySample;
    }

    private BSPRestClient generateBspRestClient(boolean containsExomeExpressReturnValue) {
        BSPRestClient restClient = Mockito.mock(BSPRestClient.class);

        ExomeExpressCheckResponse response = Mockito.mock(ExomeExpressCheckResponse.class);
        Mockito.when(restClient.callExomeExpressCheck(Mockito.anyListOf(String.class))).thenReturn(response);
        Mockito.when(response.containsAnyExomeExpressSamples()).thenReturn(containsExomeExpressReturnValue);
        return restClient;
    }

    @Override
    @NotNull
    protected LabVessel generateHighestPriorityLevelLabVessel(long labVesselIdToSet) {
        LabVessel labVessel = Mockito.mock(LabVessel.class);
        Mockito.when(labVessel.getLabVesselId()).thenReturn(labVesselIdToSet);
        // setup samples as Clia -
        Set<MercurySample> mercurySamples = new HashSet<>();
        MercurySample mercurySample = Mockito.mock(MercurySample.class);
        Mockito.when(mercurySample.isClinicalSample()).thenReturn(true);
        mercurySamples.add(mercurySample);
        Mockito.when(labVessel.getMercurySamples()).thenReturn(mercurySamples);
        return labVessel;
    }
}
