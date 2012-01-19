package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Date;

public class StatusNote {

    private static Log gLog = LogFactory.getLog(StatusNote.class);

    private LabEventName eventName;
    
    private Date date;
    
    public StatusNote(LabEventName eventName) {
        this.eventName = eventName;
        this.date = new Date();
    }
    
    public LabEventName getEventName() {
        return eventName;
    }
    
    public Date getDate() {
        return date;
    }
    
}
