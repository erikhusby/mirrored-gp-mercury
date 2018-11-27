package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.List;

@XmlRootElement
@XmlType
@XmlAccessorType
public class SampleKitInfos implements Serializable {

    @XmlElement(name = "sampleKitResource_SampleKitInfoes")
    private List<SampleKitInfo> sampleKitInfoList;

    public List<SampleKitInfo> getSampleKitInfoList() {
        return sampleKitInfoList;
    }

    public void setSampleKitInfoList(
            List<SampleKitInfo> sampleKitInfoList) {
        this.sampleKitInfoList = sampleKitInfoList;
    }
}
