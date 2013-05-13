package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

public class DbFreeSquidThriftLibraryConverterTest {

    @Test(groups = DATABASE_FREE)
    public void test_mercury_fields() throws Exception {
        TZamboniRun thriftRun = ThriftFileAccessor.deserializeRun();
        ResearchProject project = new ResearchProject(1L,"RP title","rp synopsis",false);

        BspUser bspUser = new BspUser();
        bspUser.setUserId(ResearchProjectTestFactory.TEST_CREATOR);

        ProductOrder pdo = new ProductOrder(bspUser, project);
        pdo.setJiraTicketKey("PDO-2");
        pdo.setProduct(new Product("Mashed Potatoes",new ProductFamily("Mashed Things"),
                null,null,null,null,null,null,null,null,null,null,false,null,false, "with gravy"));

        SquidThriftLibraryConverter converter = new SquidThriftLibraryConverter();
        TZamboniLibrary zamboniLibrary = thriftRun.getLanes().iterator().next().getLibraries().iterator().next();
        zamboniLibrary.setLcset("LCSET-123");

        LibraryBean lib = converter.convertLibrary(zamboniLibrary, null, pdo);
        assertEquals(pdo.getBusinessKey(),lib.getProductOrderKey());
        assertEquals(pdo.getTitle(),lib.getProductOrderTitle());
        assertEquals(pdo.getResearchProject().getBusinessKey(),lib.getResearchProjectId());
        assertEquals(pdo.getResearchProject().getTitle(),lib.getResearchProjectName());

        lib = converter.convertLibrary(zamboniLibrary, null, null);
        assertNull(lib.getProductOrderKey());
        assertNull(lib.getProductOrderTitle());
        assertNull(lib.getResearchProjectId());
        assertNull(lib.getResearchProjectName());

        pdo.setResearchProject(null);
        lib = converter.convertLibrary(zamboniLibrary, null, pdo);
        assertEquals(pdo.getBusinessKey(),lib.getProductOrderKey());
        assertEquals(pdo.getTitle(),lib.getProductOrderTitle());
        assertNull(lib.getResearchProjectId());
        assertNull(lib.getResearchProjectName());

        assertEquals("Mashed Potatoes",lib.getProduct());
        assertEquals("with gravy",lib.getDataType());
        assertEquals("Mashed Things",lib.getProductFamily());

        assertEquals(lib.getLcSet(),zamboniLibrary.getLcset());
    }

}
