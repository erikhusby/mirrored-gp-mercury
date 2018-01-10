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
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores a set of Single Nucleotide Polymorphisms that uniquely identify a sample.  Fingerprints can be generated
 * from genotyping or sequencing.  Fingerprints are used to detect mixups.  Typically, a fingerprint is made when a
 * sample is received (with a genotyping array), and later compared to another fingerprint derived from sequencing.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class Fingerprint {

    public enum Disposition {
        PASS("P"),
        FAIL("F"),
        NONE("N");

        private final String abbreviation;

        Disposition(String abbreviation) {
            this.abbreviation = abbreviation;
        }

        public String getAbbreviation() {
            return abbreviation;
        }

        public static Disposition byAbbreviation(String abbreviation) {
            for (Disposition disposition : Disposition.values()) {
                if (disposition.getAbbreviation().equals(abbreviation)) {
                    return disposition;
                }
            }
            return null;
        }
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
        MALE("M"),
        FEMALE("F"),
        UNKNOWN("U");

        private final String abbreviation;

        Gender(String abbreviation) {
            this.abbreviation = abbreviation;
        }

        public String getAbbreviation() {
            return abbreviation;
        }

        public static Gender byAbbreviation(String abbreviation) {
            for (Gender gender : Gender.values()) {
                if (gender.getAbbreviation().equals(abbreviation)) {
                    return gender;
                }
            }
            return null;
        }
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

    @ManyToOne
    private SnpList snpList;

    @OneToMany(mappedBy = "fingerprint")
    private Set<FpGenotype> fpGenotypes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Boolean match;
    // fatPandaGq
    // fatPandaPl

    /** For JPA. */
    public Fingerprint() {
    }

    public Fingerprint(MercurySample mercurySample, Disposition disposition, Platform platform, GenomeBuild genomeBuild,
            Date dateGenerated, SnpList snpList, Gender gender, Boolean match) {
        this.mercurySample = mercurySample;
        this.snpList = snpList;
        mercurySample.getFingerprints().add(this);
        this.disposition = disposition;
        this.platform = platform;
        this.genomeBuild = genomeBuild;
        this.dateGenerated = dateGenerated;
        this.gender = gender;
        this.match = match;
    }

    @Transient
    private
    Map<String, FpGenotype> mapRsIdToFpGenotype;

    public Map<String, FpGenotype> getMapRsIdToFpGenotype() {
        if (mapRsIdToFpGenotype == null) {
            mapRsIdToFpGenotype = new HashMap<>();
            for (FpGenotype fpGenotype : fpGenotypes) {
                mapRsIdToFpGenotype.put(fpGenotype.getSnp().getRsId(), fpGenotype);
            }
        }

        return mapRsIdToFpGenotype;
    }

    public List<FpGenotype> getFpGenotypesOrdered() {
        List<FpGenotype> fpGenotypesOrdered = new ArrayList<>();
        for (Snp snp : snpList.getSnps()) {
            fpGenotypesOrdered.add(getMapRsIdToFpGenotype().get(snp.getRsId()));
        }

        return fpGenotypesOrdered;
    }

    public void addFpGenotype(FpGenotype fpGenotype) {
        fpGenotypes.add(fpGenotype);
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

    public SnpList getSnpList() {
        return snpList;
    }

    public Gender getGender() {
        return gender;
    }

    public Boolean getMatch() {
        return match;
    }
}
