package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * The attributes of a genotyping array chip type are represented here by free-form key value pairs.
 * These are intended to be immutable so that a history of genotyping chip attributes is available
 * for the purpose of obtaining repeatable results on an old run.
 */

@Entity
@Audited
@Table(schema = "mercury")
public class GenotypingChipAttribute {

    @SequenceGenerator(name = "seq_genotyping_chip_attr", schema = "mercury", sequenceName = "seq_genotyping_chip_attr")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_genotyping_chip_attr")
    @Id
    private Long genotypingChipAttributeId;

    @ManyToOne
    private GenotypingChipType genotypingChipType;

    private String attributeName;

    private String attributeValue;

    public GenotypingChipAttribute(GenotypingChipType genotypingChipType, String attributeName, String attributeValue) {
        this.genotypingChipType = genotypingChipType;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    public GenotypingChipType getGenotypingChipType() {
        return genotypingChipType;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }
}
