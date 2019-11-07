package org.broadinstitute.gpinformatics.mercury.entity.hsa.state;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AggregationStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.DemultiplexStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.TaskDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_.sampleKey;

/**
 * Fixups to HSA Task entities
 */
@Test(groups = TestGroups.FIXUP)
public class TaskFixupTest extends Arquillian {

    @Inject
    private TaskDao taskDao;

    @Inject
    private AggregationStateDao aggregationStateDao;

    @Inject
    private DemultiplexStateDao demultiplexStateDao;

    @Inject
    private UserBean userBean;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateTaskName.txt, so it can
     * be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-4104
     * {Task ID},{new Task Name}
     */
    @Test(enabled = false)
    public void fixupGplim6242() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateTaskName.txt"));
        String jiraTicket = lines.get(0);

        for (String data: lines.subList(1, lines.size())) {
            String[] split = data.split(",");
            Task task = taskDao.findTaskById(Long.valueOf(split[0]));
            if (task == null) {
                throw new RuntimeException("Failed to find task with id " + split[0]);
            }
            String newTaskName = split[1].trim();
            System.out.println("Updating Task " + split[0] + " to new name " + newTaskName);
            task.setTaskName(newTaskName);
        }

        taskDao.persist(new FixupCommentary(jiraTicket + " update task names."));
        taskDao.flush();
        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateTaskStatus.txt, so it can
     * be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-6242
     * {Task ID},CANCELLED
     */
    @Test(enabled = false)
    public void fixupGplim6242Status() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateTaskStatus.txt"));
        String jiraTicket = lines.get(0);

        for (String data: lines.subList(1, lines.size())) {
            String[] split = data.split(",");
            Task task = taskDao.findTaskById(Long.valueOf(split[0]));
            if (task == null) {
                throw new RuntimeException("Failed to find task with id " + split[0]);
            }
            String newStatus = split[1].trim();
            Status status = Status.getStatusByName(newStatus);
            if (status == null) {
                throw new RuntimeException("Failed to find status " + newStatus);
            }
            System.out.println("Updating task " + split[0] + " to status " + newStatus);

            task.setStatus(status);
        }

        taskDao.persist(new FixupCommentary(jiraTicket + " update task status."));
        taskDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim6242AddExitTaskToState() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        //
        AggregationState aggregationState = aggregationStateDao.findById(AggregationState.class, 6253L);

        AlignmentMetricsTask alignmentMetricsTask = new AlignmentMetricsTask();
        alignmentMetricsTask.setTaskName("AggMetric_SM-JH386");
        aggregationState.addExitTask(alignmentMetricsTask);

        taskDao.persist(new FixupCommentary("GPLIM-6242 added alignment metrics task to state "));
        taskDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim6242AddExitTaskToDemux() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        DemultiplexState demultiplexState = demultiplexStateDao.findById(DemultiplexState.class, 6951L);

        DemultiplexMetricsTask demultiplexMetricsTask = new DemultiplexMetricsTask();
        demultiplexMetricsTask.setTaskName("DemuxMetricsAgg");
        demultiplexState.addExitTask(demultiplexMetricsTask);

        taskDao.persist(new FixupCommentary("GPLIM-6242 added demux metrics task to state "));
        taskDao.flush();
        utx.commit();
    }
}
