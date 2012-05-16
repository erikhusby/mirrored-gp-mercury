package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

//@XmlRootElement(name = "IndexPosition")
public class IndexPositionBean {

    //@XmlElement(name = "hint")
    private String hint;

    public IndexPositionBean() {}

    public IndexPositionBean(IndexPosition thriftPosition) {
        
        if (thriftPosition == IndexPosition.A) {
            this.hint = "A";                    
        }
        else if (thriftPosition == IndexPosition.B) {
            this.hint = "B";
        }
        else if (thriftPosition == IndexPosition.DEV) {
            this.hint = "DEV";
        }
        else if (thriftPosition == IndexPosition.INTRA) {
            this.hint = "INTRA";
        }
        else if (thriftPosition == IndexPosition.ONLY) {
            this.hint = "ONLY";
        }
        else if (thriftPosition == IndexPosition.P5) {
            this.hint = "P5";
        }
        else if (thriftPosition == IndexPosition.P7) {
            this.hint = "P7";
        }
        else {
            throw new RuntimeException("SequeL cannot map index position " + thriftPosition);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexPositionBean that = (IndexPositionBean) o;

        if (!hint.equals(that.hint)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hint.hashCode();
    }
}
