package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a list of SNPs, e.g. a fingerprint panel.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class SnpList {

    @SuppressWarnings("unused")
    @SequenceGenerator(name = "SEQ_SNP_LIST", schema = "mercury", sequenceName = "SEQ_SNP_LIST")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SNP_LIST")
    @Id
    private Long snpListId;

    private String name;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury", name="SNP_LIST_SNPS"
            , joinColumns = {@JoinColumn(name = "snp_list")}
            , inverseJoinColumns = {@JoinColumn(name = "snps")})
    @OrderColumn()
    private List<Snp> snps = new ArrayList<>();

    @Transient
    private Map<String, Snp> mapRsIdToSnp;

    public SnpList(String name) {
        this.name = name;
    }

    /** For JPA. */
    protected SnpList() {
    }

    public Map<String, Snp> getMapRsIdToSnp() {
        if (mapRsIdToSnp == null) {
            mapRsIdToSnp = new HashMap<>();
            for (Snp snp : snps) {
                mapRsIdToSnp.put(snp.getRsId(), snp);
            }
        }
        return mapRsIdToSnp;
    }

    public String getName() {
        return name;
    }

    public List<Snp> getSnps() {
        return snps;
    }
}
