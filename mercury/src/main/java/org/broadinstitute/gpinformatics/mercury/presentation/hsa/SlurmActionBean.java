package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.DragenInfoFetcher;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.PartitionInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SlurmController;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UrlBinding(SlurmActionBean.ACTION_BEAN_URL)
public class SlurmActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(SlurmActionBean.class);

    public static final String ACTION_BEAN_URL = "/hsa/slurm/slurm.action";

    private static final String LIST_PAGE = "/hsa/slurm/list.jsp";

    @Inject
    private SlurmController slurmController;

    @Inject
    private DragenInfoFetcher dragenInfoFetcher;

    private List<NodeStatus> nodeList = new ArrayList<>();

    // TODO Exclude nodes
    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        for (PartitionInfo partitionInfo: slurmController.listPartitions()) {
            String version = dragenInfoFetcher.getVersion(partitionInfo.getNodeList());
            NodeStatus nodeStatus = new NodeStatus();
            nodeStatus.setAvailable(partitionInfo.getAvailable());
            nodeStatus.setName(partitionInfo.getNodeList());
            nodeStatus.setState(partitionInfo.getState());
            nodeStatus.setVersion(version);
            nodeList.add(nodeStatus);
        }

        return new ForwardResolution(LIST_PAGE);
    }

    public List<NodeStatus> getNodeList() {
        return nodeList;
    }

    public class NodeStatus {

        private String name;
        private String available;
        private String state;
        private String version;

        public NodeStatus() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAvailable() {
            return available;
        }

        public void setAvailable(String available) {
            this.available = available;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
