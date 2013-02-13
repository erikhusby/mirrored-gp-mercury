package org.broadinstitute.gpinformatics.mercury.boundary.zims;


import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import junit.framework.Assert;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.easymock.EasyMock;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

public class IlluminaRunResourceBadThriftTest {

    private IlluminaRunResource runLaneResource;

    private ThriftService getMockThriftService() {
        return EasyMock.createMock(ThriftService.class);
    }


    @BeforeMethod
    protected void setUp() throws Exception {
        runLaneResource = new IlluminaRunResource();
    }

    @Test(expectedExceptions = RuntimeException.class,groups = {DATABASE_FREE})
    public void test_bad_transport() throws Exception {
        ThriftService badService = getMockThriftService();
        
        badService.fetchRun((String)EasyMock.anyObject());
        EasyMock.expectLastCall().andThrow(new RuntimeException("Can't open!"));

        EasyMock.replay(badService);
        runLaneResource.getRun(badService,"foo");
    }


    @Test(groups = {DATABASE_FREE})
    public void test_client_returns_null() throws Exception {
        ThriftService badService = getMockThriftService();
        EasyMock.expect(badService.fetchRun((String)EasyMock.anyObject())).andReturn(null);

        EasyMock.replay(badService);
        ZimsIlluminaRun run = runLaneResource.getRun(badService,"foo");
        Assert.assertNotNull(run);
        Assert.assertNotNull(run.getError());
        Assert.assertNull(run.getName());
    }

    @Test(expectedExceptions = RuntimeException.class,groups = {DATABASE_FREE})
    public void test_client_throws_exception() throws Exception {
        ThriftService badService = getMockThriftService();

        EasyMock.expect(badService.fetchRun((String)EasyMock.anyObject())).andThrow(
                new TZIMSException("something blew up remotely")
                );

        EasyMock.replay(badService);
        runLaneResource.getRun(badService,"foo");
    }

    @Test(expectedExceptions = RuntimeException.class,groups = {DATABASE_FREE})
    public void test_client_throws_another_exception() throws Exception {
        ThriftService badService = getMockThriftService();

        EasyMock.expect(badService.fetchRun((String)EasyMock.anyObject())).andThrow(
                new TException("something blew up remotely")
        );

        EasyMock.replay(badService);
        runLaneResource.getRun(badService,"foo");
    }


}
