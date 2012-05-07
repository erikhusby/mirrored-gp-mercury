package org.broadinstitute.sequel.boundary.zims;


import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftConfiguration;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

public class RunLaneResourceBadThriftTest  {

    private RunLaneResource runLaneResource;

    private TTransport getMockTransport() {
        return EasyMock.createMock(TTransport.class);
    }

    private LIMQueries.Client getMockClient() {
        return EasyMock.createMock(LIMQueries.Client.class);
    }

    @BeforeMethod
    protected void setUp() throws Exception {
        runLaneResource = new RunLaneResource(new ThriftConfiguration() {
            public String getHost() { return null; }
            public int getPort() { return 0; }
        });
    }

    @Test(expectedExceptions = RuntimeException.class,groups = {DATABASE_FREE})
    public void test_bad_transport() throws Exception {
        TTransport badTransport = getMockTransport();
        
        badTransport.open();
        EasyMock.expectLastCall().andThrow(new TTransportException("Can't open!"));

        EasyMock.replay(badTransport);
        runLaneResource.getLibraries(getMockClient(), badTransport, "foo", "3");
    }

    @Test(expectedExceptions = RuntimeException.class,groups = {DATABASE_FREE})
    public void test_client_returns_null() throws Exception {
        LIMQueries.Client limQueries = getMockClient();

        EasyMock.expect(limQueries.fetchSingleLane((String)EasyMock.anyObject(),EasyMock.anyShort())).andReturn(null);

        EasyMock.replay(limQueries);
        runLaneResource.getLibraries(limQueries,getMockTransport(),"foo", "3");
    }

    @Test(expectedExceptions = RuntimeException.class,groups = {DATABASE_FREE})
    public void test_client_throws_exception() throws Exception {
        LIMQueries.Client limQueries = getMockClient();

        EasyMock.expect(limQueries.fetchSingleLane((String)EasyMock.anyObject(),EasyMock.anyShort())).andThrow(
                new TZIMSException("something blew up remotely")
                );

        EasyMock.replay(limQueries);
        runLaneResource.getLibraries(limQueries,getMockTransport(),"foo","3");
    }

    @Test(expectedExceptions = RuntimeException.class,groups = {DATABASE_FREE})
    public void test_client_throws_another_exception() throws Exception {
        LIMQueries.Client limQueries = getMockClient();

        EasyMock.expect(limQueries.fetchSingleLane((String)EasyMock.anyObject(),EasyMock.anyShort())).andThrow(
                new TException("something blew up remotely")
        );

        EasyMock.replay(limQueries);
        runLaneResource.getLibraries(limQueries,getMockTransport(),"foo","3");
    }

}
