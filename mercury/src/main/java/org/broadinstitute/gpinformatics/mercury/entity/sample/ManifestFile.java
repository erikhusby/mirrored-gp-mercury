package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.envers.Audited;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Keeps a record of all Manifest Session files that were read.
 * Part of the intended design is to track filenames that should not
 * be processed again when they are not valid Manifest Sessions.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "MANIFEST_FILE",
        uniqueConstraints = @UniqueConstraint(columnNames = "qualified_filename"))
public class ManifestFile {
    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_FILE", schema = "mercury", sequenceName = "SEQ_MANIFEST_FILE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_FILE")
    private Long manifestFileId;

    /** The filename with the appropriate namespace, directory, bucket name, etc. */
    @Column(name = "qualified_filename")
    private String qualifiedFilename;

    protected ManifestFile() {
    }

    public ManifestFile(@NotNull String qualifiedFilename) {
        this.qualifiedFilename = qualifiedFilename;
    }

    @NotNull
    public String getQualifiedFilename() {
        return qualifiedFilename;
    }
}
