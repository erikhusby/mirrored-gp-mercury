package org.broadinstitute.sequel.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.sequel.infrastructure.thrift.ThriftService;
import org.broadinstitute.sequel.limsquery.generated.FlowcellDesignationType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author breilly
 */
@Test(singleThreaded = true)
public class LimsQueryResourceUnitTest {

    private ThriftService mockThriftService;
    private LimsQueryResourceResponseFactory mockResponseFactory;
    private LimsQueryResource resource;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        mockThriftService = createMock(ThriftService.class);
        mockResponseFactory = createMock(LimsQueryResourceResponseFactory.class);
        resource = new LimsQueryResource(mockThriftService, mockResponseFactory);
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
        replayAll();

        TException caught = null;
        try {
            resource.findFlowcellDesignationByTaskName("TestTask");
        } catch (TException e) {
            caught = e;
        }
        assertThat(caught.getMessage(), equalTo(thrown.getMessage()));

        verifyAll();
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskNameZimsError() throws Exception {
        TZIMSException thrown = new TZIMSException("ZIMS error!");
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andThrow(thrown);
        replayAll();

        TZIMSException caught = null;
        try {
            resource.findFlowcellDesignationByTaskName("TestTask");
        } catch (TZIMSException e) {
            caught = e;
        }
        assertThat(caught.getDetails(), equalTo(thrown.getDetails()));

        verifyAll();
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskNameRuntimeException() throws Exception {
        RuntimeException thrown = new RuntimeException("Runtime exception!");
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andThrow(thrown);
        replayAll();

        RuntimeException caught = null;
        try {
            resource.findFlowcellDesignationByTaskName("TestTask");
        } catch (RuntimeException e) {
            caught = e;
        }
        assertThat(caught.getMessage(), equalTo(thrown.getMessage()));

        verifyAll();
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubes() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("good_barcode"))).andReturn(true);
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("bad_barcode"))).andReturn(false);
        replayAll();

        boolean result1 = resource.doesLimsRecognizeAllTubes(Arrays.asList("good_barcode"));
        assertThat(result1, is(true));

        boolean result2 = resource.doesLimsRecognizeAllTubes(Arrays.asList("bad_barcode"));
        assertThat(result2, is(false));

        verifyAll();
    }

    private void replayAll() {
        replay(mockThriftService, mockResponseFactory);
    }

    private void verifyAll() {
        verify(mockThriftService, mockResponseFactory);
    }
}
