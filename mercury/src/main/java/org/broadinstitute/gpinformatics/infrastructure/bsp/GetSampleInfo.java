package org.broadinstitute.gpinformatics.infrastructure.bsp;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * JAXB DTOs for the {@code getsampleifno} webservice in BSP which provides a variety of data on a sample given the
 * conainter barcode of the receptacle
 */
public class GetSampleInfo
{
    @SuppressWarnings("UnusedDeclaration")
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SampleInfos implements Serializable {

        private List<GetSampleInfo.SampleInfo> sampleInfos;

        public List<GetSampleInfo.SampleInfo> getSampleInfos() {
            if (sampleInfos == null) {
                sampleInfos = new ArrayList<>();
            }
            return sampleInfos;
        }

        public void setSampleInfos(List<GetSampleInfo.SampleInfo> sampleInfos) {
            this.sampleInfos = sampleInfos;
        }
    }

    /**
     * The details for a particular container.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class SampleInfo implements Cloneable, Serializable {
        private String sampleBarcode;
        private String position;

        public String getSampleBarcode() {
            return sampleBarcode;
        }

        public void setSampleBarcode(String sampleBarcode) {
            this.sampleBarcode = sampleBarcode;
        }

        public String getPosition() {
            return position;
        }

        public void setPosition(String position) {
            this.position = position;
        }
    }
}
