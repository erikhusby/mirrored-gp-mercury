package org.broadinstitute.sequel.entity.vessel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A section is typically a set of wells in a plate, or positions in a rack.
 */
public enum SBSSection {
    ALL96("ALL96", Arrays.asList(
            new WellName("A01"),
            new WellName("A02"),
            new WellName("A03"),
            new WellName("A04"),
            new WellName("A05"),
            new WellName("A06"),
            new WellName("A07"),
            new WellName("A08"),
            new WellName("A09"),
            new WellName("A10"),
            new WellName("A11"),
            new WellName("A12"),
            new WellName("B01"),
            new WellName("B02"),
            new WellName("B03"),
            new WellName("B04"),
            new WellName("B05"),
            new WellName("B06"),
            new WellName("B07"),
            new WellName("B08"),
            new WellName("B09"),
            new WellName("B10"),
            new WellName("B11"),
            new WellName("B12"),
            new WellName("C01"),
            new WellName("C02"),
            new WellName("C03"),
            new WellName("C04"),
            new WellName("C05"),
            new WellName("C06"),
            new WellName("C07"),
            new WellName("C08"),
            new WellName("C09"),
            new WellName("C10"),
            new WellName("C11"),
            new WellName("C12"),
            new WellName("D01"),
            new WellName("D02"),
            new WellName("D03"),
            new WellName("D04"),
            new WellName("D05"),
            new WellName("D06"),
            new WellName("D07"),
            new WellName("D08"),
            new WellName("D09"),
            new WellName("D10"),
            new WellName("D11"),
            new WellName("D12"),
            new WellName("E01"),
            new WellName("E02"),
            new WellName("E03"),
            new WellName("E04"),
            new WellName("E05"),
            new WellName("E06"),
            new WellName("E07"),
            new WellName("E08"),
            new WellName("E09"),
            new WellName("E10"),
            new WellName("E11"),
            new WellName("E12"),
            new WellName("F01"),
            new WellName("F02"),
            new WellName("F03"),
            new WellName("F04"),
            new WellName("F05"),
            new WellName("F06"),
            new WellName("F07"),
            new WellName("F08"),
            new WellName("F09"),
            new WellName("F10"),
            new WellName("F11"),
            new WellName("F12"),
            new WellName("G01"),
            new WellName("G02"),
            new WellName("G03"),
            new WellName("G04"),
            new WellName("G05"),
            new WellName("G06"),
            new WellName("G07"),
            new WellName("G08"),
            new WellName("G09"),
            new WellName("G10"),
            new WellName("G11"),
            new WellName("G12"),
            new WellName("H01"),
            new WellName("H02"),
            new WellName("H03"),
            new WellName("H04"),
            new WellName("H05"),
            new WellName("H06"),
            new WellName("H07"),
            new WellName("H08"),
            new WellName("H09"),
            new WellName("H10"),
            new WellName("H11"),
            new WellName("H12")
    ));
    
    private String sectionName;
    private List<WellName> wells = new ArrayList<WellName>();

    SBSSection(String sectionName, List<WellName> wells) {
        this.sectionName = sectionName;
        this.wells = wells;
    }

    public String getSectionName() {
        return sectionName;
    }

    public List<WellName> getWells() {
        return wells;
    }
}
