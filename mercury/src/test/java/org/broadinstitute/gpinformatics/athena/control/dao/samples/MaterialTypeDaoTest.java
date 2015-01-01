package org.broadinstitute.gpinformatics.athena.control.dao.samples;


import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;


@Test(groups = {TestGroups.STUBBY})
public class MaterialTypeDaoTest extends ContainerTest {

    @Inject
    private MaterialTypeDao dao;

    @Inject
    private Log log;

    @Inject
    private UserTransaction utx;


    @BeforeMethod
    public void beforeMethod() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        createFixtureData();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    private void createFixtureData() {
        MaterialType materialType;

        materialType = new MaterialType("DNA", "DNA WGA from WGA");
        dao.persist(materialType);

        materialType = new MaterialType("DNA", "DNA Somatic");
        dao.persist(materialType);

        materialType = new MaterialType("DNA", "DNA Genomic");
        dao.persist(materialType);

        dao.flush();
    }


    public void testFindAll() {

        final List<MaterialType> materialTypes = dao.findAll();
        Assert.assertNotNull(materialTypes);

        Assert.assertTrue(materialTypes.size() > 1);

        for (MaterialType materialType : materialTypes) {

            log.debug("Material type name: " + materialType.getName());
            if ( "DNA Somatic".equals(materialType.getName()) ) {
                return;
            }
        }

        Assert.fail("Material type not found!");

    }


    public void testFind() {

        final MaterialType materialType = dao.find("DNA", "DNA Somatic");
        Assert.assertNotNull(materialType);

        // deliberately mismatching the name
        MaterialType missingMaterialType = dao.find("XNA", "ABC");
        Assert.assertNull(missingMaterialType);

    }

}
