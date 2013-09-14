package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.bsp.client.rackscan.NetworkRackScanner;
import org.broadinstitute.bsp.client.rackscan.RackScannerConfig;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.rackscan.abgene.AbgeneNetworkRackScanner;
import org.broadinstitute.bsp.client.rackscan.zaith.ZaithNetworkRackScanner;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackScanner;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.LinkedHashMap;

/**
 */
@Stateful
@RequestScoped
public class RackScannerEjb {

    public LinkedHashMap<String, String> runRackScanner(RackScanner rackScanner) throws ScannerException {

        RackScannerConfig config = rackScanner.getConfig();

        NetworkRackScanner networkRackScanner;

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
}
