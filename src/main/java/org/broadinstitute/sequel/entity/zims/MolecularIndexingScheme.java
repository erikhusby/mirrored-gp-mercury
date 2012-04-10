package org.broadinstitute.sequel.entity.zims;


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
    
}
