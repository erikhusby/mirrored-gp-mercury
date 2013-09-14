package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.bsp.client.rackscan.RackScannerConfig;
import org.broadinstitute.bsp.client.rackscan.RackScannerType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * RackScanner enum which holds all the connection info necessary to preform a rack scan.
 */
public enum RackScanner {

    BSP_AB1(RackScannerLab.BSP_5076, "AB1", "Lab 5076 Abgene Scanner", RackScannerType.AGBENE, false, false,
            "192.168.2.89", 200L),
    BSP_5112_1_1(RackScannerLab.BSP_5112, "5112-1-1", "Lab 5112 Bench 1 Position 1", "rackscanner1", RackScannerType.ZAITH, false, false,
            "192.168.98.176", 2222L),
    BSP_5112_1_2(RackScannerLab.BSP_5112, "5112-1-2", "Lab 5112 Bench 1 Position 2", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.23", 2222L),
    BSP_5076_13(RackScannerLab.BSP_5076, "5076-13", "Lab 5076 Bench 13 (XL20)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.3.208", 2222L),
    BSP_5076_12(RackScannerLab.BSP_5076, "5076-12", "Lab 5076 Bench 12 (Pico)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.3.208", 2222L),
    BSP_5076_7(RackScannerLab.BSP_5076, "5076-7", "Lab 5076 Bench 7 (Tecan)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.145", 2222L),
    BSP_123_1(RackScannerLab.BSP_123, "123-1", "Lab 123 Anteroom Scanner", "fuj1", RackScannerType.ZAITH, false, false,
            "192.168.97.235", 2222L),
    BSP_123_2(RackScannerLab.BSP_123, "123-2", "Lab 123 Pre-PCR Scanner", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.3.125", 2222L),
    BSP_5076_14(RackScannerLab.BSP_5076, "5076-14", "Lab 5076 Bench 14", "fuj", RackScannerType.ZAITH, false, false,
            "69.173.120.119", 2222L),
    BSP_5076_6(RackScannerLab.BSP_5076, "5076-6", "Lab 5076 Bench 6 (Plating)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.102", 2222L),
    BSP_5076_13_2(RackScannerLab.BSP_5076, "5076-13-2", "Lab 5076 Bench 13 (Dory)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.237", 2222L),
    BSP_5076_13_3(RackScannerLab.BSP_5076, "5076-13-3", "Lab 5076 Bench 13 (Nemo)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.236", 2222L),
    BSP_5076_13_4(RackScannerLab.BSP_5076, "5076-13-4", "Lab 5076 Bench 13 (Bruce)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.98.235", 2222L),
    BSP_5076_13_5(RackScannerLab.BSP_5076, "5076-13-5", "Lab 5076 Bench 13 (Crush)", "fuj", RackScannerType.ZAITH, false, false,
            "192.168.97.245", 2222L);

    private RackScanner(RackScannerLab rackScannerLab, String scannerUID, String scannerName,
                        RackScannerType scannerType, boolean archived, boolean withLinearScanner, String ipAddress,
                        Long port) {
        this(rackScannerLab, scannerUID, scannerName, "", scannerType, archived, withLinearScanner, ipAddress, port);
    }

    private RackScanner(RackScannerLab rackScannerLab, String scannerUID, String scannerName,
                        String scannerInternalName, RackScannerType scannerType, boolean archived,
                        boolean withLinearScanner, String ipAddress, Long port) {

        this.scannerUID = scannerUID;
        this.scannerName = scannerName;
        this.scannerInternalName = scannerInternalName;
        this.scannerType = scannerType;
        this.archived = archived;
        this.withLinearScanner = withLinearScanner;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    private RackScannerLab rackScannerLab;
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

    public RackScannerLab getRackScannerLab() {
        return rackScannerLab;
    }

    /**
     * Lab the rack scanner is in for sorting and selection purposes.
     */
    public static enum RackScannerLab {
        BSP_5112("BSP Lab 5112"),
        BSP_5076("BSP Lab 5076"),
        BSP_123("BSP Lab 123");

        private final String labName;

        RackScannerLab(String labName) {
            this.labName = labName;
        }

        public String getLabName() {
            return labName;
        }
    }

    /** Map containing all the rack scanners as the key, and the configs as the value. */
    private static final Map<RackScanner, RackScannerConfig> allConfigs = new HashMap<>();

    // Preps a config for each rackscanner.
    static {
        for (RackScanner rackScanner : values()) {
            allConfigs.put(rackScanner, new RackScannerConfig(rackScanner.scannerUID, rackScanner.scannerName,
                    rackScanner.scannerInternalName, rackScanner.scannerType, rackScanner.archived,
                    rackScanner.withLinearScanner, rackScanner.ipAddress, rackScanner.port));
        }
    }

    /**
     * @return The config specific to the RackScanner.
     */
    public RackScannerConfig getConfig() {
        return allConfigs.get(this);
    }

    /**
     * Comparator to allow the rack scanners to be sorted by lab then scanner name.
     */
    public final static Comparator<RackScanner> BY_NAME = new Comparator<RackScanner>() {
        @Override
        public int compare(RackScanner lh, RackScanner rh) {

            if (lh.getRackScannerLab() != rh.getRackScannerLab()) {
                return lh.getRackScannerLab().compareTo(rh.getRackScannerLab());
            }

            return lh.getScannerName().compareTo(rh.getScannerName());
        }
    };
}
