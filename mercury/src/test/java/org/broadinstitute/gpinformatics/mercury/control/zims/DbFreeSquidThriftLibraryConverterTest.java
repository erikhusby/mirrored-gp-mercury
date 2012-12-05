package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.thrift.MockThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResource;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResourceTest;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.HashMap;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

public class DbFreeSquidThriftLibraryConverterTest {

    @Test(groups = DATABASE_FREE)
    public void test_mercury_fields() throws Exception {
        TZamboniRun thriftRun = ThriftFileAccessor.deserializeRun();
        ResearchProject project = new ResearchProject(1L,"RP title","rp synopsis",false);
        ProductOrder pdo = new ProductOrder(new BspUser(),project);
        pdo.setJiraTicketKey("PDO-2");
        pdo.setProduct(new Product("Mashed Potatoes",new ProductFamily("Mashed Things"),
                null,null,null,null,null,null,null,null,null,null,false,null,false));

        SquidThriftLibraryConverter converter = new SquidThriftLibraryConverter();
        TZamboniLibrary zamboniLibrary = thriftRun.getLanes().iterator().next().getLibraries().iterator().next();
        zamboniLibrary.setLcset("LCSET-123");

        LibraryBean lib = converter.convertLibrary(zamboniLibrary, null, pdo);
        assertEquals(pdo.getBusinessKey(),lib.getProductOrderKey());
        assertEquals(pdo.getTitle(),lib.getProductOrderTitle());
        assertEquals(pdo.getResearchProject().getBusinessKey(),lib.getMercuryProjectKey());
        assertEquals(pdo.getResearchProject().getTitle(),lib.getMercuryProjectTitle());

        lib = converter.convertLibrary(zamboniLibrary, null, null);
        assertNull(lib.getProductOrderKey());
        assertNull(lib.getProductOrderTitle());
        assertNull(lib.getMercuryProjectKey());
        assertNull(lib.getMercuryProjectTitle());

        pdo.setResearchProject(null);
        lib = converter.convertLibrary(zamboniLibrary, null, pdo);
        assertEquals(pdo.getBusinessKey(),lib.getProductOrderKey());
        assertEquals(pdo.getTitle(),lib.getProductOrderTitle());
        assertNull(lib.getMercuryProjectKey());
        assertNull(lib.getMercuryProjectTitle());

        assertEquals("Mashed Potatoes",lib.getMercuryProduct());
        assertEquals("Mashed Things",lib.getMercuryProductFamily());

        assertEquals(lib.getLcSet(),zamboniLibrary.getLcset());
    }

}
