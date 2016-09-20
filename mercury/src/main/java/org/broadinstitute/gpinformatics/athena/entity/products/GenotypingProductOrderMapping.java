package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Contains the genotyping chip configurable attributes of a product order.
 */
@Audited
@Entity
public class GenotypingProductOrderMapping extends AttributeArchetype {
    public static final String GENOTYPING_CHIP_OVERRIDE_CONFIG = "Genotyping Chip Override";
    public static final String ZCALL_THRESHOLD = "zcall_threshold_unix";
    public static final String CLUSTER_LOCATION = "cluster_location_unix";
    public static final String MAPPING_GROUP = "genotypingProductOrderMapping";
    public static final String ATTRIBUTES_GROUP = "genotypingProductOrder";
    public static final String CALL_RATE_OVERRIDE_MAPPING = "Call Threshold Override";
    public static final String CLUSTER_FILE_OVERRIDE_MAPPING = "Cluster File Override";

    public GenotypingProductOrderMapping() {
    }

    public GenotypingProductOrderMapping(String mappingName, String zCallThreshold, String clusterLocation) {
        super(GENOTYPING_CHIP_OVERRIDE_CONFIG, mappingName);
        addOrSetAttribute(ZCALL_THRESHOLD, zCallThreshold);
        addOrSetAttribute(CLUSTER_LOCATION, clusterLocation);
    }

    public String getZCallThreshold() {
        return getAttributeMap().get(ZCALL_THRESHOLD);
    }

    @Transient
    public void setZCallThreshold(String zCallThreshold) {
        addOrSetAttribute(ZCALL_THRESHOLD, zCallThreshold);
    }

    public String getClusterLocation() {
        return getAttributeMap().get(CLUSTER_LOCATION);
    }

    @Transient
    public void setClusterLocation(String clusterLocation) {
        addOrSetAttribute(CLUSTER_LOCATION, clusterLocation);
    }
}
