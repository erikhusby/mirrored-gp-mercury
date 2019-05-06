package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TaskResult;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.File;

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = {@UniqueConstraint(columnNames = {"FILE_PATH"})})
public class WaitForFileTask extends Task {

    @Column(name = "FILE_PATH")
    private String filePath;

    public WaitForFileTask() {
    }

    public WaitForFileTask(File file) {
        this.filePath = file.getPath();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
