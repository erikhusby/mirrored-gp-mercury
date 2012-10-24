package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Audited
@Table(schema = "mercury")
public class PCRPrimerPairReagent extends Reagent {

    private static Log gLog = LogFactory.getLog(PCRPrimerPairReagent.class);

    public PrimerPair getPrimerPair() {
        throw new RuntimeException("Method not yet implemented.");
    }

}
