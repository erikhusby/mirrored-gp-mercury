package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.List;

@ManagedBean
@ViewScoped
public class SampleListBean implements Serializable {
    private List<MercurySample> sampleList;
    private MercurySample selectedSample;
    private Boolean showPlasticView = false;

    public Boolean getShowPlasticView() {
        return showPlasticView;
    }

    public void setShowPlasticView(Boolean showPlasticView) {
        this.showPlasticView = showPlasticView;
    }

    public MercurySample getSelectedSample() {
        return selectedSample;
    }

    public void setSelectedSample(MercurySample selectedSample) {
        this.selectedSample = selectedSample;
    }

    public void updateSamples(List<MercurySample> sampleList) {
        this.sampleList = sampleList;
    }

    public List<MercurySample> getSampleList() {
        return sampleList;
    }

    public void setSampleList(List<MercurySample> sampleList) {
        this.sampleList = sampleList;
    }

    public void togglePlasticView(MercurySample sample) {
        selectedSample = sample;
        showPlasticView = !showPlasticView;
    }

    public String getOpenCloseValue(Boolean shown) {
        String value = "Open";
        if (shown) {
            value = "Close";
        }
        return value;
    }
}
