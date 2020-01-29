package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcBlacklisting;
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
                "200584330132_R12C01", "200584330041_R07C02"));
        Assert.assertEquals(arraysQcList.size(), 2);
        // The data is currently in flux, so just check that there are no exceptions in the mapping
        for (ArraysQc arraysQc : arraysQcList) {
            arraysQc.getArraysQcFingerprints().size();
            arraysQc.getArraysQcGtConcordances().size();
            arraysQc.getArraysQcContamination();
        }
    }

    @Test
    public void testFindBlacklist() {
        List<ArraysQcBlacklisting> arraysBlacklist = arraysQcDao.findBlacklistByBarcodes(Arrays.asList(
                "1214201633HC0_R02C01", "1214172005HC0_R01C01"));
        Assert.assertEquals(arraysBlacklist.size(), 2);
        // The data is currently in flux, so just check that there are no exceptions in the mapping
        for (ArraysQcBlacklisting blacklisting : arraysBlacklist) {
            System.out.println( blacklisting.getChipWellBarcode() + ":" + blacklisting.getBlacklistReason() );
        }
    }
}