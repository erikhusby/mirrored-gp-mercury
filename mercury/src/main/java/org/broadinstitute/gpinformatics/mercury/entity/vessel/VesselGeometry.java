package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Row / column geometry for vessels
 */
public enum VesselGeometry {
    TUBE(
            "1x1",
            new String[]{""},
            new String[]{""}),

    WELL(
            "1x1",
            new String[]{""},
            new String[]{""}),

    G12x8(
            "12 x 8",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08, VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06, VesselPosition.B07, VesselPosition.B08, VesselPosition.B09, VesselPosition.B10, VesselPosition.B11, VesselPosition.B12,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06, VesselPosition.C07, VesselPosition.C08, VesselPosition.C09, VesselPosition.C10, VesselPosition.C11, VesselPosition.C12,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06, VesselPosition.D07, VesselPosition.D08, VesselPosition.D09, VesselPosition.D10, VesselPosition.D11, VesselPosition.D12,
                    VesselPosition.E01, VesselPosition.E02, VesselPosition.E03, VesselPosition.E04, VesselPosition.E05, VesselPosition.E06, VesselPosition.E07, VesselPosition.E08, VesselPosition.E09, VesselPosition.E10, VesselPosition.E11, VesselPosition.E12,
                    VesselPosition.F01, VesselPosition.F02, VesselPosition.F03, VesselPosition.F04, VesselPosition.F05, VesselPosition.F06, VesselPosition.F07, VesselPosition.F08, VesselPosition.F09, VesselPosition.F10, VesselPosition.F11, VesselPosition.F12,
                    VesselPosition.G01, VesselPosition.G02, VesselPosition.G03, VesselPosition.G04, VesselPosition.G05, VesselPosition.G06, VesselPosition.G07, VesselPosition.G08, VesselPosition.G09, VesselPosition.G10, VesselPosition.G11, VesselPosition.G12,
                    VesselPosition.H01, VesselPosition.H02, VesselPosition.H03, VesselPosition.H04, VesselPosition.H05, VesselPosition.H06, VesselPosition.H07, VesselPosition.H08, VesselPosition.H09, VesselPosition.H10, VesselPosition.H11, VesselPosition.H12
            }),

    G24x16(
            "24 x 16",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08, VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06, VesselPosition.B07, VesselPosition.B08, VesselPosition.B09, VesselPosition.B10, VesselPosition.B11, VesselPosition.B12,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06, VesselPosition.C07, VesselPosition.C08, VesselPosition.C09, VesselPosition.C10, VesselPosition.C11, VesselPosition.C12,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06, VesselPosition.D07, VesselPosition.D08, VesselPosition.D09, VesselPosition.D10, VesselPosition.D11, VesselPosition.D12,
                    VesselPosition.E01, VesselPosition.E02, VesselPosition.E03, VesselPosition.E04, VesselPosition.E05, VesselPosition.E06, VesselPosition.E07, VesselPosition.E08, VesselPosition.E09, VesselPosition.E10, VesselPosition.E11, VesselPosition.E12,
                    VesselPosition.F01, VesselPosition.F02, VesselPosition.F03, VesselPosition.F04, VesselPosition.F05, VesselPosition.F06, VesselPosition.F07, VesselPosition.F08, VesselPosition.F09, VesselPosition.F10, VesselPosition.F11, VesselPosition.F12,
                    VesselPosition.G01, VesselPosition.G02, VesselPosition.G03, VesselPosition.G04, VesselPosition.G05, VesselPosition.G06, VesselPosition.G07, VesselPosition.G08, VesselPosition.G09, VesselPosition.G10, VesselPosition.G11, VesselPosition.G12,
                    VesselPosition.H01, VesselPosition.H02, VesselPosition.H03, VesselPosition.H04, VesselPosition.H05, VesselPosition.H06, VesselPosition.H07, VesselPosition.H08, VesselPosition.H09, VesselPosition.H10, VesselPosition.H11, VesselPosition.H12,
                    VesselPosition.I01, VesselPosition.I02, VesselPosition.I03, VesselPosition.I04, VesselPosition.I05, VesselPosition.I06, VesselPosition.I07, VesselPosition.I08, VesselPosition.I09, VesselPosition.I10, VesselPosition.I11, VesselPosition.I12,
                    VesselPosition.J01, VesselPosition.J02, VesselPosition.J03, VesselPosition.J04, VesselPosition.J05, VesselPosition.J06, VesselPosition.J07, VesselPosition.J08, VesselPosition.J09, VesselPosition.J10, VesselPosition.J11, VesselPosition.J12,
                    VesselPosition.K01, VesselPosition.K02, VesselPosition.K03, VesselPosition.K04, VesselPosition.K05, VesselPosition.K06, VesselPosition.K07, VesselPosition.K08, VesselPosition.K09, VesselPosition.K10, VesselPosition.K11, VesselPosition.K12,
                    VesselPosition.L01, VesselPosition.L02, VesselPosition.L03, VesselPosition.L04, VesselPosition.L05, VesselPosition.L06, VesselPosition.L07, VesselPosition.L08, VesselPosition.L09, VesselPosition.L10, VesselPosition.L11, VesselPosition.L12,
                    VesselPosition.M01, VesselPosition.M02, VesselPosition.M03, VesselPosition.M04, VesselPosition.M05, VesselPosition.M06, VesselPosition.M07, VesselPosition.M08, VesselPosition.M09, VesselPosition.M10, VesselPosition.M11, VesselPosition.M12,
                    VesselPosition.N01, VesselPosition.N02, VesselPosition.N03, VesselPosition.N04, VesselPosition.N05, VesselPosition.N06, VesselPosition.N07, VesselPosition.N08, VesselPosition.N09, VesselPosition.N10, VesselPosition.N11, VesselPosition.N12,
                    VesselPosition.O01, VesselPosition.O02, VesselPosition.O03, VesselPosition.O04, VesselPosition.O05, VesselPosition.O06, VesselPosition.O07, VesselPosition.O08, VesselPosition.O09, VesselPosition.O10, VesselPosition.O11, VesselPosition.O12,
                    VesselPosition.P01, VesselPosition.P02, VesselPosition.P03, VesselPosition.P04, VesselPosition.P05, VesselPosition.P06, VesselPosition.P07, VesselPosition.P08, VesselPosition.P09, VesselPosition.P10, VesselPosition.P11, VesselPosition.P12,
            }),

    G8x6(
            "8 x 6",
            new String[] {"01", "02", "03", "04", "05", "06", "07", "08"},
            new String[] {"A", "B", "C", "D", "E", "F"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08,
                    VesselPosition.B01, VesselPosition.B02, VesselPosition.B03, VesselPosition.B04, VesselPosition.B05, VesselPosition.B06, VesselPosition.B07, VesselPosition.B08,
                    VesselPosition.C01, VesselPosition.C02, VesselPosition.C03, VesselPosition.C04, VesselPosition.C05, VesselPosition.C06, VesselPosition.C07, VesselPosition.C08,
                    VesselPosition.D01, VesselPosition.D02, VesselPosition.D03, VesselPosition.D04, VesselPosition.D05, VesselPosition.D06, VesselPosition.D07, VesselPosition.D08,
                    VesselPosition.E01, VesselPosition.E02, VesselPosition.E03, VesselPosition.E04, VesselPosition.E05, VesselPosition.E06, VesselPosition.E07, VesselPosition.E08,
                    VesselPosition.F01, VesselPosition.F02, VesselPosition.F03, VesselPosition.F04, VesselPosition.F05, VesselPosition.F06, VesselPosition.F07, VesselPosition.F08,
            }),

    SAGE_CASSETTE(
            "Sage Cassette",
            new String[]{"01"},
            new String[]{"A", "C", "E", "G"},
	    new VesselPosition[]{VesselPosition.A01, VesselPosition.C01, VesselPosition.E01, VesselPosition.G01}),

    FLUIDIGM_48_48(
            "Fluidigm 48.48 Chip",
            new String[]{"04", "05", "06"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"},
            new VesselPosition[]{
                    VesselPosition.A04, VesselPosition.A05, VesselPosition.A06,
                    VesselPosition.B04, VesselPosition.B05, VesselPosition.B06,
                    VesselPosition.C04, VesselPosition.C05, VesselPosition.C06,
                    VesselPosition.D04, VesselPosition.D05, VesselPosition.D06,
                    VesselPosition.E04, VesselPosition.E05, VesselPosition.E06,
                    VesselPosition.F04, VesselPosition.F05, VesselPosition.F06,
                    VesselPosition.G04, VesselPosition.G05, VesselPosition.G06,
                    VesselPosition.H04, VesselPosition.H05, VesselPosition.H06,
                    VesselPosition.I04, VesselPosition.I05, VesselPosition.I06,
                    VesselPosition.J04, VesselPosition.J05, VesselPosition.J06,
                    VesselPosition.K04, VesselPosition.K05, VesselPosition.K06,
                    VesselPosition.L04, VesselPosition.L05, VesselPosition.L06,
                    VesselPosition.M04, VesselPosition.M05, VesselPosition.M06,
                    VesselPosition.N04, VesselPosition.N05, VesselPosition.N06,
                    VesselPosition.O04, VesselPosition.O05, VesselPosition.O06,
                    VesselPosition.P04, VesselPosition.P05, VesselPosition.P06,
            }),


    STRIP_TUBE(
            "1x8",
            new String[]{""},
            new String[]{"TUBE1", "TUBE2", "TUBE3", "TUBE4", "TUBE5", "TUBE6", "TUBE7", "TUBE8"},
            new VesselPosition[]{VesselPosition.TUBE1, VesselPosition.TUBE2, VesselPosition.TUBE3, VesselPosition.TUBE4, VesselPosition.TUBE5, VesselPosition.TUBE6, VesselPosition.TUBE7, VesselPosition.TUBE8}),

    STRIP_TUBE_WELL(
            "1x1",
            new String[]{""},
            new String[]{""}),

    FLOWCELL1x8(
            "1x8",
            new String[]{""},
            new String[]{"LANE1", "LANE2", "LANE3", "LANE4", "LANE5", "LANE6", "LANE7", "LANE8"},
            new VesselPosition[]{VesselPosition.LANE1, VesselPosition.LANE2, VesselPosition.LANE3, VesselPosition.LANE4, VesselPosition.LANE5, VesselPosition.LANE6, VesselPosition.LANE7, VesselPosition.LANE8}),

    FLOWCELL1x2(
            "1x2",
            new String[]{""},
            new String[]{"LANE1", "LANE2"},
            new VesselPosition[]{VesselPosition.LANE1, VesselPosition.LANE2}),

    FLOWCELL1x1(
            "1x1",
            new String[]{""},
            new String[]{"LANE1"},
            new VesselPosition[]{VesselPosition.LANE1}),

    MISEQ_REAGENT_KIT(
            "MiSeq Reagent Kit",
            new String[]{"04"},
            new String[]{"D"},
            new VesselPosition[]{VesselPosition.D04}),


    RUN_CHAMBER(
                "1x1",
                new String[]{""},
                new String[]{""}),

    G32x1(
            "32x1",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32"},
            new String[]{"A"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08,
                    VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12, VesselPosition.A13, VesselPosition.A14, VesselPosition.A15, VesselPosition.A16,
                    VesselPosition.A17, VesselPosition.A18, VesselPosition.A19, VesselPosition.A20, VesselPosition.A21, VesselPosition.A22, VesselPosition.A23, VesselPosition.A24,
                    VesselPosition.A25, VesselPosition.A26, VesselPosition.A27, VesselPosition.A28, VesselPosition.A29, VesselPosition.A30, VesselPosition.A31, VesselPosition.A32,
    }),

    G24x1(
            "24x1",
            new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"},
            new String[]{"A"},
            new VesselPosition[]{
                    VesselPosition.A01, VesselPosition.A02, VesselPosition.A03, VesselPosition.A04, VesselPosition.A05, VesselPosition.A06, VesselPosition.A07, VesselPosition.A08,
                    VesselPosition.A09, VesselPosition.A10, VesselPosition.A11, VesselPosition.A12, VesselPosition.A13, VesselPosition.A14, VesselPosition.A15, VesselPosition.A16,
                    VesselPosition.A17, VesselPosition.A18, VesselPosition.A19, VesselPosition.A20, VesselPosition.A21, VesselPosition.A22, VesselPosition.A23, VesselPosition.A24
            });

    private final String name;
    private final String[] columnNames;
    private final String[] rowNames;
    private final Integer capacity;
    private VesselPosition[] vesselPositions;
    private Map<VesselPosition, RowColumn> mapVesselPositionToRowColumn = new HashMap<>();

    public class RowColumn implements Serializable {
        private int row;
        private int column;

        public RowColumn(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }
    }

    VesselGeometry(String name, String[] columnNames, String[] rowNames) {
        this.name = name;
        this.columnNames = columnNames;
        this.rowNames = rowNames;
        this.capacity = columnNames.length * rowNames.length;
    }

    VesselGeometry(String name, String[] columnNames, String[] rowNames, VesselPosition[] vesselPositions) {
        this(name, columnNames, rowNames);
        this.vesselPositions = vesselPositions;
        int columnIndex = 0;
        int rowIndex= 0;

        for (VesselPosition vesselPosition : vesselPositions) {
            mapVesselPositionToRowColumn.put(vesselPosition, new RowColumn(rowIndex + 1, columnIndex + 1));
            columnIndex++;
            if(columnIndex >= columnNames.length) {
                columnIndex = 0;
                rowIndex++;
            }
        }
    }

    public String getName() {
        return name;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return rowNames.length;
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public Integer getCapacity(){
        return capacity;
    }

    public Iterator<String> getPositionNames() {
        return new Iterator<String>() {
            private int columnIndex = 0;
            private int rowIndex= 0;

            @Override
            public boolean hasNext() {
                return columnIndex < columnNames.length && rowIndex < rowNames.length;
            }

            @Override
            public String next() {
                if (! hasNext()) {
                    throw new NoSuchElementException();
                }
                String next = rowNames[rowIndex] + columnNames[columnIndex];
                columnIndex++;
                if(columnIndex >= columnNames.length) {
                    columnIndex = 0;
                    rowIndex++;
                }
                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        };
    }

    public VesselPosition[] getVesselPositions() {
        return vesselPositions;
    }

    public RowColumn getRowColumnForVesselPosition(VesselPosition vesselPosition) {
        return mapVesselPositionToRowColumn.get(vesselPosition);
    }
}
