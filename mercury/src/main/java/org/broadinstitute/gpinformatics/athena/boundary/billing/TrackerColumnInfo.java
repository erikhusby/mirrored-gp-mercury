package org.broadinstitute.gpinformatics.athena.boundary.billing;

public class TrackerColumnInfo {
        private final BillableRef billableRef;
        private final int columnIndex;

        public TrackerColumnInfo(final BillableRef billableRef, final int columnIndex) {
            this.billableRef = billableRef;
            this.columnIndex = columnIndex;
        }

        public BillableRef getBillableRef() {
            return billableRef;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (null == o) return true;
            if (!(o instanceof TrackerColumnInfo)) return false;

            final TrackerColumnInfo that = (TrackerColumnInfo) o;

            if (columnIndex != that.columnIndex) return false;
            if (!billableRef.equals(that.billableRef)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = billableRef.hashCode();
            result = 31 * result + columnIndex;
            return result;
        }
}