package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PipelineDataType;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

@Test(groups = TestGroups.DATABASE_FREE)
public class DbFreeSquidThriftLibraryConverterTest {

    @Test(groups = DATABASE_FREE)
    public void test_mercury_fields() throws Exception {
        TZamboniRun thriftRun = ThriftFileAccessor.deserializeRun();
        ResearchProject project = new ResearchProject(1L,"RP title","rp synopsis",false,
                                                      ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);

        BspUser bspUser = new BspUser();
        bspUser.setUserId(ResearchProjectTestFactory.TEST_CREATOR);

        ProductOrder pdo = new ProductOrder(bspUser, project);
        pdo.setJiraTicketKey("PDO-2");
        pdo.setProduct(
                new Product("Mashed Potatoes", new ProductFamily("Mashed Things"), null, null, null, null, null, null,
                        null, null, null, null, false, Workflow.NONE, false,new PipelineDataType("with gravy", true)));

        SquidThriftLibraryConverter converter = new SquidThriftLibraryConverter();
        TZamboniLibrary zamboniLibrary = thriftRun.getLanes().iterator().next().getLibraries().iterator().next();
        zamboniLibrary.setLcset("LCSET-123");

        LibraryBean lib = converter.convertLibrary(zamboniLibrary, null, pdo);
        Assert.assertEquals(pdo.getBusinessKey(), lib.getProductOrderKey());
        Assert.assertEquals(pdo.getTitle(), lib.getProductOrderTitle());
        Assert.assertEquals(pdo.getResearchProject().getBusinessKey(), lib.getResearchProjectId());
        Assert.assertEquals(pdo.getResearchProject().getTitle(), lib.getResearchProjectName());
        Assert.assertEquals(lib.getMetadataSource(), MercurySample.GSSR_METADATA_SOURCE);

        lib = converter.convertLibrary(zamboniLibrary, null, null);
        Assert.assertNull(lib.getProductOrderKey());
        Assert.assertNull(lib.getProductOrderTitle());
        Assert.assertNull(lib.getResearchProjectId());
        Assert.assertNull(lib.getResearchProjectName());
        Assert.assertEquals(lib.getMetadataSource(), MercurySample.GSSR_METADATA_SOURCE);

        lib = converter.convertLibrary(zamboniLibrary, null, pdo);
        Assert.assertEquals(lib.getMetadataSource(), MercurySample.GSSR_METADATA_SOURCE);
        Assert.assertEquals(pdo.getBusinessKey(), lib.getProductOrderKey());
        Assert.assertEquals(pdo.getTitle(), lib.getProductOrderTitle());
        Assert.assertEquals(lib.getResearchProjectId(),project.getBusinessKey());
        Assert.assertEquals(lib.getResearchProjectName(),project.getTitle());

        Assert.assertEquals("Mashed Potatoes", lib.getProduct());
        Assert.assertEquals("with gravy", lib.getDataType());
        Assert.assertEquals("Mashed Things", lib.getProductFamily());

        Assert.assertEquals(lib.getLcSet(), zamboniLibrary.getLcset());

        lib = converter.convertLibrary(zamboniLibrary,new BspSampleData(),pdo);
        Assert.assertEquals(lib.getMetadataSource(), MercurySample.BSP_METADATA_SOURCE);
    }

}
