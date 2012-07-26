package org.broadinstitute.sequel.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.commons.logging.Log;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftService;
import org.broadinstitute.sequel.limsquery.generated.FlowcellDesignationType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.WebApplicationException;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author breilly
 */
@Test(singleThreaded = true)
public class LimsQueryResourceTest {

    private ThriftService mockThriftService;
    private LimsQueryResourceResponseFactory mockResponseFactory;
    private LimsQueryResource resource;
    private Log mockLog;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        mockThriftService = createMock(ThriftService.class);
        mockResponseFactory = createMock(LimsQueryResourceResponseFactory.class);
        mockLog = createMock(Log.class);
        resource = new LimsQueryResource(mockThriftService, mockResponseFactory, mockLog);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskName() throws TException, TZIMSException {
        FlowcellDesignation flowcellDesignation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andReturn(flowcellDesignation);
        expect(mockResponseFactory.makeFlowcellDesignation(flowcellDesignation)).andReturn(new FlowcellDesignationType());
        replayAll();

        resource.findFlowcellDesignationByTaskName("TestTask");

        verifyAll();
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskNameThriftError() throws Exception {
        TException thrown = new TException("Thrift error!");
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andThrow(thrown);
        mockLog.error(thrown);
        replayAll();

        Exception caught = null;
        try {
            resource.findFlowcellDesignationByTaskName("TestTask");
        } catch (Exception e) {
            caught = e;
        }
        assertException(caught, 500, thrown.getMessage());

        verifyAll();
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskNameZimsError() throws Exception {
        TZIMSException thrown = new TZIMSException("ZIMS error!");
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andThrow(thrown);
        mockLog.error(thrown);
        replayAll();

        Exception caught = null;
        try {
            resource.findFlowcellDesignationByTaskName("TestTask");
        } catch (Exception e) {
            caught = e;
        }
        assertException(caught, 500, thrown.getDetails());

        verifyAll();
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskNameRuntimeException() throws Exception {
        RuntimeException thrown = new RuntimeException("Runtime exception!");
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andThrow(thrown);
        mockLog.error(thrown);
        replayAll();

        Exception caught = null;
        try {
            resource.findFlowcellDesignationByTaskName("TestTask");
        } catch (Exception e) {
            caught = e;
        }
        assertException(caught, 500, thrown.getMessage());

        verifyAll();
    }

    private void assertException(Exception caught, int status, String error) {
        assertThat(caught, instanceOf(WebApplicationException.class));
        WebApplicationException webApplicationException = (WebApplicationException) caught;
        assertThat(webApplicationException.getResponse().getStatus(), equalTo(status));
        Object entity = webApplicationException.getResponse().getEntity();
        assertThat((String) entity, equalTo(error));
    }

    private void replayAll() {
        replay(mockThriftService, mockResponseFactory, mockLog);
    }

    private void verifyAll() {
        verify(mockThriftService, mockResponseFactory, mockLog);
    }
}
