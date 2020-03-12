package org.broadinstitute.gpinformatics.mercury.entity.hsa.state;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AggregationStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.DemultiplexStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.TaskDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.BclDemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to HSA Task entities
 */
@Test(groups = TestGroups.FIXUP)
public class TaskFixupTest extends Arquillian {

    @Inject
    private TaskDao taskDao;

    @Inject
    private StateMachineDao finiteStateMachineDao;

    @Inject
    private AggregationStateDao aggregationStateDao;

    @Inject
    private DemultiplexStateDao demultiplexStateDao;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private UserBean userBean;

    @Inject
    private FiniteStateMachineFactory finiteStateMachineFactory;

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
    public void fixupGplim6242AddExitTaskToStateAndStart() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("AddExitTaskAndStartMachine.txt"));
        String reason = lines.get(0);
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split("\\s");
            Long stateId = Long.valueOf(fields[1]);
            DemultiplexState aggregationState = demultiplexStateDao.findById(DemultiplexState.class, stateId);
            if (aggregationState == null) {
                throw new RuntimeException("Not a valid aggregation state");
            }

            BclDemultiplexMetricsTask alignmentMetricsTask = new BclDemultiplexMetricsTask();
            alignmentMetricsTask.setTaskName(aggregationState.getStateName().replace("Bcl", "BclMetric"));
            aggregationState.addExitTask(alignmentMetricsTask);

            aggregationState.getFiniteStateMachine().setStatus(Status.RUNNING);
            aggregationState.setAlive(true);
        }

        taskDao.persist(new FixupCommentary("GPLIM-6242 added bcl metrics task to state "));
        taskDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim6242AddExitTaskToState() throws Exception {
        userBean.loginOSUser();
        utx.begin();

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

    @Test(enabled = false)
    public void fixupGplim6242CreateDemuxStateForChambers() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName("190924_SL-NVB_0278_AHL5YWDSXX");
        FiniteStateMachine finiteStateMachine = new FiniteStateMachine();
        finiteStateMachine.setStateMachineName("Demultiplex_" + run.getRunName());

        SampleSheetBuilder sampleSheetBuilder = new SampleSheetBuilder();
        Set<IlluminaSequencingRunChamber> runChambers = run.getSequencingRunChambers();
        Set<VesselPosition> lanes =
                runChambers.stream().map(IlluminaSequencingRunChamber::getLanePosition).collect(Collectors.toSet());
        SampleSheetBuilder.SampleSheet sampleSheet = sampleSheetBuilder.makeSampleSheet(
                run, lanes, Collections.emptySet());
        Set<MercurySample> samples = new HashSet<>(sampleSheet.getData().getMapSampleToMercurySample().values());

        DemultiplexState demultiplexState =
                new DemultiplexState("2019-12-06--15-33-04", finiteStateMachine, samples, runChambers);
        demultiplexState.setAlive(false);
        demultiplexState.setStartState(true);
        demultiplexState.setStartTime(new Date());
        demultiplexState.setEndTime(new Date());

        File fastQ = new File("/seq/illumina/proc/SL-NVB/190924_SL-NVB_0278_AHL5YWDSXX/dragen/2019-12-06--15-33-04/fastq");
        File runDir = new File("/seq/illumina/proc/SL-NVB/190924_SL-NVB_0278_AHL5YWDSXX");
        File ssFile = new File("/seq/illumina/proc/SL-NVB/190924_SL-NVB_0278_AHL5YWDSXX/dragen/2019-12-06--15-33-04/SampleSheet_hsa.csv");
        DemultiplexTask task = new DemultiplexTask(runDir, fastQ, ssFile);
        task.setTaskName("Demux_" + run.getRunName());
        task.setStatus(Status.COMPLETE);

        finiteStateMachine.setStatus(Status.COMPLETE);
        finiteStateMachine.setStates(Collections.singletonList(demultiplexState));

        taskDao.persist(new FixupCommentary("GPLIM-6242 added misisng demux state"));
        taskDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim6242ReplaceCommandLineArg() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("FixTaskArg.txt"));
        String reason = lines.get(0);
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split("\\s");
            Long taskId = Long.valueOf(fields[0]);
            Task task = taskDao.findTaskById(taskId);
            if (task == null) {
                throw new RuntimeException("Not a valid task");
            }

            ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
            String replace = fields[1];
            String replacement = fields[2];
            Assert.assertEquals(true, processTask.getCommandLineArgument().contains(replace));
            String commandLineArgument = processTask.getCommandLineArgument();
            String testOutput = commandLineArgument.replace(replace, replacement);
            System.out.println(testOutput);
            processTask.setCommandLineArgument(testOutput);
        }

        taskDao.persist(new FixupCommentary("GPLIM-6242 fix task command line argument"));
        taskDao.flush();
        utx.commit();
    }
}
