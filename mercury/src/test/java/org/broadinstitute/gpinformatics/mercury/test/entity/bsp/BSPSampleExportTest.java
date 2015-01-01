package org.broadinstitute.gpinformatics.mercury.test.entity.bsp;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.inject.Inject;
import java.util.Collection;

public class BSPSampleExportTest {

    public static final String masterSample1 = "SM-1111";
    public static final String masterSample2 = "SM-2222";

    @Inject
    BSPConfig bspConfig;

    @Inject
    private Log log;

    public static void runBSPExportTest(BSPPlatingReceipt bspReceipt, LabBatch labBatch) throws Exception {
        String ALIQUOT_LSID_PATTERN = "broadinstitute.org:bsp.prod.sample:Test Aliquot ";

        if (bspReceipt == null) {
            throw new IllegalArgumentException("Invalid BSP Plating Receipt ");
        }
        if (labBatch == null) {
            throw new IllegalArgumentException("Invalid Project Plan ");
        }

        //TODO .. asserts for size of 2 should be removed and check with LabBatch size.
        //LabBatch ?? projectPlan can have many starters .. but batched .. so probably plating should be by batch
//        labBatch.getProjectPlan().getPendingPlatingRequests().addAll(bspReceipt.getPlatingRequests());
//        Collection<Starter> starters = labBatch.getStarters();
//        Assert.assertNotNull(starters);
//        Assert.assertEquals(starters.size(), 2, "Project Plan should have 2 starters");
//        Assert.assertEquals(starters.size(), bspReceipt.getPlatingRequests().size(), "Project Plan should have same starters as BSP Plating Receipt");
        //for now hardcode
//        Map<String, StartingSample> aliquotSourceMap = new HashMap<String, StartingSample>();
        Collection<BSPPlatingRequest> bspPlatingRequests = bspReceipt.getPlatingRequests();
        String stockName = null;
        String aliquotLSID = null;
//        StartingSample bspStock = null;
        for (BSPPlatingRequest bspRequest : bspPlatingRequests) {
            stockName = bspRequest.getSampleName();
            String[] stockNameChunks = stockName.split("-"); //assuming stockName is in format SM-99999
            if (stockNameChunks.length > 1) {
                aliquotLSID = ALIQUOT_LSID_PATTERN + stockNameChunks[stockNameChunks.length - 1];
            } else {
                aliquotLSID = ALIQUOT_LSID_PATTERN + stockName;
            }

//            ProjectPlan projPlan = bspRequest.getAliquotParameters().getProjectPlan();
//            Assert.assertNotNull(projPlan);
//            Assert.assertNotNull(projPlan.getStarters());
//            Assert.assertEquals(2, projPlan.getStarters().size());
//            for (Starter starter : projPlan.getStarters()) {
//                if (starter.getLabel().equals(stockName)) {
//                    bspStock = (StartingSample) starter;
//                }
//            }

//            aliquotSourceMap.put(aliquotLSID, bspStock);
        }

        BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
//        List<LabVessel> bspAliquots = bspSampleFactory.receiveBSPAliquots(bspReceipt, aliquotSourceMap, null);

        //now iterate through these aliquots, do some asserts and see if we can navigate back to requests.
//        Assert.assertNotNull(bspAliquots);
//        Assert.assertEquals(bspAliquots.size(), starters.size(), "BSP Aliquot size should match Staters size");
//        for (Starter aliquot : bspAliquots) {
//            Assert.assertTrue(aliquot.getLabel().contains("Aliquot"));
            //check the source stock sample of each aliquot
//            BSPSampleAuthorityTwoDTube bspAliquot = (BSPSampleAuthorityTwoDTube) aliquot;
//            Project testProject = bspAliquot.getAllProjects().iterator().next();
            //Assert.assertEquals("BSPExportTestingProject", testProject.getProjectName());
//            Assert.assertEquals(labBatch.getProjectPlan().getProject().getProjectName(), testProject.getProjectName());
            //navigate from aliquot to ----> BSPStartingSample - !!
//            StartingSample stockAliquot = bspAliquot.getSampleInstances().iterator().next().getStartingSample();
//            Assert.assertNotNull(stockAliquot);
//            ProjectPlan projPlan = stockAliquot.getRootProjectPlan();
//            Assert.assertNotNull(projPlan);
//            Collection<Starter> starterStocks = projPlan.getStarters();
            //should have starters
//            Assert.assertNotNull(starterStocks);
            //Iterator<Starter> starterStocksIterator = starterStocks.iterator();
//            Assert.assertEquals(2, starterStocks.size());
//        }

        //iterate through Starters (BSPStartingSample) and make sure they all have aliquots
//        Collection<Starter> starterStocks = labBatch.getStarters();
        //should have starter
//        Assert.assertNotNull(starterStocks);
//        Iterator<Starter> starterStocksIterator = starterStocks.iterator();
//        Assert.assertEquals(bspAliquots.size(), starterStocks.size());
//        while (starterStocksIterator.hasNext()) {
//            Starter starter = starterStocksIterator.next();
//            LabVessel aliquot = labBatch.getProjectPlan().getAliquotForStarter(starter);
//            Assert.assertNotNull(aliquot);
//            Assert.assertEquals(true, aliquot.getLabel().contains("Aliquot"));
            //assumed stock is in format SM-9999
//            String[] stockChunks = starter.getLabel().split("-");
//            String stockNum = null;
//            if (stockChunks.length > 1) {
//                stockNum = stockChunks[stockChunks.length - 1];
//            } else {
//                stockNum = stockName;
//            }

            //aliquot label should contain stock number
//            Assert.assertEquals(true, aliquot.getLabel().contains(stockNum));
//        }

        //return bspAliquots ??
    }

}
