package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;

@ManagedBean
@ViewScoped
public class SampleListBean implements Serializable {
    private MercurySample selectedSample;

    public MercurySample getSelectedSample() {
        return selectedSample;
    }

    public void setSelectedSample(MercurySample selectedSample) {
        this.selectedSample = selectedSample;
    }
}
