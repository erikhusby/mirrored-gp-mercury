package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * A fairly generic "search everything"
 * kind of thing.
 */
public interface LabMaterialSearch {

    /**
     * Given a pile of aliquots, the molecular
     * state you're looking for, and some
     * metric ranges, tell me what lab materials
     * we have in GSP.
     *
     * @param aliquots
     * @param molecularStateRange
     * @param samplePlexity are you looking for pooled or unpooled
     *                      containers?  How many {@link SampleInstance}s
     *                      do you want in the container?
     * @param containerType what kind of {@link org.broadinstitute.sequel.LabVessel.CONTAINER_TYPE container type} do you want to
     *                      search for?
     * @param labMetricRangeRestrictions
     * @return
     */
    public Collection<LabVessel> doSearch(Iterable<Goop> aliquots,
                                          MolecularStateRange molecularStateRange,
                                          Integer samplePlexity,
                                          LabVessel.CONTAINER_TYPE containerType,
                                          Iterable<LabMetricRange> labMetricRangeRestrictions);

    /**
     * Given a pile of aliquots and the queue
     * you'd like to add stuff to, and some
     * metric ranges, tell me what lab materials
     * we have in GSP.
     *
     * You'd run this when you're trying to "fill
     * capacity" in the lab.  "Hey" says the lab tech
     * (or project manager),
     * "I'm supposed to run this step for 24 tubes,
     * but I only have 5 in my hand.  Where are some other
     * tubes that I can add to the reaction?"
     *
     * @param aliquots
     * @param queueName the name of the queue
     *                  into which you'd like to add stuff
     * @param labMetricRangeRestrictions
     * @return
     */
    public Collection<LabVessel> doSearch(Iterable<Goop> aliquots,
                                          LabWorkQueueName queueName,
                                          Integer samplePlexity,
                                          Iterable<LabMetricRange> labMetricRangeRestrictions);

}
