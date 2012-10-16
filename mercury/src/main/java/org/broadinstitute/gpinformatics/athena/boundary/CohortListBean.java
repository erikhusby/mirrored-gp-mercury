package org.broadinstitute.gpinformatics.athena.boundary;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Could not seem to inject the BSPUserList directly into the xhtml file, so this is a wrapper that does
 * the injection and provides the access to the find for anything that wants user names instead of the stored
 * ID.
 */
@Named
@RequestScoped
public class CohortListBean {

    @Inject
    private BSPCohortList cohortList;

    public String getCohort(String cohortId) {
        String fullName = "";
        if (cohortId != null) {
            Cohort cohort = cohortList.getById(cohortId);

            if (cohort == null) {
                return "(Unknown user: " + cohortId + ")";
            }
            return cohort.getCohortId() + ": " + cohort.getName() + "(" + cohort.getGroup() + ", " + cohort.getCategory() + ")";
        }

        return fullName;
    }

    public Cohort[] getCohorts(String[] cohortIds) {
        if (cohortIds == null) {
            return new Cohort[0];
        }

        Cohort[] cohorts = new Cohort[cohortIds.length];
        int i=0;
        for (String cohortId : cohortIds) {
            cohorts[i++] = cohortList.getById(cohortId);
        }

        return cohorts;
    }

    public List<Cohort> searchActiveCohort(String query) {
        return cohortList.findActive(query);
    }

    public String getCohortListString(String[] cohortIds) {
        String cohortListString = "";

        if ((cohortList != null) && (cohortIds != null) && (cohortIds.length > 0)) {
            String[] nameList = new String[cohortIds.length];
            int i=0;
            for (String cohortId : cohortIds) {
                nameList[i++] = getCohort(cohortId);
            }

            cohortListString = StringUtils.join(nameList, ", ");
        }

        return cohortListString;
    }
}

