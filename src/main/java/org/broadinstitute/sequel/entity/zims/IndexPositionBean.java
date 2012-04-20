package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "IndexPosition")
@XmlAccessorType(XmlAccessType.FIELD)
public class IndexPositionBean {

    private String hint;

    public IndexPositionBean(IndexPosition thriftPosition) {
        if (thriftPosition == IndexPosition.A) {
            this.hint = "A";                    
        }
        else {
            throw new RuntimeException("SequeL cannot map index position " + thriftPosition);
        }
    }
    
}
