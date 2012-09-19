package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.FlowcellDesignationType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.TestGroups.DATABASE_FREE;
import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author breilly
 */
@Test(singleThreaded = true)
public class LimsQueryResourceUnitTest {

    private ThriftService mockThriftService;
    private LimsQueryResourceResponseFactory mockResponseFactory;
    private TwoDBarcodedTubeDAO mockTwoDBarcodedTubeDAO;
    private LimsQueryResource resource;
    private StaticPlateDAO mockStaticPlateDAO;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
        mockThriftService = createMock(ThriftService.class);
        mockResponseFactory = createMock(LimsQueryResourceResponseFactory.class);
        mockTwoDBarcodedTubeDAO = createMock(TwoDBarcodedTubeDAO.class);
        mockStaticPlateDAO = createMock(StaticPlateDAO.class);
        resource = new LimsQueryResource(mockThriftService, mockResponseFactory, mockTwoDBarcodedTubeDAO, mockStaticPlateDAO);
    }

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskName() throws TException, TZIMSException {
        FlowcellDesignation flowcellDesignation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andReturn(flowcellDesignation);
        expect(mockResponseFactory.makeFlowcellDesignation(flowcellDesignation)).andReturn(new FlowcellDesignationType());
        replayAll();

        resource.findFlowcellDesignationByTaskName("TestTask");

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
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

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByReagentBlockBarcode() throws Exception {
        FlowcellDesignation flowcellDesignation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock")).andReturn(flowcellDesignation);
        FlowcellDesignationType expected = new FlowcellDesignationType();
        expect(mockResponseFactory.makeFlowcellDesignation(flowcellDesignation)).andReturn(expected);
        replayAll();

        FlowcellDesignationType result = resource.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock");
        assertThat(result, equalTo(expected));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByReagentBlockBarcodeNotFound() {
        RuntimeException thrown = new RuntimeException("Not found");
        expect(mockThriftService.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock")).andThrow(thrown);
        replayAll();

        RuntimeException caught = null;
        try {
            resource.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock");
        } catch (RuntimeException e) {
            caught = e;
        }
        assertThat(caught.getMessage(), equalTo(thrown.getMessage()));

        verifyAll();
    }


    @Test(groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesFromSquid() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("squid_barcode"))).andReturn(true);
        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("squid_barcode"))).andReturn(new HashMap<String, TwoDBarcodedTube>());
        replayAll();

        boolean result = resource.doesLimsRecognizeAllTubes(Arrays.asList("squid_barcode"));
        assertThat(result, is(true));
    }

    // TODO: enable when Mercury implementation is complete
    @Test(enabled = false, groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesFromSequel() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("sequel_barcode"))).andReturn(false);
        Map<String, TwoDBarcodedTube> sequelTubes = new HashMap<String, TwoDBarcodedTube>();
        sequelTubes.put("sequel_barcode", new TwoDBarcodedTube("sequel_barcode"));
        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("sequel_barcode"))).andReturn(sequelTubes);
        replayAll();

        boolean result = resource.doesLimsRecognizeAllTubes(Arrays.asList("sequel_barcode"));
        assertThat(result, is(true));
    }

    // TODO: enable when Mercury implementation is complete
    @Test(enabled = false, groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesInBothSquidAndSequel() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("good_barcode"))).andReturn(true);
        Map<String, TwoDBarcodedTube> sequelTubes = new HashMap<String, TwoDBarcodedTube>();
        sequelTubes.put("good_barcode", new TwoDBarcodedTube("good_barcode"));
        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("good_barcode"))).andReturn(sequelTubes);
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("bad_barcode"))).andReturn(false);
        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("bad_barcode"))).andReturn(new HashMap<String, TwoDBarcodedTube>());
        replayAll();

        boolean result1 = resource.doesLimsRecognizeAllTubes(Arrays.asList("good_barcode"));
        assertThat(result1, is(true));

        boolean result2 = resource.doesLimsRecognizeAllTubes(Arrays.asList("bad_barcode"));
        assertThat(result2, is(false));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesSplitBetweenSquidAndSequel() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("squid_barcode", "sequel_barcode"))).andReturn(false);
        Map<String, TwoDBarcodedTube> sequelTubes = new HashMap<String, TwoDBarcodedTube>();
        sequelTubes.put("sequel_barcode", new TwoDBarcodedTube("sequel_barcode"));
        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("squid_barcode", "sequel_barcode"))).andReturn(sequelTubes);
        replayAll();

        boolean result = resource.doesLimsRecognizeAllTubes(Arrays.asList("squid_barcode", "sequel_barcode"));
        // result is false because one system does not know about both tubes (TBD if this is correct behavior)
        assertThat(result, is(false));
    }

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByFlowcellBarcode() throws Exception {
        FlowcellDesignation designation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByFlowcellBarcode("good_barcode")).andReturn(designation);
        expect(mockResponseFactory.makeFlowcellDesignation(designation)).andReturn(new FlowcellDesignationType());
        replayAll();

        FlowcellDesignationType result = resource.findFlowcellDesignationByFlowcellBarcode("good_barcode");

        assertThat(result, notNullValue());

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchQpcrForTube() throws Exception {
        expect(mockThriftService.fetchQpcrForTube("barcode")).andReturn(1.23);
        replayAll();

        double result = resource.fetchQpcrForTube("barcode");
        assertThat(result, equalTo(1.23));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchQuantForTube() throws Exception {
        expect(mockThriftService.fetchQuantForTube("barcode", "test")).andReturn(1.23);
        replayAll();

        double result = resource.fetchQuantForTube("barcode", "test");
        assertThat(result, equalTo(1.23));

        verifyAll();
    }

    private void replayAll() {
        replay(mockThriftService, mockResponseFactory, mockTwoDBarcodedTubeDAO, mockStaticPlateDAO);
    }

    private void verifyAll() {
        verify(mockThriftService, mockResponseFactory, mockTwoDBarcodedTubeDAO, mockStaticPlateDAO);
    }
}
