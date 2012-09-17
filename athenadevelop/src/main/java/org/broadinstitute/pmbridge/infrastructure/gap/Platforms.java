package org.broadinstitute.pmbridge.infrastructure.gap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="platforms")
public class Platforms {
    private List<Platform> platforms = new ArrayList<Platform>();

    @XmlElement(name="platform")
    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }
}
    
