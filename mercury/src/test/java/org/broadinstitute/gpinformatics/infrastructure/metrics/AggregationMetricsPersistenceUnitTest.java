package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.STUBBY)
public class AggregationMetricsPersistenceUnitTest extends ContainerTest {

    @PersistenceContext(unitName = "metrics_pu", type = PersistenceContextType.EXTENDED)
    private EntityManager metricsEntityManager;

    public void testDatabaseConnection() {
        Query query = metricsEntityManager.createNativeQuery("select 'test' from dual");
        String result = (String) query.getSingleResult();
        assertThat(result, equalTo("test"));
    }
}
