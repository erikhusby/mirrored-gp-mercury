package org.broadinstitute.gpinformatics.athena.entity.project;


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

import java.util.Comparator;

public class Cohort implements Displayable {

    public final String cohortId;
    public final String name;
    public final String category;
    public final String group;
    public final boolean archived;

    public Cohort(String cohortId, String name, String category, String group, boolean archived) {
        this.cohortId = cohortId;
        this.name = name;
        this.category = category;
        this.group = group;
        this.archived = archived;
    }

    public String getCohortId() {
        return cohortId;
    }

    public boolean isArchived() {
        return archived;
    }

    public String getGroup() {
        return group;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if ( !(other instanceof Cohort) ) {
            return false;
        }

        Cohort castOther = (Cohort) other;
        return new EqualsBuilder().append(getCohortId(), castOther.getCohortId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getCohortId()).toHashCode();
    }

    @Override
    public String getDisplayName() {
        return cohortId + ": " + name;
    }

    public static final Comparator<Cohort> COHORT_BY_ID = new Comparator<Cohort> () {
        @Override
        public int compare(Cohort cohort, Cohort cohort1) {
            Integer nullCohort = nullCompare(cohort, cohort1);
            if (nullCohort != null) {
                return nullCohort;
            }

            // Neither cohort is null, check if the cohortId is null
            nullCohort = nullCompare(cohort.getCohortId(), cohort1.getCohortId());
            if (nullCohort != null) {
                return nullCohort;
            }

            // Neither of the cohortIds are null, so compare them
            return cohort.getCohortId().compareTo(cohort1.getCohortId());
        }

        private Integer nullCompare(Object obj1, Object obj2) {
            if ( (obj1 == null) && (obj2 == null) ) { return 0; }
            if (obj2 == null) { return 1; }
            if (obj1 == null) { return -1; }

            return null;
        }
    };
}
