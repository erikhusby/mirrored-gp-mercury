package org.broadinstitute.sequel.control.bsp;

import java.util.Collection;
import java.util.List;
import java.util.Map;


public interface BSPSampleSearchService {


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

        

    /**
     * Return a mapping from each LSID in the input collection to a bare Sample ID
     * (ID without an SM- or SP- prefix) suitable for feeding to the runSampleSearch method
     * 
     * @param lsids
     * 
     * @return mapping from LSIDs to samples
     * 
     */
    Map<String, String> lsidsToBareIds(Collection<String> lsids);

}
