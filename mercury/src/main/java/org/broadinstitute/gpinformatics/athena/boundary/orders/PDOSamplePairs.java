package org.broadinstitute.gpinformatics.athena.boundary.orders;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
// todo arz docs
public class PDOSamplePairs {

    private List<PDOSamplePair> pdoSamplePairs;

    public PDOSamplePairs() {}

    public List<PDOSamplePair> getPdoSamplePairs() {
        return pdoSamplePairs;
    }

    public void setPdoSamplePairs(List<PDOSamplePair> pdoSamplePairs) {
        this.pdoSamplePairs = pdoSamplePairs;
    }
}
