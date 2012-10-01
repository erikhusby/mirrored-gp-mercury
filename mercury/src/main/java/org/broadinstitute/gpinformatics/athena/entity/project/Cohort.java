package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:25 PM
 */
public class Cohort {


    public final CohortID id;
    public final String name;

    public Cohort(CohortID id, String name) {
        this.id = id;
        this.name = name;
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
