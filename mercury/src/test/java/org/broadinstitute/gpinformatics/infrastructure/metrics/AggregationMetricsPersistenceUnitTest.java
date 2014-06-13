package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class AggregationMetricsPersistenceUnitTest extends ContainerTest {

    @PersistenceContext(unitName = "metrics_pu")
    private EntityManager entityManager;

    public void testDatabaseConnection() {
        assertThat(entityManager.isOpen(), is(true));
    }
}
