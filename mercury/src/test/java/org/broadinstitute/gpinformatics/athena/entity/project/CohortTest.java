package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.SortedSet;
import java.util.TreeSet;

@Test(groups = TestGroups.DATABASE_FREE)
public class CohortTest {

    @Test
    public void sortCohortTest() throws Exception {
        SortedSet<Cohort> usersCohorts = new TreeSet<>(Cohort.COHORT_BY_ID);

        // Add an item with a null id
        Cohort cohort = new Cohort(null, null, null, null, false);
        usersCohorts.add(cohort);
        Assert.assertTrue(usersCohorts.size() == 1);

        cohort = new Cohort("test5", "test1", "cat1", "group1", true);
        usersCohorts.add(cohort);
        cohort = new Cohort("test7", "test1", "cat1", "group1", true);
        usersCohorts.add(cohort);
        cohort = new Cohort("test2", "test1", "cat1", "group1", true);
        usersCohorts.add(cohort);
        cohort = new Cohort("test4", "test1", "cat1", "group1", true);
        usersCohorts.add(cohort);
        cohort = new Cohort("test4", "test1", "cat1", "group1", true);
        usersCohorts.add(cohort);
        cohort = new Cohort("test9", "test1", "cat1", "group1", true);
        usersCohorts.add(cohort);
        cohort = new Cohort("atest", "test1", "cat1", "group1", true);
        usersCohorts.add(cohort);

        int i=0;
        String[] ids = new String[usersCohorts.size()];
        for (Cohort currentCohort : usersCohorts) {
            ids[i++] = currentCohort.getCohortId();
        }

        Assert.assertEquals(StringUtils.join(ids, ", "), ", atest, test2, test4, test5, test7, test9", "failed to sort as expected");
    }
}
