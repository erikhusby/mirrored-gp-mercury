package org.broadinstitute.gpinformatics.infrastructure.gap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="samples")
public class Samples {

    private List<GapSample> samples = new ArrayList<GapSample>();

    @XmlElement(name="sample")
    public List<GapSample> getSamples() {
        return samples;
    }

    public void setSamples(List<GapSample> samples) {
        this.samples = samples;
    }

}
    
