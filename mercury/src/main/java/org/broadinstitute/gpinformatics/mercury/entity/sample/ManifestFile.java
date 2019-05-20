package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.envers.Audited;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * Keeps a record of all Manifest Session files that were read,
 * including files that could not be made into a ManifestSession.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "MANIFEST_FILE",
        uniqueConstraints = @UniqueConstraint(columnNames = "qualified_filename"))
public class ManifestFile {
    public final static String NAMESPACE_DELIMITER = "/";

    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_FILE", schema = "mercury", sequenceName = "SEQ_MANIFEST_FILE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_FILE")
    private Long manifestFileId;

    /** The filename with the appropriate namespace, directory, bucket name, etc. */
    @Column(name = "qualified_filename", unique = true)
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

    /** Returns the namespace and filename. */
    @Transient
    public Pair<String, String> getNamespaceFilename() {
        return Pair.of (StringUtils.substringBefore(qualifiedFilename, NAMESPACE_DELIMITER),
                StringUtils.substringAfter(qualifiedFilename, NAMESPACE_DELIMITER));
    }

    public static String extractFilename(String qualifiedFilename) {
        return StringUtils.substringAfter(qualifiedFilename, NAMESPACE_DELIMITER);
    }

    public static String qualifiedFilename(String namespace, String filename) {
        return namespace + NAMESPACE_DELIMITER + filename;
    }

}
