package org.broadinstitute.gpinformatics.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false )
public class BSPMaterialTypeListTest extends ContainerTest {

    @Inject
    BSPMaterialTypeList bspMaterialTypeList;

    @Test
    public void testGetAllMaterials() throws Exception {
        List<MaterialType> materialTypes = bspMaterialTypeList.getMaterialTypes();
        Assert.assertNotNull(materialTypes);
        Assert.assertTrue(!materialTypes.isEmpty());
        //An arbitrary sanity check; the actual database has at least 80 types
        Assert.assertTrue(materialTypes.size() > 80);

    }

    @Test
    public void testFindUserByCategory() throws Exception {
        List<MaterialType> materialTypes = bspMaterialTypeList.getByCategory( "DNA");
        Assert.assertNotNull(materialTypes);
        Assert.assertTrue(!materialTypes.isEmpty());
        // An arbitrary sanity check; the actual database has at least several DNA material types
        Assert.assertTrue(materialTypes.size() > 8);
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
        // An arbitrary sanity check; the actual database has at least one Genomic material types
        Assert.assertTrue(materialTypes.size() > 0);
    }
}
