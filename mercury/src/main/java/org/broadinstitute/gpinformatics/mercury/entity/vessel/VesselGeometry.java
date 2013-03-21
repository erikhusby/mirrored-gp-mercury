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
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"}),

    SAGE_CASSETTE(
            "Sage Cassette",
            new String[]{"01"},
            new String[]{"A", "C", "E", "G"}),

    FLUIDIGM_48_48(
            "Fluidigm 48.48 Chip",
            new String[]{"04", "05", "06"},
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"}),

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

    RUN_CHAMBER(
                "1x1",
                new String[]{""},
                new String[]{""});

    private final String name;
    private final String[] columnNames;
    private final String[] rowNames;
    private final Integer capacity;
    private VesselPosition[] vesselPositions;
    private Map<VesselPosition, RowColumn> mapVesselPositionToRowColumn = new HashMap<VesselPosition, RowColumn>();

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
