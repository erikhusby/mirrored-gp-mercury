package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * @author breilly
 */
public class ThriftConnectionTest {

    private ThriftConnection connection;

    @BeforeMethod
    public void setUp() throws Exception {
        connection = new ThriftConnection(ThriftConfigProducer.produce(Deployment.TEST));
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testConnection() throws Exception {
        double result = connection.call(new ThriftConnection.Call<Double>() {
            @Override
            public Double call(LIMQueries.Client client) throws TException, TZIMSException {
                return client.fetchQpcrForTube("0121541795");
            }
        });
        assertThat(result, greaterThan(1.0));
    }
}
