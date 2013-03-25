package org.broadinstitute.gpinformatics.infrastructure.jpa;


import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;

import javax.annotation.Nonnull;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Splits long lists into multiple queries. Oracle suffers from only being able
 * to run "in" queries on items of up to length 1000.
 * This splits data sets into sets, then queries each set individually, and and unions
 * the data together.
 */
@SuppressWarnings("UnusedDeclaration")
public class JPASplitter extends BaseSplitter {

    /**
     * A convenience method that will run a HQL query. The query must have all
     * the methods except for the long parameterList set on the query. This will
     * then split the long parameter list into chunks, and run the query on each
     * chunk of the data.
     *
     * @param query         The HQL query to be run. Set all parameters (except the long parameter list) for this first!
     * @param propertyName  The name of the parameter in the HQL query
     * @param parameterList The data to put into the query in the place of the parameterListName
     *
     * @return A List of the results, which is created by running the HQL query
     *         over each chunk of the data and adding those results together.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static <SPLIT_DATA_TYPE, RETURN_DATA_TYPE> List<RETURN_DATA_TYPE> runQuery(
            Query query, String propertyName, Collection<SPLIT_DATA_TYPE> parameterList) {

        // if the splitting is not needed, run the original query.
        if (parameterList.size() < BaseSplitter.DEFAULT_SPLIT_SIZE) {
            return runQueryStandard(query, propertyName, parameterList);
        }

        if (parameterList.isEmpty()) {
            return Collections.emptyList();
        }

        List<RETURN_DATA_TYPE> result = new ArrayList<RETURN_DATA_TYPE>();
        List<Collection<SPLIT_DATA_TYPE>> temp = split(parameterList);
        for (Collection<SPLIT_DATA_TYPE> data : temp) {
            List<RETURN_DATA_TYPE> o = runQueryStandard(query, propertyName, data);
            result.addAll(o);
        }
        return result;
    }

    /**
     * Run the query in the standard way, without any special splits, etc.
     *
     * @param query         The SQL or JQL query - null if a criteria query
     * @param propertyName  The name of the parameter in the query. null if a criteria query
     * @param parameterList The data to put into the query in the place of the
     *                      parameterListName
     *
     * @return The list of the results, created by running the HQL query.
     */
    @SuppressWarnings("unchecked")
    private static <SPLIT_DATA_TYPE, RETURN_DATA_TYPE>
    List<RETURN_DATA_TYPE> runQueryStandard(@Nonnull Query query,
                                            @Nonnull String propertyName,
                                            @Nonnull Collection<SPLIT_DATA_TYPE> parameterList) {

        if (parameterList.isEmpty()) {
            return Collections.emptyList();
        }

        query.setParameter(propertyName, parameterList);
        return query.getResultList();
    }

    public static <SPLIT_DATA_TYPE, RETURN_DATA_TYPE> List<RETURN_DATA_TYPE> runCriteriaQuery(
            @Nonnull Collection<SPLIT_DATA_TYPE> parameterList,
            @Nonnull CriteriaInClauseCreator<SPLIT_DATA_TYPE> criteriaCreator) {

        // if the splitting is not needed, run the original query.
        if (parameterList.size() < BaseSplitter.DEFAULT_SPLIT_SIZE) {
            return runCriteriaQuery(criteriaCreator, parameterList);
        }

        if (parameterList.isEmpty()) {
            return Collections.emptyList();
        }

        List<RETURN_DATA_TYPE> result = new ArrayList<RETURN_DATA_TYPE>();
        List<Collection<SPLIT_DATA_TYPE>> temp = split(parameterList);
        for (Collection<SPLIT_DATA_TYPE> data : temp) {
            List<RETURN_DATA_TYPE> o = runCriteriaQuery(criteriaCreator, data);

            System.out.println("input size: " + data.size() + " output size: " + o.size());

            result.addAll(o);
        }

        return result;
    }

    /**
     * Run a criteria query
     *
     * @param criteriaCreator  The engine to create the
     * @param parameterList    The data to put into the query in the place of the
     *                      parameterListName
     *
     * @return The list of the results, created by running the HQL query.
     */
    @SuppressWarnings("unchecked")
    private static <SPLIT_DATA_TYPE, RETURN_DATA_TYPE>
    List<RETURN_DATA_TYPE> runCriteriaQuery(@Nonnull CriteriaInClauseCreator<SPLIT_DATA_TYPE> criteriaCreator,
                                            @Nonnull Collection<SPLIT_DATA_TYPE> parameterList) {

        if (parameterList.isEmpty()) {
            return Collections.emptyList();
        }

        return criteriaCreator.createCriteriaInQuery(parameterList).getResultList();
    }
}

