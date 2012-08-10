package org.broadinstitute.sequel.entity.notice;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Audited
public class StatusNote {

    @Id
    private Long statusNoteId;

    private static Log gLog = LogFactory.getLog(StatusNote.class);

    private LabEventName eventName;
    
    private Date noteDate;
    
    public StatusNote(LabEventName eventName) {
        this.eventName = eventName;
        this.noteDate = new Date();
    }

    protected StatusNote() {
    }

    public LabEventName getEventName() {
        return eventName;
    }
    
    public Date getNoteDate() {
        return noteDate;
    }
    
}
