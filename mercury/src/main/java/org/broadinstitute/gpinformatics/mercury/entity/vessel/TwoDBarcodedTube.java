package org.broadinstitute.gpinformatics.mercury.entity.vessel;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
@Entity
@Audited
@Table(schema = "mercury")
/**
 * Represents a tube with a two dimensional barcode on its bottom.  These tubes are usually stored in racks.
 */
public class TwoDBarcodedTube extends LabVessel {

    private static Log gLog = LogFactory.getLog(TwoDBarcodedTube.class);
    
    public TwoDBarcodedTube(String twoDBarcode) {
        super(twoDBarcode);
        if (twoDBarcode == null) {
            throw new IllegalArgumentException("twoDBarcode must be non-null in TwoDBarcodedTube.TwoDBarcodedTube");
        }
    }

    protected TwoDBarcodedTube() {
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.TUBE;
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

}
