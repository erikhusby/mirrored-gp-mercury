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
    
    private Map<IndexPosition,String> sequences;

     public MolecularIndexingSchemeBean() {}

    public MolecularIndexingSchemeBean(MolecularIndexingScheme indexingScheme) {
        this.name  = indexingScheme.getName();
        this.sequences = indexingScheme.getSequences();
    }
}
