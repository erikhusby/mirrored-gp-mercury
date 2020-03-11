package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DeleteFolderTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;

import javax.enterprise.context.Dependent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Dependent
public class DeleteFolderTaskHandler extends AbstractTaskHandler<DeleteFolderTask> {

    private static final Log log = LogFactory.getLog(DeleteFolderTaskHandler.class);

    @Override
    public void handleTask(DeleteFolderTask task, SchedulerContext schedulerContext) {
        File file = new File(task.getFilePath());
        if (!file.exists()) {
            task.setStatus(Status.SUSPENDED);
            task.setErrorMessage("Folder doesn't exist: " + file.getPath());
        } else if (!file.isDirectory()) {
            task.setStatus(Status.SUSPENDED);
            task.setErrorMessage("Folder isn't a directory: " + file.getPath());
        } else if (task.getQueuedTime() == null) {
            task.setStatus(Status.SUSPENDED);
            task.setErrorMessage("Task queued time required.");
        } else {
            try {
                FileTime lastModifiedTime = Files.getLastModifiedTime(file.toPath(), LinkOption.NOFOLLOW_LINKS);
                Instant lastModifiedInstant = lastModifiedTime.toInstant();
                Instant twoWeeksFromStart = task.getQueuedTime().toInstant().plus(14L, ChronoUnit.DAYS);
                if (twoWeeksFromStart.isBefore(lastModifiedInstant)) {
                    FileUtils.deleteDirectory(file);
                    task.setStatus(Status.COMPLETE);
                }
            } catch (IOException e) {
                log.error("Error occurred reading directory: " + file.getPath(), e);
                task.setStatus(Status.SUSPENDED);
                task.setErrorMessage(e.getMessage());
            }
        }
    }
}
