package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniLane;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.thrift.MockThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.zims.SquidThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

/**
 * Instead of spending so much time waiting for the thrift server
 * (that's left to {@link IlluminaRunResourceTest}), this test
 * loads a pre-serialized thrift run from local disk, converts it
 * to {@link ZimsIlluminaRun}, and does some basic assertions.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class DbFreeIlluminaRunResourceTest {


    @BeforeMethod
    private ProductOrderDao getMockDao() {
        ProductOrderDao pdoDao = EasyMock.createMock(ProductOrderDao.class);
        EasyMock.expect(pdoDao.findByBusinessKey((String) EasyMock.anyObject())).andReturn(null).atLeastOnce();
        EasyMock.replay(pdoDao);
        return pdoDao;
    }


    @Test(groups = DATABASE_FREE)
    public void test_error_handling() throws Exception {
        ThriftService brokenThrift = EasyMock.createMock(ThriftService.class);
        EasyMock.expect(brokenThrift.fetchRun((String) EasyMock.anyObject())).andThrow(
                new RuntimeException("something blew up remotely")
        );

        IlluminaSequencingRunDao illuminaSequencingRunDao = EasyMock.createMock(IlluminaSequencingRunDao.class);
        EasyMock.expect(illuminaSequencingRunDao.findByRunName((String) EasyMock.anyObject())).andReturn(null);
        EasyMock.replay(brokenThrift, illuminaSequencingRunDao);

        ZimsIlluminaRun runBean = new IlluminaRunResource(
                brokenThrift,
                new BSPSampleDataFetcherImpl(new BSPSampleSearchServiceStub()),
                illuminaSequencingRunDao
        ).getRun("whatever");

        Assert.assertNotNull(runBean.getError());
        Assert.assertTrue(runBean.getError().contains("Failed while running pipeline query for run"));
    }

    @Test(groups = DATABASE_FREE)
    public void testErrorWithMultipleBatches() throws Exception {
//        Assert.fail("not implemented");
        // TODO: set up a mercury run that will cause getMercuryRun() to throw an exception
        ThriftService brokenThrift = EasyMock.createMock(ThriftService.class);
        EasyMock.expect(brokenThrift.fetchRun((String) EasyMock.anyObject())).andThrow(
                new RuntimeException("something blew up remotely")
        );

        IlluminaSequencingRunDao illuminaSequencingRunDao = EasyMock.createMock(IlluminaSequencingRunDao.class);
        long timeStamp = new Date().getTime();
        IlluminaFlowcell flowcell =
                new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, "FS-" + timeStamp);
//        IlluminaSequencingRun run = new IlluminaSequencingRun(flowcell, "testrun-"+timeStamp);
//        EasyMock.expect(illuminaSequencingRunDao.findByRunName((String)EasyMock.anyObject())).andReturn(run);
        EasyMock.replay(brokenThrift, illuminaSequencingRunDao);

        ZimsIlluminaRun runBean = new IlluminaRunResource(
                brokenThrift,
                new BSPSampleDataFetcherImpl(new BSPSampleSearchServiceStub()),
                illuminaSequencingRunDao
        ).getMercuryRun("whatever");

        Assert.assertNotNull(runBean.getError());
        Assert.assertTrue(runBean.getError().contains("Failed while running pipeline query for run"));
    }

    @Test(groups = DATABASE_FREE)
    public void test_null_run_name() throws Exception {
        ZimsIlluminaRun runBean = new IlluminaRunResource().getRun(null);

        Assert.assertNotNull(runBean.getError());
    }

    @Test(groups = DATABASE_FREE)
    public void test_known_good_run() throws Exception {
        IlluminaSequencingRunDao illuminaSequencingRunDao = EasyMock.createMock(IlluminaSequencingRunDao.class);
        EasyMock.expect(illuminaSequencingRunDao.findByRunName((String) EasyMock.anyObject())).andReturn(null);
        EasyMock.replay(illuminaSequencingRunDao);

        TZamboniRun thriftRun = ThriftFileAccessor.deserializeRun();
        System.out.println("----DBFree IRR test : " + thriftRun.getImagedAreaPerLaneMM2());
        ZimsIlluminaRun runBean = new IlluminaRunResource(
                new MockThriftService(),
                new BSPSampleDataFetcherImpl(new BSPSampleSearchServiceStub()),
                illuminaSequencingRunDao
        ).getRun(thriftRun, new HashMap<String, SampleData>(), new SquidThriftLibraryConverter(), getMockDao()
        );
        IlluminaRunResourceTest.doAssertions(thriftRun, runBean, new HashMap<Long, ProductOrder>());
    }

    @Test(groups = DATABASE_FREE)
    public void test_known_good_bsp_sample_run() throws Exception {
        TZamboniRun thriftRun = ThriftFileAccessor.deserializeRun();
        BSPSampleSearchService sampleSearchService = new BSPSampleSearchServiceStub();
        BSPSampleDataFetcher sampleDataFetcher = new BSPSampleDataFetcher(sampleSearchService) {
            @Override
            public BspSampleData fetchSingleSampleFromBSP(String sampleName) {
                Assert.assertEquals(sampleName, BSPSampleSearchServiceStub.SM_12CO4);
                return super.fetchSingleSampleFromBSP(sampleName);
            }
        };
        String sample = BSPSampleSearchServiceStub.SM_12CO4;
        SampleData sampleDTO = sampleDataFetcher.fetchSingleSampleFromBSP(sample);
        Map<String, SampleData> lsidToSampleDTO = new HashMap<>();
        lsidToSampleDTO.put(sampleDTO.getSampleLsid(), sampleDTO);

        IlluminaSequencingRunDao illuminaSequencingRunDao = EasyMock.createMock(IlluminaSequencingRunDao.class);
        EasyMock.expect(illuminaSequencingRunDao.findByRunName((String) EasyMock.anyObject())).andReturn(null);
        EasyMock.replay(illuminaSequencingRunDao);

        for (TZamboniLane lane : thriftRun.getLanes()) {
            for (TZamboniLibrary library : lane.getLibraries()) {
                library.setLsid(sampleDTO.getSampleLsid());
            }
        }
        IlluminaRunResource runResource = new IlluminaRunResource(new MockThriftService(), sampleDataFetcher,
                illuminaSequencingRunDao);
        ZimsIlluminaRun runBean = runResource
                .getRun(thriftRun, lsidToSampleDTO, new SquidThriftLibraryConverter(), getMockDao()
                );

        for (ZimsIlluminaChamber lane : runBean.getLanes()) {
            for (LibraryBean libraryBean : lane.getLibraries()) {
                Assert.assertEquals(sampleDTO.getOrganism(), libraryBean.getSpecies());

                Assert.assertEquals(sampleDTO.getPrimaryDisease(), libraryBean.getPrimaryDisease());
                Assert.assertEquals(sampleDTO.getSampleType(), libraryBean.getSampleType());
            }
        }
    }

}
