package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

/**
* TODO scottmat fill in javadoc!!!
*/
public interface LabEventRefDataFetcher {
    BspUser getOperator(String userId);

    BspUser getOperator(Long bspUserId);

    LabBatch getLabBatch(String labBatchName);
}
