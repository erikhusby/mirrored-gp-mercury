package org.broadinstitute.gpinformatics.mercury.entity.infrastructure;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Audited
@Entity
@Table(schema = "mercury", name = "MANIFEST_FILE", uniqueConstraints = @UniqueConstraint(columnNames = {"FILENAME"}))
public class ManifestFile {
    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_FILE", schema = "mercury", sequenceName = "SEQ_MANIFEST_FILE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_FILE")
    private Long manifestFileId;

    @Column
    private String filename;

    public ManifestFile(String filename) {
        this.filename = filename;
    }

    public Long getManifestFileId() {
        return manifestFileId;
    }

    public String getFilename() {
        return filename;
    }
}
