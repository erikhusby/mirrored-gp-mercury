package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 12/17/12
 *         Time: 4:51 PM
 */
@ManagedBean
@ViewScoped
public class BatchConfirm extends AbstractJsfBean {

    @Inject
    LabBatchDAO labBatchDAO;

    @Inject
    private BSPUserList bspUserList;

    private LabBatch foundBatch;

    private List<LabVessel> listOfVessels;

    public void initForm() {

        listOfVessels = new ArrayList<LabVessel>(foundBatch.getStartingLabVessels());
    }

    public List<LabVessel> getListOfVessels() {
        return listOfVessels;
    }

    public void setListOfVessels(List<LabVessel> listOfVessels) {
        this.listOfVessels = listOfVessels;
    }

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }

    public LabBatch getFoundBatch() {
        return foundBatch;
    }

    public void setFoundBatch(LabBatch foundBatch) {
        this.foundBatch = foundBatch;
    }
}
