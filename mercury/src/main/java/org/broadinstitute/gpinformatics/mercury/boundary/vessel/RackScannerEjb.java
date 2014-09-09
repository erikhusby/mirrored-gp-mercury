package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.bsp.client.rackscan.NetworkRackScanner;
import org.broadinstitute.bsp.client.rackscan.RackScanner;
import org.broadinstitute.bsp.client.rackscan.RackScannerConfig;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.rackscan.abgene.AbgeneNetworkRackScanner;
import org.broadinstitute.bsp.client.rackscan.zaith.ZaithNetworkRackScanner;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
    private SampleDataFetcher sampleDataFetcherService;

    /**
     * Based upon the RackScanner selected, this runs the rack scan and returns a linked HashMap of position to barcode
     *
     * @param rackScanner RackScanner to connect and run.
     * @return Linked HashMap of position to scanned barcode.
     * @throws ScannerException
     */
    public LinkedHashMap<String, String> runRackScanner(RackScanner rackScanner) throws ScannerException {

        RackScannerConfig config = rackScanner.getRackScannerConfig();

        NetworkRackScanner networkRackScanner;

        // Based on the selected scanner, create the rack scanner object
        switch (config.getScannerType()) {
            case AGBENE:
                networkRackScanner = new AbgeneNetworkRackScanner(config.getIpAddress(), config.getPort());
                break;
            case ZAITH:
                networkRackScanner = new ZaithNetworkRackScanner(config.getIpAddress(), config.getPort(),
                                                                 config.getScannerInternalName());
                break;
            default:
                throw new ScannerException("Failed to find the proper scanner.");
        }

        return networkRackScanner.readRackScan().getPositionData();
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
