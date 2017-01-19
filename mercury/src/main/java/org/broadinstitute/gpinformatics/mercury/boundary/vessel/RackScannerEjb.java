package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.bsp.client.rackscan.NetworkRackScanner;
import org.broadinstitute.bsp.client.rackscan.RackScan;
import org.broadinstitute.bsp.client.rackscan.RackScanner;
import org.broadinstitute.bsp.client.rackscan.RackScannerConfig;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.rackscan.SimulatorRackScanner;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilized for obtaining a rack scan (Map<position, 2d barcode>) from any sort of rack scanner (bsp or mercury).
 * This can also obtain the bsp sample ids if necessary from the returned rack scan.
 */
@Stateful
@RequestScoped
public class RackScannerEjb {

    @Inject
    private BSPSampleDataFetcher sampleDataFetcherService;

    /**
     * Scans a rack, does NOT include rack barcode in results
     *
     * @param rackScanner RackScanner to connect to and run.
     * @return Linked HashMap of position to scanned barcode.
     * @throws ScannerException
     */
    public LinkedHashMap<String, String> runRackScanner(RackScanner rackScanner) throws ScannerException {
        return runRackScanner(rackScanner, null, false);
    }

    /**
     * Scans a rack or runs a rack scan simulation using the data from specified file and excludes rack barcode
     *
     * @param rackScanner RackScanner to connect to and run.
     * @param simulationContent For a rack scan simulation, gets positions and barcodes from this reader.
     * @return Linked HashMap of position to scanned barcode.
     * @throws ScannerException
     */
    public LinkedHashMap<String, String> runRackScanner(RackScanner rackScanner, Reader simulationContent )
            throws ScannerException {
        return runRackScanner(rackScanner, simulationContent, false);
    }

    /**
     * Scans a rack or runs a rack scan simulation using the data from specified file and optionally includes rack barcode
     *
     * @param rackScanner RackScanner to connect to and run.
     * @param simulationContent For a rack scan simulation, gets positions and barcodes from this reader.
     * @param includeRackBarcode Optionally include rack barcode in result map, key = "rack"
     * @return Linked HashMap of position to scanned barcode.
     * @throws ScannerException
     */
    public LinkedHashMap<String, String> runRackScanner(RackScanner rackScanner, Reader simulationContent, boolean includeRackBarcode )
            throws ScannerException {
        RackScannerConfig config = rackScanner.getRackScannerConfig();
        NetworkRackScanner networkRackScanner = NetworkRackScanner.createNetworkRackScanner(config);
        if (networkRackScanner instanceof SimulatorRackScanner) {
            ((SimulatorRackScanner)networkRackScanner).setSimulationContent(simulationContent);
        }
        RackScan scanData = networkRackScanner.readRackScan();
        if( includeRackBarcode ) {
            scanData.getPositionData().put("rack", scanData.getLinearBarcode());
        }
        return scanData.getPositionData();
    }

    /**
     * Takes the received rack scan and obtains the BSP sample Ids.
     *
     * @param rackScan Rack scan to obtain the sample ids from.
     * @return List of BSP sample Ids found.
     */
    public List<String> obtainSampleIdsFromRackscan(LinkedHashMap<String, String> rackScan) {

        // Utilizes a service in BSP which takes any type of barcode and returns SampleInfo objects
        Map<String,GetSampleDetails.SampleInfo> sampleInfoMap =
                sampleDataFetcherService.fetchSampleDetailsByBarcode(rackScan.values());

        // Just return the sample ids.
        List<String> sampleIds = new ArrayList<>();
        for (GetSampleDetails.SampleInfo sampleInfo : sampleInfoMap.values()) {
            sampleIds.add(sampleInfo.getSampleId());
        }

        return sampleIds;
    }
}
