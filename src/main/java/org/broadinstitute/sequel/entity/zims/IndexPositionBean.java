package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "IndexPosition")
@XmlAccessorType(XmlAccessType.FIELD)
public class IndexPositionBean {

    private String hint;

    public IndexPositionBean() {}

    public IndexPositionBean(IndexPosition thriftPosition) {

        this.hint = thriftPosition.name();
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
