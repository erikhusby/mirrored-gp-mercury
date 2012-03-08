package org.broadinstitute.sequel.entity.queue;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;

/**
 * Concrete subclasses will load their configuration
 * from an entity that has things like min/max
 * queue size, as well as the molecular envelope,
 * min/max concentration, and basic "expected molecular
 * state" information.
 * 
 * The question will come up: "Do we make a new queue
 * for this, or add a new set of parameters and re-use
 * an existing queue?"  The answer depends on to what
 * extent you want the {@link org.broadinstitute.sequel.entity.vessel.MolecularStateRange} to
 * determine admissability into the queue.  If 
 * the lab is making a special version of a protocol
 * (for instance a "low concentration" version),
 * you want to make a new queue because the new
 * queue will give you the ability to set {@link org.broadinstitute.sequel.entity.vessel.MolecularStateRange}
 * customized to the queue.  If you just add a new
 * set of parameters, then whoever is putting stuff
 * into the queue can screw it up by putting something
 * out of range into the queue.
 *
 * There is a queue per "team" in the lab.  The
 * team that makes flowcells, the team that
 * loads sequencers, the team that does picos,
 * etc.
 *
 * Tangibles in the queue primarily have one state:
 * either they're in the queue pending, or they're
 * "out for delivery", to use a fedex analogy.
 *
 * For the most part, we don't ever receive a reliable
 * signal that something that left the loading dock
 * has actually reached the destination.  It goes
 * out the door, but we're not requiring delivery
 * confirmation of the lab techs because it's a big
 * pain in the ass for them.  See markComplete() for more
 * details.
 *
 * Priority (set at project, sample, or tangible level)
 * is helpful, as is the Project.isOutbreak() bit,
 * but we also have capacity reserved for certain
 * classes of projects.  This "class" loosely corresponds
 * to the funding source in the quote from the quotes
 * database.  For example, we might have 40% of our
 * sequencing capacity reserved for NHGRI, and 10% reserved
 * for NIAID.  But the timelines shift.  So we might
 * have 40% for one grant year, and feel like
 * doing 0% for two months of the year.  That's the
 * long way of saying that priority isn't the only
 * thing to pay attention to.  I suspect that the capacity
 * requirements are vague enough and volatile enough
 * week-to-week that we'll avoid them most of the
 * time and just have every queue extend from FIFOLabWorkQueue.
 *
 * We should probabl refactor this into a ReadableWorkQueue
 * and WriteableWorkQueue to deal with things like BSP,
 * where we're only putting things into the queue, not
 * taking things out.
 */
public interface FullAccessLabWorkQueue<T extends LabWorkQueueParameters> extends LabWorkQueue<T> {

    /**
     * We're all about communication at the Broad, and this
     * method gives the queue an opportunity to look at
     * prioritization schemes set by project managers
     * and suggest which things should be worked on
     * first.
     *
     * When the rubber hits the road, the lab tech can
     * always say "forget that, I'm going to work on these
     * samples" and ignore prioritization, which is why
     * startWork() just takes any pile of things.
     *
     * @param batchSize
     * @return
     */
    public Collection<LabVessel> suggestNextBatch(int batchSize, T bucket);

    /**
     * Returns the contents which are waiting for
     * work, but doesn't alter the
     * state.  Useful for probing queries for senior
     * management that wants to guess at future
     * demand.
     * @return
     */
    public Collection<LabVessel> peek(T bucket);


    public Collection<LabVessel> peekAll();

    /**
     * Sometimes you just gotta put something
     * at the top of the queue, in violation
     * of whatever rules you have.  Here's
     * how you do that.
     * @param vessel
     */
    public void moveToTop(LabVessel vessel,T bucket);

    /**
     * When the lab actually starts work, they
     * call this method.  Note that they might start
     * work on things that conflicts with the
     * preferred priority.  As long as they
     * start work on something in the queue, they
     * should be okay.
     * @param vessel
     */
    public void startWork(LabVessel vessel, T bucket);

    /**
     * This should be considered optional.  Most of the time
     * we start work in the lab but have no reasonable way
     * to say that the work is done.  The reason is that
     * most teams consume the entire contents of something
     * like a tube, then throw the tube away after they've
     * transferred stuff out of the tube.
     *
     * If you've thrown away your source container,
     * how do you get it back, scan it, and then tell
     * us that your work is done?  We could consider
     * using a durable task name of some sort, so that
     * people could scan in a barcode name off a printed
     * sheet to say "I'm done", but this leaves you with
     * the problem of partial failures, which happen often.
     * If you finished the task, but 3 of the tubes dropped
     * out, how do you tell this to the system?  As a lab
     * tech, you have to map somehow between the containers
     * you have as final products and the inputs that
     * you threw away.  Some magical workflow system
     * might help here, but let's just assume calling markComplete()
     * is rare, maybe only reserved for BSP aliquot
     * fulfillment.
     *
     * In other words: if you are making a feature that
     * requires someone mark their input tubes as complete,
     * 2 hours after they've thrown out the single-use
     * starting material, you're probably screwed.
     * @param vessel
     */
    public void markComplete(LabVessel vessel, T bucket);

    /**
     * Prints a work sheet for the operator.  Could be a zebra printer,
     * a regular printer, or some other thing that writes out
     * highly transportable information for the lab tech to use
     * in a wet lab.
     *
     * One implementation could be to make new jira ticket for
     * this step, or send a message to some other separate system
     * that lab techs use to track their work.
     * @param vessel
     */
    public void printWorkSheet(Collection<LabVessel> vessel,T bucket);


    /**
     * The same tangible may be in multiple buckets at the same
     * time.  This tells you which buckets the tangible is curently
     * in.  When the lab says "I'm starting work on this plate",
     * sometimes you may have to say "Wait, which bucket?",
     * for cases when perhaps you want a 76bp lane for the
     * library as well as a 101bp lane for the very
     * same library.
     *
     *
     * @param vessel@return
     */
    public Collection<T> getContainingBuckets(LabVessel vessel);

    /**
     * How many times has the given tangible
     * been placed in this work queue?
     *
     * @param vessel@return
     */
    public int getNumOrbits(LabVessel vessel);

}
