package org.broadinstitute.gpinformatics.infrastructure.bsp;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


public interface BSPSampleSearchService extends Serializable {


    /**
     * Pull down data for the specified set of samples from the BSP
     * runSampleSearch service.
     * 
     *
     *
     * @param sampleIDs
     *            list of sample ids for which to search
     *
     * @param resultColumns
     *            the columns of data to search for
     *
     * @return The specified columns in order, the samples in the order supplied
     *         to this method. BSP does not guarantee all samples listed in the
     *         query will be present in the results.
     */
    List<String[]> runSampleSearch(Collection<String> sampleIDs,
                                   BSPSampleSearchColumn... resultColumns);

    /**
     * Same method as above with a List instead of varargs for the BSPSampleSearchColumns
     *
     *
     * @param sampleIDs
     *            list of sample ids for which to search
     *
     * @param resultColumns
     *            the columns of data to search for
     *
     * @return The specified columns in order, the samples in the order supplied
     *         to this method. BSP does not guarantee all samples listed in the
     *         query will be present in the results.
     */
    List<String []> runSampleSearch(Collection<String> sampleIDs,
                                    List<BSPSampleSearchColumn> resultColumns);
}
