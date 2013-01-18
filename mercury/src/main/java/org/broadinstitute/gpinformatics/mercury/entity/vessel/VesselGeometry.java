package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import java.util.Iterator;
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
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H"}),

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
            new String[]{"TUBE1", "TUBE2", "TUBE3", "TUBE4", "TUBE5", "TUBE6", "TUBE7", "TUBE8"}),

    STRIP_TUBE_WELL(
            "1x1",
            new String[]{""},
            new String[]{""}),

    FLOWCELL(
            "1x8",
            new String[]{""},
            new String[]{"LANE1", "LANE2", "LANE3", "LANE4", "LANE5", "LANE6", "LANE7", "LANE8"});

    private final String name;
    private final String[] columnNames;
    private final String[] rowNames;
    private final Integer capacity;

    VesselGeometry(String name, String[] columnNames, String[] rowNames) {
        this.name = name;
        this.columnNames = columnNames;
        this.rowNames = rowNames;
        this.capacity = columnNames.length * rowNames.length;
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
}
