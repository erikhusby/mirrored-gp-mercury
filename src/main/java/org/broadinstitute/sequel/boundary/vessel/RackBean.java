package org.broadinstitute.sequel.boundary.vessel;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to import racks from Squid
 */
@XmlRootElement
public class RackBean {
    public String barcode;
    public String lcSet;
    public List<TubeBean> tubeBeans = new ArrayList<TubeBean>();

    public RackBean(String barcode, String lcSet, List<TubeBean> tubeBeans) {
        this.barcode = barcode;
        this.lcSet = lcSet;
        this.tubeBeans = tubeBeans;
    }

    public RackBean() {
    }
}
