package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

/**
 * A lab event isn't just at the granularity
 * of what we now consider a station event.
 *
 * Any lab event has the potential to change the
 * molecular state (see MolecularEnvelope) of the
 * lab vessels it references.  Some lab events may
 * not change the molecular envelope, but for those
 * that do, we expect the LabEvent to know the "expected"
 * molecular envelope of the input materials.  The event
 * itself can also alter the molecular envelope.
 *
 * This isn't too far off base: a Bravo protocol might
 * add adaptors.  The covaris event shears target DNA.
 * Both events change the molecular state; in order for
 * the lab operation to do its job, there are certain
 * input requirements that need to be met, such as
 * a concentration range, or the presence of adaptors
 * of a particular type.  If the incoming samples do not
 * meet the requirements, bad things happen, which
 * we expose via throwing InvalidPriorMolecularStateException
 * in applyMolecularStateChanges() method.
 *
 * In this model, LabEvents have to be quite aware of
 * the particulars of their inputs and outputs.  This
 * means it must be easy for lab staff to change expected
 * inputs and outputs based bot on the event definition
 * (think: Bravo protocol file) as well as the workflow.
 *
 * LabEvents can be re-used in different workflows, with
 * different expected ranges, and project manaagers might
 * want to override these ranges.
 */
// todo rename to "Event"--everything is an event, including
    // deltas in an aggregation in zamboni
public interface LabEvent {

    public LabEventName getEventName();

    /**
     * This is the change to sample state that this
     * operation accomplishes.
     *
     * A lab event could be someone scanning
     * a rack of tubes and saying "I hereby
     * bless thee as having this arrangement
     * of molecular indexes."  In this example,
     * there's no transfer for us to track,
     * and we don't bother to ask for the details
     * of how we managed to get the
     * indexes applied.  But we would configure
     * the magical "apply molecular indexes"
     * event type in the database such that
     * any applcation of the event would
     * have this effect.
     *
     * For example:
     * adding molecular indexes
     * changing volume
     * changing concentration
     * changing from RNA to DNA
     * changing target sample size by fragmentation
     * @return
     * @throws InvalidMolecularStateException when this LabEvent is being
     * applied in such a way that the molecular state change it causes is not
     * what is expected.
     */
    public void applyMolecularStateChanges() throws InvalidMolecularStateException;

    /**
     * Are the sources in the expected molecular state?
     *
     * In the messaging world, we are told of events
     * after they happen.  So refusing to persist
     * an event is unacceptable, although we might
     * want to log/alert when this exception is thrown.
     *
     * On the other hand, if we want to up-front validation
     * prior to events, we can leave it to the client
     * to respond to this exception without getting
     * our transactions confused.
     *
     * @throws InvalidMolecularStateException
     */
    public void validateSourceMolecularState() throws InvalidMolecularStateException;

    /**
     * Are the targets in the expected molecular state?
     *
     * In the messaging world, we are told of events
     * after they happen.  So refusing to persist
     * an event is unacceptable, although we might
     * want to log/alert when this exception is thrown.
     *
     * On the other hand, if we want to up-front validation
     * prior to events, we can leave it to the client
     * to respond to this exception without getting
     * our transactions confused.
     *
     * @throws InvalidMolecularStateException
     */
    public void validateTargetMolecularState() throws InvalidMolecularStateException;

    public Collection<LabVessel> getTargetLabVessels();

    public void addTargetLabVessel(LabVessel targetVessel);

    /**
     * For transfer events, this returns the sources
     * of the transfer
     * @return may return null
     */
    public Collection<LabVessel> getSourceLabVessels();

    public void addSourceLabVessel(LabVessel sourceVessel);

    public Collection<LabVessel> getSourcesForTarget(LabVessel targetVessel);

    public Collection<LabVessel> getTargetsForSource(LabVessel sourceVessl);

    /**
     * Returns all the lab vessels involved in this
     * operation, regardless of source/destination.
     *
     * Useful convenience method for alerts
     * @return
     */
    public Collection<LabVessel> getAllLabVessels();

    /**
     * Machine name?  Name of the bench?
     * GPS coordinates?
     * @return
     */
    public String getEventLocation();

    public Person getEventOperator();

    public Date getEventDate();

    public Collection<Reagent> getReagents();

    public void addReagent(Reagent reagent);

    /**
     * Probably a transient method that iterates
     * over all {@link org.broadinstitute.sequel.entity.vessel.LabVessel}s involved
     * in this event and builds a Collection of
     * {@link org.broadinstitute.sequel.entity.sample.SampleSheet}s
     *
     * Useful for sending out alerts about
     * the event.  Otherwise clients have to iterate
     * over containers and iterate over sample
     * sheets
     * @return
     */
    public Collection<SampleSheet> getAllSampleSheets();

    Set<SectionTransfer> getSectionTransfers();
    
    public void setQuoteServerBatchId(String batchId);
    
    public String getQuoteServerBatchId();

    Set<CherryPickTransfer> getCherryPickTransfers();
}
