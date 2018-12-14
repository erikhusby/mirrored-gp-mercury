package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;

@Test(groups = TestGroups.STUBBY, enabled = true )
@Dependent
public class BSPMaterialTypeListTest extends StubbyContainerTest {

    public BSPMaterialTypeListTest(){}

    @Inject
    BSPMaterialTypeList bspMaterialTypeList;
//
//    @Deployment
//    public static WebArchive deployment() {
//        return DeploymentBuilder.buildMercuryWar(DEV);
//    }

    @Test
    public void testGetAllMaterials() throws Exception {
        List<MaterialType> materialTypes = bspMaterialTypeList.getMaterialTypes();
        Assert.assertNotNull(materialTypes);
        Assert.assertTrue(!materialTypes.isEmpty());
        // An arbitrary sanity check, BSP Dev has over 80, Stubby has 5 total.
        Assert.assertTrue(materialTypes.size() > 4, "actual number was " + materialTypes.size());

    }

    @Test
    public void testFindUserByCategory() throws Exception {
        List<MaterialType> materialTypes = bspMaterialTypeList.getByCategory( "DNA");
        Assert.assertNotNull(materialTypes);
        Assert.assertTrue(!materialTypes.isEmpty());
        // An arbitrary sanity check; BSP Dev has many, Stubby has 3 for DNA.
        Assert.assertTrue(materialTypes.size() > 2, "actual number was " + materialTypes.size());
    }

    @Test
    public void testFind() throws Exception {
        List<MaterialType> materialTypes = bspMaterialTypeList.find( null );
        Assert.assertNotNull(materialTypes);
        Assert.assertTrue(materialTypes.isEmpty());

        materialTypes = bspMaterialTypeList.find( " " );
        Assert.assertNotNull(materialTypes);
        Assert.assertTrue(materialTypes.isEmpty());

        materialTypes = bspMaterialTypeList.find( "  QWERTY KEYBOARD ");
        Assert.assertNotNull(materialTypes);
        Assert.assertTrue(materialTypes.isEmpty());

        materialTypes = bspMaterialTypeList.find( "  Genomic  ");
        Assert.assertNotNull(materialTypes);
        Assert.assertTrue(!materialTypes.isEmpty());
        // An arbitrary sanity check; the actual database has at least one Genomic material type.
        Assert.assertTrue(materialTypes.size() > 0, "actual number was " + materialTypes.size());
    }
}
