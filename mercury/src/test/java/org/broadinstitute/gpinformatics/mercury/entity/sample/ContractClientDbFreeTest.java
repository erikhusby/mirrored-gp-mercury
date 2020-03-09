package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.testng.Assert.*;

@Test(singleThreaded = true, groups = DATABASE_FREE)
public class ContractClientDbFreeTest {

    /**
     * Farily simple test, just to make sure that we can accept either value to find a ContractClient
     */
    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFindByNameOrText() {
        Assert.assertEquals(ContractClient.MAYO, ContractClient.findByNameOrText(ContractClient.MAYO.name()));
        Assert.assertEquals(ContractClient.MAYO, ContractClient.findByNameOrText(ContractClient.MAYO.getDescription()));
    }

    /**
     * Makes sure that we can't have any duplicates for fidning contract clients.  Since we allow the search to be
     * within both name and description, we need to make sure they're unique TOGETHER not just apart.
     */
    @Test(groups = TestGroups.DATABASE_FREE)
    public void verifyAllDescriptionsAreUniqueTest() {
        Set<String> uniquenessCheck = new HashSet<>();
        for (ContractClient contractClient : ContractClient.values()) {
            Assert.assertTrue(uniquenessCheck.add(contractClient.getDescription()));
            Assert.assertTrue(uniquenessCheck.add(contractClient.name()));
        }
    }
}