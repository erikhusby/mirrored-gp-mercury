package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.bsp.client.rackscan.RackScannerConfig;
import org.broadinstitute.bsp.client.rackscan.RackScannerType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 */
public enum RackScanner {

    BSP_AB1("AB1", "Lab 5076 Abgene Scanner", RackScannerType.AGBENE, false, false,
            "192.168.2.89", 200L),
    BSP_5112_1_1("5112-1-1", "Lab 5112 Bench 1 Position 1", "rackscanner1", RackScannerType.ZAITH, false, false,
            "192.168.98.176", 2222L),
    BSP_5112_1_2("5112-1-2", "Lab 5112 Bench 1 Position 2", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.23", 2222L),
    BSP_5076_13("5076-13", "Lab 5076 Bench 13 (XL20)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.3.208", 2222L),
    BSP_5076_12("5076-12", "Lab 5076 Bench 12 (Pico)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.3.208", 2222L),
    BSP_5076_7("5076-7", "Lab 5076 Bench 7 (Tecan)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.145", 2222L),
    BSP_123_1("123-1", "Lab 123 Anteroom Scanner", "fuj1", RackScannerType.ZAITH, false, false,
            "192.168.97.235", 2222L),
    BSP_123_2("123-2", "Lab 123 Pre-PCR Scanner", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.3.125", 2222L),
    BSP_5076_14("5076-14", "Lab 5076 Bench 14", "fuj", RackScannerType.ZAITH, false, false,
            "69.173.120.119", 2222L),
    BSP_5076_6("5076-6", "Lab 5076 Bench 6 (Plating)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.102", 2222L),
    BSP_5076_13_2("5076-13-2", "Lab 5076 Bench 13 (Dory)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.237", 2222L),
    BSP_5076_13_3("5076-13-3", "Lab 5076 Bench 13 (Nemo)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.236", 2222L),
    BSP_5076_13_4("5076-13-4", "Lab 5076 Bench 13 (Bruce)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.235", 2222L),
    BSP_5076_13_5("5076-13-5", "Lab 5076 Bench 13 (Crush)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.97.245", 2222L);

    private RackScanner(String scannerUID, String scannerName, RackScannerType scannerType, boolean archived,
                        boolean withLinearScanner, String ipAddress, Long port) {
        this(scannerUID, scannerName, "", scannerType, archived, withLinearScanner, ipAddress, port);
    }

    private RackScanner(String scannerUID, String scannerName, String scannerInternalName,
                        RackScannerType scannerType, boolean archived, boolean withLinearScanner, String ipAddress,
                        Long port) {
        this.scannerUID = scannerUID;
        this.scannerName = scannerName;
        this.scannerInternalName = scannerInternalName;
        this.scannerType = scannerType;
        this.archived = archived;
        this.withLinearScanner = withLinearScanner;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    private String scannerUID;
    private String scannerName;
    private String scannerInternalName;
    private RackScannerType scannerType;
    private boolean archived;
    private boolean withLinearScanner;
    private String ipAddress;
    private Long port;

    public String getScannerName() {
        return scannerName;
    }

    private static final Map<RackScanner, RackScannerConfig> allConfigs = new HashMap<>();

    static {
        for (RackScanner rackScanner : values()) {
            allConfigs.put(rackScanner, new RackScannerConfig(rackScanner.scannerUID, rackScanner.scannerName,
                    rackScanner.scannerInternalName, rackScanner.scannerType, rackScanner.archived,
                    rackScanner.withLinearScanner, rackScanner.ipAddress, rackScanner.port));
        }
    }

    public RackScannerConfig getConfig() {
        return allConfigs.get(this);
    }

    public final static Comparator<RackScanner> BY_NAME = new Comparator<RackScanner>() {
        @Override
        public int compare(RackScanner lh, RackScanner rh) {

            return lh.getScannerName().compareTo(rh.getScannerName());
        }
    };
}
