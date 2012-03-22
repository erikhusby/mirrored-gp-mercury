package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import java.util.Collection;
import java.util.Date;

public class SimpleUserEvent extends AbstractLabEvent {

    private LabEventName eventName;

    // todo make this a superclass of lab events,
    // so that "simple" events like "kiosk checkin",
    // "work started", with just user, type and location,
    // can be shared by more detailed automation events.
    public SimpleUserEvent(Person user,
                           LabEventName eventName) {
        if (user == null) {
             throw new NullPointerException("user cannot be null."); 
        }
        if (eventName == null) {
             throw new NullPointerException("eventName cannot be null."); 
        }
        setEventDate(new Date(System.currentTimeMillis()));
        setEventOperator(user);
        this.eventName = eventName;
    }
    
    @Override
    public LabEventName getEventName() {
        return eventName;
    }

    @Override
    public void applyMolecularStateChanges() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void validateSourceMolecularState() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void validateTargetMolecularState() throws InvalidMolecularStateException {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<SampleSheet> getAllSampleSheets() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
