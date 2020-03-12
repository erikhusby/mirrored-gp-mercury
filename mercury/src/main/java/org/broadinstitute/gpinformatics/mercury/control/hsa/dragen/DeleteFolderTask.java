package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.File;

@Entity
@Audited
public class DeleteFolderTask extends Task {

    @Column(name = "FILE_PATH")
    private String filePath;

    public DeleteFolderTask() {
    }

    public DeleteFolderTask(File file) {
        this.filePath = file.getPath();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
