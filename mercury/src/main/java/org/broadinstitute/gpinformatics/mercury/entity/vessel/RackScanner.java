package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.bsp.client.rackscan.RackScannerConfig;
import org.broadinstitute.bsp.client.rackscan.RackScannerType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * RackScanner enum which holds all the connection info necessary to preform a rack scan.
 */
public enum RackScanner {

    BSP_AB1(RackScannerLab.BSP_5076, "AB1", "Lab 5076 Abgene Scanner", "192.168.2.89", 200L),
    BSP_5112_1_1(RackScannerLab.BSP_5112, "5112-1-1", "Lab 5112 Bench 1 Position 1", "rackscanner1",
            "192.168.98.176", 2222L),
    BSP_5112_1_2(RackScannerLab.BSP_5112, "5112-1-2", "Lab 5112 Bench 1 Position 2", "fuj", "192.168.98.23", 2222L),
    BSP_5076_13(RackScannerLab.BSP_5076, "5076-13", "Lab 5076 Bench 13 (XL20)", "fuj", "192.168.3.208", 2222L),
    BSP_5076_12(RackScannerLab.BSP_5076, "5076-12", "Lab 5076 Bench 12 (Pico)", "fuj", "192.168.3.208", 2222L),
    BSP_5076_7(RackScannerLab.BSP_5076, "5076-7", "Lab 5076 Bench 7 (Tecan)", "fuj", "192.168.98.145", 2222L),
    BSP_123_1(RackScannerLab.BSP_123, "123-1", "Lab 123 Anteroom Scanner", "fuj1", "192.168.97.235", 2222L),
    BSP_123_2(RackScannerLab.BSP_123, "123-2", "Lab 123 Pre-PCR Scanner", "fuj", "192.168.3.125", 2222L),
    BSP_5076_6(RackScannerLab.BSP_5076, "5076-6", "Lab 5076 Bench 6 (Plating)", "fuj", "192.168.98.102", 2222L),
    BSP_5076_13_2(RackScannerLab.BSP_5076, "5076-13-2", "Lab 5076 Bench 13 (Dory)", "fuj", "192.168.98.237", 2222L),
    BSP_5076_13_3(RackScannerLab.BSP_5076, "5076-13-3", "Lab 5076 Bench 13 (Nemo)", "fuj", "192.168.98.236", 2222L),
    BSP_5076_13_4(RackScannerLab.BSP_5076, "5076-13-4", "Lab 5076 Bench 13 (Bruce)", "fuj", "192.168.98.235", 2222L),
    BSP_5076_13_5(RackScannerLab.BSP_5076, "5076-13-5", "Lab 5076 Bench 13 (Crush)", "fuj", "192.168.97.245", 2222L),
    BSP_5081_1(RackScannerLab.BSP_5081, "5081-1", "Lab 5081 Freezer Scanner", "fuj", "192.168.97.245", 2222L);

    private static final boolean ACTIVE = false;
    private static final boolean NOT_LINEAR = false;

    private RackScanner(RackScannerLab rackScannerLab, String scannerUid, String scannerName, String ipAddress,
                        Long port) {
        this(rackScannerLab, scannerUid, scannerName, "", RackScannerType.AGBENE, ACTIVE, NOT_LINEAR, ipAddress, port);
    }

    private RackScanner(RackScannerLab rackScannerLab, String scannerUid, String scannerName,
                        String scannerInternalName, String ipAddress, Long port) {
        this(rackScannerLab, scannerUid, scannerName, scannerInternalName, RackScannerType.ZAITH, ACTIVE, NOT_LINEAR,
                ipAddress, port);
    }

    private RackScanner(RackScannerLab rackScannerLab, String scannerUid, String scannerName,
                        String scannerInternalName, RackScannerType scannerType, boolean archived,
                        boolean withLinearScanner, String ipAddress, Long port) {

        this.rackScannerLab = rackScannerLab;
        rackScannerConfig = new RackScannerConfig(scannerUid, scannerName,
                scannerInternalName, scannerType, archived,
                withLinearScanner, ipAddress, port);
    }

    private final RackScannerLab rackScannerLab;

    private RackScannerConfig rackScannerConfig;

    public String getScannerName() {
        return rackScannerConfig.getScannerName();
    }

    public RackScannerLab getRackScannerLab() {
        return rackScannerLab;
    }

    public String getName() {
        return name();
    }

    /**
     * Software which is utilizing this particular lab.
     */
    public static enum SoftwareSystem {
        BSP,
        CRSP_BSP,
        MERCURY
    }

    /**
     * Lab the rack scanner is in for sorting and selection purposes.
     */
    public static enum RackScannerLab {
        BSP_5112("BSP Lab 5112", SoftwareSystem.BSP),
        BSP_5076("BSP Lab 5076", SoftwareSystem.BSP),
        BSP_5081("BSP Lab 5081", SoftwareSystem.BSP),
        BSP_123("BSP Lab 123", SoftwareSystem.BSP);

        private final String labName;
        private final SoftwareSystem softwareSystem;

        RackScannerLab(String labName, SoftwareSystem softwareSystem) {
            this.labName = labName;
            this.softwareSystem = softwareSystem;
        }

        public String getLabName() {
            return labName;
        }

        public SoftwareSystem getSoftwareSystem() {
            return softwareSystem;
        }

        public static List<RackScannerLab> getLabsBySoftwareSystems(SoftwareSystem... softwareSystems) {
            List<SoftwareSystem> systems = Arrays.asList(softwareSystems);
            List<RackScannerLab> labs = new ArrayList<RackScannerLab>();
            for (RackScannerLab lab : values()) {
                if (systems.contains(lab.getSoftwareSystem())) {
                    labs.add(lab);
                }
            }
            return labs;
        }

        /** Allows us to use this enum as part of a JSP for the value of a option. */
        public String getName() {
            return name();
        }
    }

    /**
     * @return The config specific to the RackScanner.
     */
    public RackScannerConfig getRackScannerConfig() {
        return rackScannerConfig;
    }

    /**
     * Finds the configs by the software system sent in.
     *
     * @param softwareSystem Software system to search for.
     * @return List of RackScannerConfigs to used by that software.
     */
    public static List<RackScannerConfig> getRackScannerConfigsBySoftwareSystem(SoftwareSystem softwareSystem) {

        List<RackScannerConfig> configList = new ArrayList<RackScannerConfig>();

        for (RackScanner rackScanner : values()) {

            if (rackScanner.getRackScannerLab().getSoftwareSystem() == softwareSystem) {
                configList.add(rackScanner.getRackScannerConfig());
            }
        }
        return configList;
    }

    /**
     * Finds the configs by the software system sent in.
     *
     * @param rackScannerLab To search for a rack scanner within.
     * @return List of RackScannerConfigs within the lab.
     */
    public static List<RackScannerConfig> getRackScannerConfigsByLab(RackScannerLab rackScannerLab) {

        List<RackScannerConfig> configList = new ArrayList<RackScannerConfig>();

        for (RackScanner rackScanner : values()) {

            if (rackScanner.getRackScannerLab() == rackScannerLab) {
                configList.add(rackScanner.getRackScannerConfig());
            }
        }
        return configList;
    }

    public static List<RackScanner> getRackScannersByLab(RackScannerLab labToFilterBy) {
        List<RackScanner> rackScanners = new ArrayList<RackScanner>();
        for (RackScanner scanner : RackScanner.values()) {
            if (labToFilterBy == scanner.getRackScannerLab()) {
                rackScanners.add(scanner);
            }
        }
        return rackScanners;
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
