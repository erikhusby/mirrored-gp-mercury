package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "username",
        "registerNonBroadTubeBeans"
})
@XmlRootElement(name = "registerTubesBean")
public class RegisterNonBroadTubesBean {

    @XmlElement(required = true)
    private String username;

    @XmlElement(required = true)
    private List<RegisterNonBroadTubeBean>
            registerNonBroadTubeBeans;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<RegisterNonBroadTubeBean> getRegisterTubeBeans() {
        if (registerNonBroadTubeBeans == null) {
            registerNonBroadTubeBeans = new ArrayList<RegisterNonBroadTubeBean>();
        }
        return this.registerNonBroadTubeBeans;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "registerNonBroadTubeBean", propOrder = {

    })
    public static class RegisterNonBroadTubeBean {
        @XmlElement(required = true)
        @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
        @XmlSchemaType(name = "normalizedString")
        protected String sampleKey;

        @XmlElement(required = true)
        @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
        @XmlSchemaType(name = "normalizedString")
        protected String collaboratorSampleId;

        public String getSampleKey() {
            return sampleKey;
        }

        public void setSampleKey(String sampleKey) {
            this.sampleKey = sampleKey;
        }

        public String getCollaboratorSampleId() {
            return collaboratorSampleId;
        }

        public void setCollaboratorSampleId(String collaboratorSampleId) {
            this.collaboratorSampleId = collaboratorSampleId;
        }
    }
}
