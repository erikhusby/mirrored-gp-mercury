package org.broadinstitute.sequel.entity.zims;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class DevExperimentData {
   
    private String experiment;

    private List<String> conditionChain;
    
}
