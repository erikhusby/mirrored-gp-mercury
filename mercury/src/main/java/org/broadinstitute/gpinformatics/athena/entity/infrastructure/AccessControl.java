package org.broadinstitute.gpinformatics.athena.entity.infrastructure;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "ACCESS_CONTROL", schema = "athena")
public abstract class AccessControl implements Serializable{

    private static final long serialVersionUID = -8698508477326258176L;
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private AccessStatus accessStatus = AccessStatus.ENABLED;



    private String disabledFeatures;

    public Long getId() {
        return id;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }

    public void setAccessStatus(AccessStatus accessStatus) {
        this.accessStatus = accessStatus;
    }

    public Set<String> getDisabledFeatures() {
        final HashSet<String> featureSet = new HashSet<>();

        if (StringUtils.isNotBlank(disabledFeatures)) {
            featureSet.addAll(Arrays.asList(StringUtils.split(disabledFeatures, ',')));
        }
        return featureSet;
    }

    public void setDisabledFeatures(Set<String> disabledFeatures) {
        this.disabledFeatures = StringUtils.join(disabledFeatures, ',');
    }

    public void addDisabledFeatures(String feature) {

        Set<String> currentFeatures = new HashSet<>(getDisabledFeatures());

        if(!currentFeatures.contains(feature)) {
            currentFeatures.add(feature);
        }

        setDisabledFeatures(currentFeatures);
    }
}
