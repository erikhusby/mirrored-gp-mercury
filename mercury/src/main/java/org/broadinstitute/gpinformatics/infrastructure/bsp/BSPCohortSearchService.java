package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.List;
import java.util.Set;


public interface BSPCohortSearchService {


    /**
     * Get list of cohorts ( BSP collections ) for a user.
     * Initially we expect this to be a Program PM username but could also be a sponsoring scientist.
     * @param bspUser
     * @return
     */
    Set<Cohort> getCohortsByUser(Person bspUser);

    //TODO PMB Remove the comments
//
//
//    /**
//     * Pull down data for the specified set of samples from the BSP
//     * runSampleSearchByCohort service.
//     *
//     *
//     *
//     * @param sampleIDs
//     *            list of sample ids for which to search
//     *
//     * @param resultColumns
//     *            the columns of data to search for
//     *
//     * @return The specified columns in order, the samples in the order supplied
//     *         to this method. BSP does not guarantee all samples listed in the
//     *         query will be present in the results.
//     */
//    List<String[]> runSampleSearch(Collection<String> sampleIDs,
//                                   BSPSampleSearchColumn... resultColumns);
//
//
////    /**
////     * Same method as above with a List instead of varargs for the BSPSampleSearchColumns
////     *
////     *
////     * @param sampleIDs
////     *            list of sample ids for which to search
////     *
////     * @param resultColumns
////     *            the columns of data to search for
////     *
////     * @return The specified columns in order, the samples in the order supplied
////     *         to this method. BSP does not guarantee all samples listed in the
////     *         query will be present in the results.
////     */
////    List<String []> runSampleSearch(Collection<String> sampleIDs,
////                                    List<BSPSampleSearchColumn> resultColumns);

    /**
     * Get list of samples for a cohort (BSP collections).
     * Initially we expect this to be a Program PM username but could also be a sponsoring scientist.
     *
     * @param cohort
     * @return
     */
    List<String> runSampleSearchByCohort(Cohort cohort) ;



}
