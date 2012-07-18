package org.broadinstitute.sequel.infrastructure.thrift;

import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author breilly
 */
public class ThriftConnectionTest {

    private ThriftConnection connection;

    @BeforeMethod
    public void setUp() throws Exception {
        connection = new ThriftConnection(ThriftConfigProducer.produce(Deployment.TEST));
    }

    @Test(groups = TestGroups.DATABASE_FREE, expectedExceptions = IllegalStateException.class)
    public void testGetClientBeforeOpen() {
        connection.getClient();
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testGetClient() {
        connection.open();
        connection.getClient();
        connection.close();
    }
}
