package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement(name = "MolecularIndexingScheme")
@XmlAccessorType(XmlAccessType.FIELD)
public class MolecularIndexingSchemeBean {
   
    private String name;
    
    private Map<IndexPositionBean,String> sequences;

     public MolecularIndexingSchemeBean() {}

    public MolecularIndexingSchemeBean(MolecularIndexingScheme indexingScheme) {
        this.name  = indexingScheme.getName();
        for (Map.Entry<IndexPosition, String> entry : indexingScheme.getSequences().entrySet()) {
            IndexPosition thriftPosition = entry.getKey();
            sequences.put(new IndexPositionBean(thriftPosition),entry.getValue());
        }
    }
    
    public String getName() {
        return name;
    }
    
    public Map<IndexPositionBean,String> getSequences() {
        return sequences;
    }
}
