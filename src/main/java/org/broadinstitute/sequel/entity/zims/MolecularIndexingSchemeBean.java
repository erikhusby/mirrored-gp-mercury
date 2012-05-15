package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;

import javax.xml.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@XmlRootElement(name = "MolecularIndexingScheme")
public class MolecularIndexingSchemeBean {

    @XmlElement(name = "name")
    private String name;

    @XmlElementWrapper(name = "sequences")
    private HashMap<IndexPositionBean,String> sequences = new HashMap<IndexPositionBean, String>();

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
