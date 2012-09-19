package org.broadinstitute.gpinformatics.mercury.infrastructure.gap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="samples")
public class Samples {

    private List<Sample> samples = new ArrayList<Sample>();

    @XmlElement(name="sample")
    public List<Sample> getSamples() {
        return samples;
    }

    public void setSamples(List<Sample> samples) {
        this.samples = samples;
    }

}
    
