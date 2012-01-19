package org.broadinstitute.sequel;

/**
 * I think I might get rid of this interface.  The problem
 * is that {@link MolecularState} is very much tied
 * up with things that might be containable.
 *
 * Suppose you take some Goop and aliquot it.
 * That's easy: the Goop could be containable
 * and after the aliquot, it exists in two
 * places.  The more interesting problems are
 * about what happens when you transfer goop
 * from container a to contain b, and add
 * some index, or maybe don't change the
 * molecular envelope, but do change the
 * concentration.  Is the goop the same?
 * From who's perspective is the goop the
 * same, and from whose is it different?
 *
 * I think instead we should just use
 * search.
 * 
 * Something is containable if it can exist
 * in multiple containers in the lab.  Samples,
 * libraries, and reagents are all containables.
 *
 * When we do any kind of fluid transfer (make an aliquot,
 * transfer a tube/plate, squirt stuff into a flowcell
 * lane, etc.), we should consider calling addContainer()
 * to the source "stuff".
 *
 * This is a different model from the current squid.
 * Squid currently makes a new "library" for almost
 * every transfer.  This is a problem when you're just
 * aliquoting something.  If you're not altering the
 * molecular state of something, you should be able
 * to just add the same thing to a different container.
 *
 * Historically the lab has objected to having the same
 * named library in different containers, but this is
 * because existing UIs and reports don't visualize this
 * situation very clearly.  Instead of having three libraries
 * with the same molecular state, you should have one library
 * with three containers.
 *
 * For instance, consider the situation with denatured libraries.
 * You might have 3 containers of denatured libraries that
 * are derived from the same normalized library.  Are these
 * three different libraries?  Probably not.  They're the
 * same molecular state, with the same samples.  They might
 * have been created at different times, but maybe they
 * were created in a single batch but doled out into
 * three containers because the total volume of the
 * batch exceeded the capacity of a 1.5mL tube.
 *
 * To succeed with this model, we have to give the lab
 * the tools to visualize and find their containers.
 * The rule of thumb should be: If you are looking
 * for containers of the same *thing*, and by *thing*
 * we mean something with the same molecular state
 * and samples, then don't make a new library, just
 * associate it to multiple containers.
 *
 * Various reports and UIs could show a named library
 * and then have a hover-over or click-out to list
 * all containers that contain the library.
 *
 * LabEvents should be configured to understand when
 * an event is created a new library vs. transferring
 * it to a new container.  Probably this could be some
 * kind of "is aliquot/new library" bit on the event.
 * For production, we wouldn't let users define this
 * on the fly, but for dev work, we could let the user
 * set this bit on-the-fly.
 */
public interface Containable {

    /**
     * Returns every thing that contains this
     * thing.  We should make this as lazy
     * as possible.
     * @return
     */
    public Iterable<LabVessel> getContainers();

    public Iterable<LabVessel> getContainers(MolecularStateRange molecularStateRange);

    public void addToContainer(LabVessel container);

}
