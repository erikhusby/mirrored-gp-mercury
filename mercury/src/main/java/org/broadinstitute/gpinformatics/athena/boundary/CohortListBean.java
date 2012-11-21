package org.broadinstitute.gpinformatics.athena.boundary;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Could not seem to inject the BSPCohortList directly into the xhtml file, so this is a wrapper that does
 * the injection and provides the access to the find for anything that wants cohort names instead of the stored
 * ID.
 */
@Named
@RequestScoped
public class CohortListBean {

    @Inject
    private BSPCohortList cohortList;

    private String getCohortName(String cohortId) {
        String cohortName = "";
        if (cohortId != null) {
            Cohort cohort = cohortList.getById(cohortId);

            if (cohort == null) {
                return "(Unknown cohort: " + cohortId + ")";
            }
            return cohort.getName();
        }

        return cohortName;
    }

    public List<Cohort> searchActiveCohort(String query) {
        return cohortList.findActive(query);
    }

    public String getCohortListString(String[] cohortIds) {
        String cohortListString = "";

        if ((cohortIds != null) && (cohortIds.length > 0)) {
            String[] nameList = new String[cohortIds.length];
            int i=0;
            for (String cohortId : cohortIds) {
                nameList[i++] = getCohortName(cohortId);
            }

            cohortListString = StringUtils.join(nameList, ", ");
        }

        return cohortListString;
    }
}

