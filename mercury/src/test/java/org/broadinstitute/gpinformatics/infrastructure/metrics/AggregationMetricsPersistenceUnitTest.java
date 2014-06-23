package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.STUBBY)
public class AggregationMetricsPersistenceUnitTest extends ContainerTest {

    @PersistenceContext(unitName = "metrics_pu")
    private EntityManager entityManager;

    public void testDatabaseConnection() {
        Query query = entityManager.createNativeQuery("select 'test' from dual");
        String result = (String) query.getSingleResult();
        assertThat(result, equalTo("test"));
    }
}
