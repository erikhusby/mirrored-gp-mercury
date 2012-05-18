package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import org.codehaus.jackson.annotate.JsonAutoDetect;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "MolecularIndexingScheme")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class MolecularIndexingSchemeBean {

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "sequences")
    private List<IndexComponent> sequences = new ArrayList<IndexComponent>();

    public MolecularIndexingSchemeBean() {}

    public MolecularIndexingSchemeBean(MolecularIndexingScheme indexingScheme) {
        this.name  = indexingScheme.getName();
        for (Map.Entry<IndexPosition, String> entry : indexingScheme.getSequences().entrySet()) {
            IndexPosition thriftPosition = entry.getKey();
            sequences.add(new IndexComponent(thriftPosition, entry.getValue()));
        }
    }
    
    public String getName() {
        return name;
    }
    
    public List<IndexComponent> getSequences() {
        return sequences;
    }
}
