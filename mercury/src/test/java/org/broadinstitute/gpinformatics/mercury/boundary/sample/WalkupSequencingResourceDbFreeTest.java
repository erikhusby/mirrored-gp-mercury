package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class WalkupSequencingResourceDbFreeTest {

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SampleInstanceEntityDao sampleInstanceEntityDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private SampleKitRequestDao sampleKitRequestDao;


    public void testWalkupSequencing() {

        WalkUpSequencing walkUpSequencing = new WalkUpSequencing();
        walkUpSequencing.setLibraryName("TEST_LIBRARY");
        walkUpSequencing.setTubeBarcode("TEST_TUBE");
        walkUpSequencing.setReadType("TEST_READ_TYPE");
        walkUpSequencing.setAnalysisType("TEST");
        walkUpSequencing.setEmailAddress("TEST@TEST.COM");
        walkUpSequencing.setBaitSetName("TEST_BAIT");
        WalkupSequencingResource walkupSequencingResource = new WalkupSequencingResource();

        setMocks(walkUpSequencing, walkupSequencingResource);

        String status = walkupSequencingResource.getJson(walkUpSequencing);
        Assert.assertEquals(status, walkupSequencingResource.STATUS);

    }



    /**
     * This is where we setup the initial Mocks for External Library testing.
     */
    public void setMocks(WalkUpSequencing walkUpSequencing, WalkupSequencingResource walkupSequencingResource  )
    {


        MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        SampleKitRequestDao sampleKitRequestDao = Mockito.mock(SampleKitRequestDao.class);


        walkupSequencingResource.setSampleKitRequestDao(sampleKitRequestDao);
        walkupSequencingResource.setMercurySampleDao(mercurySampleDao);

        ReagentDesignDao reagentDesignDao = Mockito.mock(ReagentDesignDao.class);
        ReagentDesign reagentDesign = new ReagentDesign();

        List<String> barcodes = new ArrayList<>();
        final String tubeBarcode = walkUpSequencing.getTubeBarcode();
        barcodes.add(tubeBarcode);
        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        Mockito.when(labVesselDao.findByBarcodes(barcodes)).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                }});

        walkupSequencingResource.setLabVesselDao(labVesselDao);

        reagentDesign.setDesignName(walkUpSequencing.getBaitSetName());

        Mockito.when(reagentDesignDao.findByBusinessKey(walkUpSequencing.getBaitSetName())).thenReturn(reagentDesign);


        walkupSequencingResource.setReagentDesignDao(reagentDesignDao);

        SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);
        walkupSequencingResource.setSampleInstanceEntityDao(sampleInstanceEntityDao);




    }


}
