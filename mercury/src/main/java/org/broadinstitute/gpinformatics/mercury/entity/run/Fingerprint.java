package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

/**
 * Stores a set of Single Nucleotide Polymorphisms that uniquely identify a sample.  Fingerprints can be generated
 * from genotyping or sequencing.  Fingerprints are used to detect mixups.  Typically, a fingerprint is made when a
 * sample is received, and later compared to another fingerprint taken from sequencing.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class Fingerprint {

    public enum Disposition {
        PASS,
        FAIL,
        NONE
    }

    public enum Platform {
        FLUIDIGM,
        GENERAL_SEQUENCING,
        GENERAL_ARRAY,
        FAT_PANDA
    }

    public enum GenomeBuild {
        HG18,
        HG19,
        HG38
    }

    public enum Gender {
        MALE,
        FEMALE,
        UNKNOWN
    }

    @SuppressWarnings("unused")
    @SequenceGenerator(name = "SEQ_FINGERPRINT", schema = "mercury", sequenceName = "SEQ_FINGERPRINT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FINGERPRINT")
    @Id
    private Long fingerprintId;

    @ManyToOne
    private MercurySample mercurySample;

    @Enumerated(EnumType.STRING)
    private Disposition disposition;

    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    private GenomeBuild genomeBuild;

    private Date dateGenerated;
    // reference to list of RSIDs?

    private String genotypes;

    private String callConfidences;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Boolean match;
    // fatPandaGq
    // fatPandaPl

    /** For JPA. */
    public Fingerprint() {
    }

    public Fingerprint(MercurySample mercurySample, Disposition disposition, Platform platform, GenomeBuild genomeBuild,
            Date dateGenerated, String genotypes, String callConfidences, Gender gender, Boolean match) {
        this.mercurySample = mercurySample;
        this.disposition = disposition;
        this.platform = platform;
        this.genomeBuild = genomeBuild;
        this.dateGenerated = dateGenerated;
        this.genotypes = genotypes;
        this.callConfidences = callConfidences;
        this.gender = gender;
        this.match = match;
    }

    public MercurySample getMercurySample() {
        return mercurySample;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    public Platform getPlatform() {
        return platform;
    }

    public GenomeBuild getGenomeBuild() {
        return genomeBuild;
    }

    public Date getDateGenerated() {
        return dateGenerated;
    }

    public String getGenotypes() {
        return genotypes;
    }

    public String getCallConfidences() {
        return callConfidences;
    }

    public Gender getGender() {
        return gender;
    }

    public Boolean getMatch() {
        return match;
    }
}
