package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.net.URL;

/**
 * Some kind of abstraction around file
 * handles, URL, cloud data access, relational
 * storage, etc. for raw sequencer output.
 *
 * Relating a named run or a named flowcell/ptp
 * with "where the heck is the data for this run"
 * is what we're trying to abstract here.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class OutputDataLocation {

    @SequenceGenerator(name = "SEQ_DATA_LOCATION", schema = "mercury", sequenceName = "SEQ_DATA_LOCATION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DATA_LOCATION")
    @Id
    private Long dataLocationId;

    private String dataLocation;

    private boolean archived = false;

    protected OutputDataLocation() {
    }

    public OutputDataLocation(String dataLocation) {
        this.dataLocation = dataLocation;
    }

    public String getDataLocation() {
        return dataLocation;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

}
