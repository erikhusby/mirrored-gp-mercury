package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import org.apache.thrift.TException;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * @author breilly
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ThriftConnectionTest {

    private ThriftConnection connection;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        connection = new ThriftConnection(ThriftConfig.produce(Deployment.TEST));
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testConnection() throws Exception {
        double result = connection.call(new ThriftConnection.Call<Double>() {
            @Override
            public Double call(LIMQueries.Client client) {
                try {
                    return client.fetchQpcrForTube("0121541795");
                } catch (TException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        assertThat(result, greaterThan(1.0));
    }
}
