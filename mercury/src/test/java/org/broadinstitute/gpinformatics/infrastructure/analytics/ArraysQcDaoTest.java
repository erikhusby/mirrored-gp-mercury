package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.testng.Assert.*;

/**
 * Test the DAO.
 */
@Test(groups = TestGroups.STANDARD)
public class ArraysQcDaoTest extends Arquillian {

    @Inject
    private ArraysQcDao arraysQcDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testFindByBarcodes() {
        List<ArraysQc> arraysQcList = arraysQcDao.findByBarcodes(Arrays.asList(
                "200737970182_R01C01", "200737970181_R12C02"));
        Assert.assertEquals(arraysQcList.size(), 2);
    }
}