package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

/**
 * Plateable represents something that has the potential to be plated into a PlatingArray
 */
public interface Plateable {

    public String getSampleId();

    public Well getSpecifiedWell();

    public String getPlatingQuote();

    public Float getVolume();

    public Float getConcentration();

    public enum Order {
        ROW,
        COLUMN
    }


    public enum Size {

        WELLS_96(8, 12),
        WELLS_384(16, 24);

        private int rowCount;

        private int columnCount;

        Size(int rowCount, int columnCount) {
            this.rowCount = rowCount;
            this.columnCount = columnCount;
        }

        public int getRowCount() {
            return rowCount;
        }

        public int getColumnCount() {
            return columnCount;
        }
    }
}
