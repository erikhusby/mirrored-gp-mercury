package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
})
@XmlRootElement(name = "sampleKitReceivedBean")
public class SampleKitReceivedBean {
    private List<String> result;
    private boolean success;
    private List<KitInfo> missingSamplesPerKit;
    private List<KitInfo> receivedSamplesPerKit;
    private List<TubeTypePerSample> tubeTypePerSample;
    private List<String> messages;

    public SampleKitReceivedBean() {
    }

    public SampleKitReceivedBean(boolean success) {
        this.success = success;
    }

    public void setResult(List<String> result) {
        this.result = result;
    }

    public List<String> getResult() {
        return result;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<KitInfo> getMissingSamplesPerKit() {
        return missingSamplesPerKit;
    }

    public void setMissingSamplesPerKit(
            List<KitInfo> missingSamplesPerKit) {
        if (this.missingSamplesPerKit == null) {
            this.missingSamplesPerKit = new ArrayList<>();
        }
        this.missingSamplesPerKit = missingSamplesPerKit;
    }

    public List<KitInfo> getReceivedSamplesPerKit() {
        return receivedSamplesPerKit;
    }

    public void setReceivedSamplesPerKit(
            List<KitInfo> receivedSamplesPerKit) {
        if (this.receivedSamplesPerKit == null) {
            this.receivedSamplesPerKit = new ArrayList<>();
        }
        this.receivedSamplesPerKit = receivedSamplesPerKit;
    }

    public List<TubeTypePerSample> getTubeTypePerSample() {
        if (this.tubeTypePerSample == null) {
            this.tubeTypePerSample = new ArrayList<>();
        }
        return tubeTypePerSample;
    }

    public void setTubeTypePerSample(
            List<TubeTypePerSample> tubeTypePerSample) {
        this.tubeTypePerSample = tubeTypePerSample;
    }

    public List<String> getMessages() {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "kitInfo", propOrder = {

    })
    public static class KitInfo {
        private String kitId;
        private List<String> samples;

        public KitInfo() {
        }

        public KitInfo(String kitId, List<String> samples) {
            this.kitId = kitId;
            this.samples = samples;
        }

        public String getKitId() {
            return kitId;
        }

        public List<String> getSamples() {
            return samples;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "tubeTypePerSample", propOrder = {

    })
    public static class TubeTypePerSample {
        private String sampleId;
        private String tubeType;

        public TubeTypePerSample() {
        }

        public TubeTypePerSample(String sampleId, String tubeType) {
            this.sampleId = sampleId;
            this.tubeType = tubeType;
        }

        public String getSampleId() {
            return sampleId;
        }

        public String getTubeType() {
            return tubeType;
        }
    }
}