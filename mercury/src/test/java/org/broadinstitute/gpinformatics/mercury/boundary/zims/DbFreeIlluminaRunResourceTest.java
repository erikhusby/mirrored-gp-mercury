package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniLane;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import junit.framework.Assert;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.gpinformatics.mercury.control.zims.SquidThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.control.zims.ThriftLibraryConverter;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.thrift.MockThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

/**
 * Instead of spending so much time waiting for the thrift server
 * (that's left to {@link IlluminaRunResourceTest}), this test
 * loads a pre-serialized thrift run from local disk, converts it
 * to {@link ZimsIlluminaRun}, and does some basic assertions.
 */
public class DbFreeIlluminaRunResourceTest {

    
    @Test(groups = DATABASE_FREE)
    public void test_known_good_run() throws Exception {
        TZamboniRun thriftRun = ThriftFileAccessor.deserializeRun();
        ZimsIlluminaRun runBean = new IlluminaRunResource(
                new MockThriftService(),
                new BSPSampleDataFetcher(new BSPSampleSearchServiceStub())
        ).getRun(thriftRun,new HashMap<String, BSPSampleDTO>(),new SquidThriftLibraryConverter());
        IlluminaRunResourceTest.doAssertions(thriftRun,runBean,null);
    }

    @Test(groups = DATABASE_FREE)
    public void test_known_good_bsp_sample_run() throws Exception {
        TZamboniRun thriftRun = ThriftFileAccessor.deserializeRun();
        BSPSampleDataFetcher sampleDataFetcher = new BSPSampleDataFetcher(new BSPSampleSearchServiceStub());
        String sample = BSPSampleSearchServiceStub.SM_12CO4;
        BSPSampleDTO sampleDTO = sampleDataFetcher.fetchSingleSampleFromBSP(sample);
        Map <String, BSPSampleDTO> lsidToSampleDTO = new HashMap<String, BSPSampleDTO>();
        lsidToSampleDTO.put(sampleDTO.getSampleLsid(),sampleDTO);


        for (TZamboniLane lane : thriftRun.getLanes()) {
            for (TZamboniLibrary library : lane.getLibraries()) {
                library.setLsid(sampleDTO.getSampleLsid());
            }
        }
        IlluminaRunResource runResource = new IlluminaRunResource(new MockThriftService(),sampleDataFetcher);
        ZimsIlluminaRun runBean = runResource.getRun(thriftRun, lsidToSampleDTO, new SquidThriftLibraryConverter());

        for (ZimsIlluminaChamber lane : runBean.getLanes()) {
            for (LibraryBean libraryBean : lane.getLibraries()) {
                Assert.assertEquals(sampleDTO.getOrganism(),libraryBean.getBspSpecies());
                Assert.assertEquals(sampleDTO.getPrimaryDisease(),libraryBean.getPrimaryDisease());
                Assert.assertEquals(sampleDTO.getSampleType(),libraryBean.getSampleType());
            }
        }


    }

}
