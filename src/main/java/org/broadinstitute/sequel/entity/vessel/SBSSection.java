package org.broadinstitute.sequel.entity.vessel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A section is typically a set of wells in a plate, or positions in a rack.
 */
public enum SBSSection {
    ALL96("ALL96", Arrays.asList(
            VesselPosition.A01,
            VesselPosition.A02,
            VesselPosition.A03,
            VesselPosition.A04,
            VesselPosition.A05,
            VesselPosition.A06,
            VesselPosition.A07,
            VesselPosition.A08,
            VesselPosition.A09,
            VesselPosition.A10,
            VesselPosition.A11,
            VesselPosition.A12,
            VesselPosition.B01,
            VesselPosition.B02,
            VesselPosition.B03,
            VesselPosition.B04,
            VesselPosition.B05,
            VesselPosition.B06,
            VesselPosition.B07,
            VesselPosition.B08,
            VesselPosition.B09,
            VesselPosition.B10,
            VesselPosition.B11,
            VesselPosition.B12,
            VesselPosition.C01,
            VesselPosition.C02,
            VesselPosition.C03,
            VesselPosition.C04,
            VesselPosition.C05,
            VesselPosition.C06,
            VesselPosition.C07,
            VesselPosition.C08,
            VesselPosition.C09,
            VesselPosition.C10,
            VesselPosition.C11,
            VesselPosition.C12,
            VesselPosition.D01,
            VesselPosition.D02,
            VesselPosition.D03,
            VesselPosition.D04,
            VesselPosition.D05,
            VesselPosition.D06,
            VesselPosition.D07,
            VesselPosition.D08,
            VesselPosition.D09,
            VesselPosition.D10,
            VesselPosition.D11,
            VesselPosition.D12,
            VesselPosition.E01,
            VesselPosition.E02,
            VesselPosition.E03,
            VesselPosition.E04,
            VesselPosition.E05,
            VesselPosition.E06,
            VesselPosition.E07,
            VesselPosition.E08,
            VesselPosition.E09,
            VesselPosition.E10,
            VesselPosition.E11,
            VesselPosition.E12,
            VesselPosition.F01,
            VesselPosition.F02,
            VesselPosition.F03,
            VesselPosition.F04,
            VesselPosition.F05,
            VesselPosition.F06,
            VesselPosition.F07,
            VesselPosition.F08,
            VesselPosition.F09,
            VesselPosition.F10,
            VesselPosition.F11,
            VesselPosition.F12,
            VesselPosition.G01,
            VesselPosition.G02,
            VesselPosition.G03,
            VesselPosition.G04,
            VesselPosition.G05,
            VesselPosition.G06,
            VesselPosition.G07,
            VesselPosition.G08,
            VesselPosition.G09,
            VesselPosition.G10,
            VesselPosition.G11,
            VesselPosition.G12,
            VesselPosition.H01,
            VesselPosition.H02,
            VesselPosition.H03,
            VesselPosition.H04,
            VesselPosition.H05,
            VesselPosition.H06,
            VesselPosition.H07,
            VesselPosition.H08,
            VesselPosition.H09,
            VesselPosition.H10,
            VesselPosition.H11,
            VesselPosition.H12
    ));
    
    private String sectionName;
    private List<VesselPosition> wells = new ArrayList<VesselPosition>();

    SBSSection(String sectionName, List<VesselPosition> wells) {
        this.sectionName = sectionName;
        this.wells = wells;
    }

    public String getSectionName() {
        return sectionName;
    }

    public List<VesselPosition> getWells() {
        return wells;
    }
}
