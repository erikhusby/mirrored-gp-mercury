package org.broadinstitute.gpinformatics.mercury.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MolecularIndexingSchemeBean {

    @JsonProperty("name")
    private String name;

    @JsonProperty("sequences")
    private List<IndexComponent> sequences = new ArrayList<>();

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
