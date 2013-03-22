package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class PCRPrimerPairReagent extends Reagent {

    private static Log gLog = LogFactory.getLog(PCRPrimerPairReagent.class);

}
