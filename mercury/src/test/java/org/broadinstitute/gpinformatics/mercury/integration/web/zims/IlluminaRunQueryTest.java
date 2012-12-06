package org.broadinstitute.gpinformatics.mercury.integration.web.zims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.thrift.OfflineThriftService;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.inject.Alternative;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author breilly
 */
public class IlluminaRunQueryTest extends Arquillian {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(StubThriftService.class);
    }

    @Drone
    private FirefoxDriver driver;
//    private HtmlUnitDriver driver;

    @ArquillianResource
    private URL deploymentUrl;

    private IlluminaRunQueryPage page;

    @BeforeMethod(groups = TestGroups.UI)
    protected void setUp() throws Exception {
        if (deploymentUrl == null) {
            deploymentUrl = new URL("http://localhost:8080/");
        }
        page = new IlluminaRunQueryPage(driver, deploymentUrl);
    }

    @Test(groups = TestGroups.UI)
    public void testLoadQueryFormWebDriver() throws InterruptedException {
        page.get();

        page.enterRunName("120320_SL-HBN_0159_AFCC0GHCACXX");
        page.submitQueryAndWait();

        assertEquals(page.getRunName(), "120320_SL-HBN_0159_AFCC0GHCACXX");
        assertEquals(page.getRunBarcode(), "Run-123");
        assertEquals(page.getRunDate(), "05/11/2012 17:08");
        assertEquals(page.getSequencer(), "Sequencer 123");
        assertEquals(page.getSequencerModel(), "Test Sequencer");
        assertEquals(page.getFlowcellBarcode(), "Flowcell-123");
        assertEquals(page.getPaired(), "No");
        assertEquals(page.getFirstCycle(), "1");
        assertEquals(page.getFirstCycleReadLength(), "2");
        assertEquals(page.getLastCycle(), "3");
        assertEquals(page.getMolecularBarcodeCycle(), "4");
        assertEquals(page.getMolecularBarcodeLength(), "5");
        assertEquals(page.getNumLanes(), 4);
        for (int lane = 0; lane < 4; lane++) {
            assertEquals(page.getLaneNumber(lane), String.valueOf(lane + 1));
            assertEquals(page.getPrimer(lane), "PESP1+T");
            assertEquals(page.getNumLibraries(lane), "3");
        }

        assertEquals(page.getLaneDetailsHeader(), "Lane Details");
        assertEquals(page.getLaneDetailFirstCell(), "Select a lane");

        assertEquals(page.getNumReads(), 2);
        assertEquals(page.getReadType(0), "INDEX");
        assertEquals(page.getReadFirstCycle(0), "1");
        assertEquals(page.getReadLength(0), "10");
        assertEquals(page.getReadType(1), "TEMPLATE");
        assertEquals(page.getReadFirstCycle(1), "11");
        assertEquals(page.getReadLength(1), "20");

        assertEquals(page.getLibraryColumns(), Arrays.asList("", "Library Name", "Project", "Work Request", "Sample Alias", "GSSR Barcodes"));

        page.selectLaneAndWait(0);
        assertEquals(page.getLaneDetailsHeader(), "Lane 1 Details");
        assertEquals(page.getNumLibraryDatas(), 3);

        for (int i = 0; i < 3; i++) {
            int num = 100 + i;
            assertEquals(page.getLibraryData(i, "Library Name"), "Library-" + num);
            assertEquals(page.getLibraryData(i, "Project"), "Project-" + num);
            assertEquals(page.getLibraryData(i, "Work Request"), "1");
            assertEquals(page.getLibraryData(i, "Sample Alias"), "Sample-" + num);
            assertEquals(page.getLibraryData(i, "GSSR Barcodes"), num + ".0, " + num + ".1, " + num + ".2");
        }

        page.toggleColumn("Indexing Scheme");
        assertEquals(page.getLibraryColumns(), Arrays.asList("", "Library Name", "Project", "Work Request", "Indexing Scheme", "Sample Alias", "GSSR Barcodes"));
        for (int i = 0; i < 3; i++) {
            assertEquals(page.getLibraryData(i, "Indexing Scheme"), "IndexingScheme-" + (100 + i));
        }

        page.toggleColumn("Indexing Scheme");
        assertEquals(page.getLibraryColumns(), Arrays.asList("", "Library Name", "Project", "Work Request", "Sample Alias", "GSSR Barcodes"));
    }

    @Alternative
    public static class StubThriftService implements ThriftService {
        @Override
        public TZamboniRun fetchRun(String runName) {
            return OfflineThriftService.makeRun(runName, 4, 3);
        }

        @Override
        public List<LibraryData> fetchLibraryDetailsByTubeBarcode(List<String> tubeBarcodes, boolean includeWorkRequestDetails) {
            return null;
        }

        @Override
        public boolean doesSquidRecognizeAllLibraries(List<String> barcodes) {
            return false;
        }

        @Override
        public List<String> fetchMaterialTypesForTubeBarcodes(List<String> tubeBarcodes) {
            return null;
        }

        @Override
        public FlowcellDesignation findFlowcellDesignationByTaskName(String taskName) {
            return null;
        }

        @Override
        public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(String flowcellBarcode) {
            return null;
        }

        @Override
        public FlowcellDesignation findFlowcellDesignationByReagentBlockBarcode(String flowcellBarcode) {
            return null;
        }

        @Override
        public List<String> findImmediatePlateParents(String plateBarcode) {
            return null;
        }

        @Override
        public String fetchUserIdForBadgeId(String badgeId) {
            return null;
        }

        @Override
        public Map<String, Boolean> fetchParentRackContentsForPlate(String plateBarcode) {
            return null;
        }

        @Override
        public double fetchQpcrForTube(String tubeBarcode) {
            return 0;
        }

        @Override
        public double fetchQuantForTube(String tubeBarcode, String quantType) {
            return 0;
        }

        @Override
        //@Test
        public List<LibraryData> fetchLibraryDetailsByLibraryName(List<String> libraryNames) {
            List<LibraryData> libraryDataList = new ArrayList<LibraryData>();
            for (String libraryName : libraryNames) {
                LibraryData libraryData = new LibraryData();
                libraryData.setLibraryName(libraryName);
                libraryData.setLibraryNameIsSet(true);
                libraryDataList.add(libraryData);
            }

            return libraryDataList;
        }
    }
}
