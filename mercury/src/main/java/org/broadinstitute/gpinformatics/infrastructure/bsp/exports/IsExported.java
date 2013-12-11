package org.broadinstitute.gpinformatics.infrastructure.bsp.exports;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Suite of DTOs to support the /exports/isExported webservice in BSP.
 */
public class IsExported {

    /**
     * This is a copy/paste of ExternalSystem in BSP, retaining the same mixed case names to facilitate
     * interoperability with that enum.
     */
    @XmlType
    @XmlEnum
    public enum ExternalSystem {
        GAP,
        Sequencing,
        External,
        Mercury
    }

    /**
     * Class representing export results for a single query barcode.
     */
    @XmlRootElement
    public static class ExportResult {

        private String barcode;

        private String error;

        private String notFound;

        private ExternalSystem exportDestination;

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public String getNotFound() {
            return notFound;
        }

        public void setNotFound(String notFound) {
            this.notFound = notFound;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public ExternalSystem getExportDestination() {
            return exportDestination;
        }

        public void setExportDestination(ExternalSystem exportDestination) {
            this.exportDestination = exportDestination;
        }

        /**
         * For JAX-RS runtime.
         */
        public ExportResult() {
        }

        public ExportResult(String barcode, ExternalSystem exportDestination) {
            this.barcode = barcode;
            this.exportDestination = exportDestination;
        }

        public boolean isError() {
            return error != null;
        }

        public boolean isExportedToSequencing() {
            return exportDestination == ExternalSystem.Sequencing;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ExportResult that = (ExportResult) o;

            return barcode.equals(that.barcode);
        }

        @Override
        public int hashCode() {
            return barcode.hashCode();
        }
    }

    /**
     * Class containing all export results for all query barcodes.
     */
    @XmlRootElement
    public static class ExportResults {

        private List<ExportResult> exportResult = new ArrayList<>();

        // This actually is used by the JAX-RS runtime.
        @SuppressWarnings("UnusedDeclaration")
        public ExportResults() {
        }

        public ExportResults(List<ExportResult> exportResult) {
            this.exportResult = exportResult;
        }

        public List<ExportResult> getExportResult() {
            return exportResult;
        }

        public void setExportResult(List<ExportResult> exportResult) {
            this.exportResult = exportResult;
        }
    }
}
