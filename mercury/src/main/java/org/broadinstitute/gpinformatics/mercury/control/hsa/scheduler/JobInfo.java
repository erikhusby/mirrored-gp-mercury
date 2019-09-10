package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Contains data from sacct -j {jobId} -p
 */
public class JobInfo {

    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @CsvBindByName(column = "JobName")
    private String name;

    @CsvBindByName(column = "JobID")
    private String id;

    @CsvBindByName(column = "ExitCode")
    private String exitCode;

    @CsvBindByName(column = "State")
    private String state;

    @CsvBindByName(column = "Partition")
    private String partition;

    @CsvBindByName(column = "End")
    private String end;

    public JobInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExitCode() {
        return exitCode;
    }

    public void setExitCode(String exitCode) {
        this.exitCode = exitCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    public Date getEnd() {
        if (!end.toLowerCase().equals("unknown")) {
            try {
                return DATE_PARSER.parse(end);
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public Status getStatus() {
        SlurmStateCode slurmStateCode = SlurmStateCode.getByName(state);
        switch (slurmStateCode) {
        case BOOT_FAIL:
        case FAILED:
        case OUT_OF_MEMORY:
        case DEADLINE:
        case NODE_FAIL:
        case PREEMPTED:
        case SPECIAL_EXIT:
        case STOPPED:
        case TIMEOUT:
        case REVOKED:
            return Status.FAILED;
        case CANCELLED:
            return Status.CANCELLED;
        case COMPLETED:
            return Status.COMPLETE;
        case PENDING:
        case REQUEUE_HOLD:
        case REQUEUE_FED:
        case REQUEUED:
        case STAGE_OUT:
        case SIGNALING:
            return Status.QUEUED;
        case RUNNING:
        case COMPLETING:
        case RESIZING:
            return Status.RUNNING;
        case RESV_DEL_HOLD:
        case SUSPENDED:
            return Status.SUSPENDED;
        default:
            return Status.QUEUED;
        }
    }
}
