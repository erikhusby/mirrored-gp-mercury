package org.broadinstitute.gpinformatics.infrastructure.bsp;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * JAXB DTOs for the {@code getsampledetails} webservice in BSP which provides a variety of data on a sample given the
 * Matrix barcode of the sample's receptacle.
 */
public class GetSampleDetails {
    /**
     * This wraps the sample details so that other information can be added in the future and to hold multiple sets.
     * Copied from BSP.
     */
    @SuppressWarnings("UnusedDeclaration")
    @XmlRootElement(name = "details")
    public static class Details implements Serializable {

        private String batchId;

        private SampleDetails sampleDetails;

        public String getBatchId() {
            return batchId;
        }

        public void setBatchId(String batchId) {
            this.batchId = batchId;
        }

        public SampleDetails getSampleDetails() {
            return sampleDetails;
        }

        public void setSampleDetails(SampleDetails sampleDetails) {
            this.sampleDetails = sampleDetails;
        }
    }

    /**
     * Information about a sample that is sent out as XML through web services used by Automation Engineering.
     * Copied from BSP.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class SampleDetails implements Serializable {

        private List<SampleInfo> sampleInfo;

        public List<SampleInfo> getSampleInfo() {
            return sampleInfo;
        }

        public void setSampleInfo(List<SampleInfo> sampleInfo) {
            this.sampleInfo = sampleInfo;
        }
    }

    /**
     * The details for a particular sample.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class SampleInfo implements Cloneable, Serializable {

        private String sampleId;

        private String wellPosition;

        private String manufacturerBarcode;

        private Float volume;

        private Float concentration;

        public String getSampleId() {
            return sampleId;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getWellPosition() {
            return wellPosition;
        }

        public void setWellPosition(String wellPosition) {
            this.wellPosition = wellPosition;
        }

        public String getManufacturerBarcode() {
            return manufacturerBarcode;
        }

        public void setManufacturerBarcode(String manufacturerBarcode) {
            this.manufacturerBarcode = manufacturerBarcode;
        }

        public Float getVolume() {
            return volume;
        }

        public void setVolume(Float volume) {
            this.volume = volume;
        }

        public Float getConcentration() {
            return concentration;
        }

        public void setConcentration(Float concentration) {
            this.concentration = concentration;
        }
    }
}
