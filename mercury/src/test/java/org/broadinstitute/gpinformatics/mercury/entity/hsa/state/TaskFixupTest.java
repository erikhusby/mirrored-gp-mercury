package org.broadinstitute.gpinformatics.mercury.entity.hsa.state;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.TaskDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to HSA Task entities
 */
@Test(groups = TestGroups.FIXUP)
public class TaskFixupTest extends Arquillian {

    @Inject
    private TaskDao taskDao;

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
}
