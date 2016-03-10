package org.broadinstitute.gpinformatics.mercury.entity.run;

import freemarker.ext.beans.HashAdapter;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class models each type of Genotyping array chip. Edits made by a user should cause an existing
 * GenotypingChipType to be versioned, to give a history accessible by date, and allows the user to get
 * repeatable results when an older run is reanalyzed.
 */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"chipName","createdDate"}))
public class GenotypingChipType {

    @SequenceGenerator(name = "seq_genotyping_chip_type", schema = "mercury", sequenceName = "seq_genotyping_chip_type")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_genotyping_chip_type")
    @Id
    private Long genotypingChipTypeId;

    private String chipName;

    private Date createdDate;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "genotypingChipType")
    private List<GenotypingChipAttribute> chipAttributes;

    public GenotypingChipType(String chipName) {
        this.chipName = chipName;
        createdDate = new Date();
        chipAttributes = new ArrayList<>();
    }

    public String getChipName() {
        return chipName;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public List<GenotypingChipAttribute> getChipAttributes() {
        return chipAttributes;
    }

    public Map<String, String> getChipAttributeMap() {
        return new HashMap<String, String>(){{
            for (GenotypingChipAttribute chipAttribute : chipAttributes) {
                put(chipAttribute.getAttributeName(), chipAttribute.getAttributeValue());
            }
        }};
    }

    public static final Comparator BY_NAME_DATE_DESC = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            GenotypingChipType g1 = (GenotypingChipType)o1;
            GenotypingChipType g2 = (GenotypingChipType)o2;
            int nameCompare = g1.chipName.compareTo(g2.chipName);
            return nameCompare != 0 ? nameCompare : g1.createdDate.compareTo(g2.createdDate);
        }
    };
}
