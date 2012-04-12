package org.broadinstitute.sequel.entity.zims;


import org.broadinstitute.sequel.entity.reagent.DNAAppendage;
import org.broadinstitute.sequel.entity.reagent.IndexEnvelope;
import org.broadinstitute.sequel.entity.vessel.MolecularAppendage;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.hibernate.Hibernate;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

@XmlRootElement
public class MolecularIndexingScheme {

    public enum Index_Position {
        P7,
        P5,
        INTRA,
        DEV,
        ONLY,
        A,
        B
    }

    private String schemeName;
    
    private Map<Index_Position, String> sequences = new HashMap<Index_Position,String>();
    
    public MolecularIndexingScheme(MolecularEnvelope envelope) {
        if (envelope == null) {
            throw new RuntimeException("envelope cannot be null.");
        }

    }
    
}
