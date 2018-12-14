package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcBlacklisting;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentrationStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.InfiniumEntityBuilder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.text.Format;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.test.LabEventTest.buildSamplePlates;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Tests the Metrics View Action Bean.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class MetricsViewActionBeanTest {
    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
    private MetricsViewActionBean actionBean;
    private LabVesselDao labVesselDaoMock;
    private ArraysQcDao arraysQcDaoMock;
    private ProductOrderSampleDao productOrderSampleDaoMock;
    private ProductEjb productEjbMock;
    private AttributeArchetypeDao attributeArchetypeDaoMock;
    private Format fastDateFormat = FastDateFormat.getInstance("MM/dd/yyyy");

    @BeforeMethod
    public void setUp() throws Exception {
        actionBean = new MetricsViewActionBean();
        actionBean.setContext(new CoreActionBeanContext());
        labVesselDaoMock = mock(LabVesselDao.class);
        actionBean.setLabVesselDao(labVesselDaoMock);
        arraysQcDaoMock = mock(ArraysQcDao.class);
        actionBean.setArraysQcDao(arraysQcDaoMock);
        productOrderSampleDaoMock = mock(ProductOrderSampleDao.class);
        actionBean.setProductOrderSampleDao(productOrderSampleDaoMock);
        productEjbMock = mock(ProductEjb.class);
        actionBean.setProductEjb(productEjbMock);
        attributeArchetypeDaoMock = mock(AttributeArchetypeDao.class);
        actionBean.setAttributeArchetypeDao(attributeArchetypeDaoMock);
    }

    public void testFindArraysQcForHybChip() throws Exception {
        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        BSPSetVolumeConcentration bspSetVolumeConcentration =  new BSPSetVolumeConcentrationStub();
        LabEventFactory labEventFactory = new LabEventFactory(testUserList, bspSetVolumeConcentration);
        LabEventHandler labEventHandler = new LabEventHandler();
        Date now = new Date();
        String blacklistFailReason = "DATA_QUALITY";

        int numSamples = 94;
        BaseEventTest.expectedRouting = SystemRouter.System.SQUID;
        ProductOrder productOrder = ProductOrderTestFactory.buildInfiniumProductOrder(numSamples);
        List<StaticPlate> sourcePlates = buildSamplePlates(productOrder, "AmpPlate");
        StaticPlate sourcePlate = sourcePlates.get(0);

        InfiniumEntityBuilder infiniumEntityBuilder =
                new InfiniumEntityBuilder(bettaLimsMessageTestFactory, labEventFactory, labEventHandler,
                        sourcePlate, "Infinium").invoke();

        List<StaticPlate> hybChips = infiniumEntityBuilder.getHybChips();
        StaticPlate hybChip = hybChips.iterator().next();
        VesselPosition hybWellPosition = VesselPosition.R01C01;

        when(labVesselDaoMock.findByIdentifier(hybChip.getLabel())).thenReturn(hybChip);
        Map<String, LabVessel> barcodeToVesselMap = new HashMap<>();
        barcodeToVesselMap.put(hybChip.getLabel(), hybChip);
        when(labVesselDaoMock.findByBarcodes(Collections.singletonList(hybChip.getLabel()))).
                thenReturn(barcodeToVesselMap);

        ProductOrderSample productOrderSample = mock(ProductOrderSample.class);
        when(productOrderSampleDaoMock.findBySamples(anyList())).thenReturn(
                Collections.singletonList(productOrderSample));
        when(productOrderSample.getProductOrder()).thenReturn(productOrder);
        when(productEjbMock.getGenotypingChip(productOrder, productOrder.getCreatedDate())).thenReturn(
                Pair.of("Infinium", "HumanOmni2.5-8v1_A"));

        GenotypingChip genotypingChip = mock(GenotypingChip.class);
        when(attributeArchetypeDaoMock.findGenotypingChip("Infinium", "HumanOmni2.5-8v1_A")).thenReturn(genotypingChip);
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("call_rate_threshold", "98");
        when(genotypingChip.getAttributeMap()).thenReturn(attributeMap);

        actionBean.setBarcodes(hybChip.getLabel());
        actionBean.validateData();
        assertTrue(actionBean.isFoundResults());

        ArraysQc arraysQc = mock(ArraysQc.class);
        when(arraysQc.getChipWellBarcode()).thenReturn(hybChip.getLabel() + "_" + hybWellPosition);
        when(arraysQc.getCallRate()).thenReturn(new BigDecimal(".9877"));
        when(arraysQc.getAutocallCallRate()).thenReturn(new BigDecimal(".9788"));
        when(arraysQc.getHetPct()).thenReturn(new BigDecimal(".19"));
        when(arraysQc.getAutocallDate()).thenReturn(now);
        when(arraysQcDaoMock.findByBarcodes(anyList())).thenReturn(Collections.singletonList(arraysQc));

        ArraysQcBlacklisting arraysQcBlacklisting = mock(ArraysQcBlacklisting.class);
        when(arraysQcBlacklisting.getBlacklistedOn()).thenReturn(now);
        when(arraysQcBlacklisting.getWhitelistedOn()).thenReturn(now);
        when(arraysQcBlacklisting.getBlacklistReason()).thenReturn(blacklistFailReason);

        ListValuedMap<String, ArraysQcBlacklisting> multiMapWellBarcodeToMetric = new ArrayListValuedHashMap<>();
        multiMapWellBarcodeToMetric.put(hybChip.getLabel() + "_" + hybWellPosition, arraysQcBlacklisting);
        when(arraysQcDaoMock.findBlacklistMapByBarcodes(anyList())).thenReturn(multiMapWellBarcodeToMetric);

        Map<String, ArraysQc> mapWellBarcodeToMetric = new HashMap<>();
        mapWellBarcodeToMetric.put(hybChip.getLabel() + "_" + hybWellPosition, arraysQc);
        when(arraysQcDaoMock.findMapByBarcodes(anyList())).thenReturn(mapWellBarcodeToMetric);

        actionBean.buildInfiniumMetricsTable(hybChip);

        MetricsViewActionBean.PlateMap plateMap = actionBean.getPlateMap();
        assertNotNull(plateMap);

        MetricsViewActionBean.WellDataset callRateDataset = null;
        for (MetricsViewActionBean.PlateMapMetrics plateMapMetrics: MetricsViewActionBean.PlateMapMetrics.values()) {
            boolean foundMetric = false;
            for (MetricsViewActionBean.WellDataset wellDataset: plateMap.getDatasets()) {
                if (wellDataset.getPlateMapMetrics() == plateMapMetrics) {
                    foundMetric = true;
                    if (wellDataset.getPlateMapMetrics() == MetricsViewActionBean.PlateMapMetrics.CALL_RATE) {
                        callRateDataset = wellDataset;
                    }
                    break;
                }
            }
            if (!foundMetric) {
                fail("Failed to find plate map metric in datasets: " + plateMapMetrics.name());
            }
        }

        assertEquals( plateMap.getWellStatusMap().get(hybWellPosition.name()), MetricsViewActionBean.WellStatus.Blacklisted );

        String blackListDisplay = null;
        for( MetricsViewActionBean.Metadata metadata : plateMap.getWellMetadataMap().get(hybWellPosition.name()) ) {
            if( metadata.getLabel().equals("Pipeline Blacklist")) {
                blackListDisplay = metadata.getValue();
                break;
            }
        }

        assertEquals( fastDateFormat.format(now) + " - " + blacklistFailReason, blackListDisplay );

        for (MetricsViewActionBean.WellData wellData: callRateDataset.getWellData()) {
            assertEquals(wellData.getValue(), "98.77");
        }
    }
}