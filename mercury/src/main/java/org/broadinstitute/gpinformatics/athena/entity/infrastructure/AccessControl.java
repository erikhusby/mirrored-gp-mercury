package org.broadinstitute.gpinformatics.athena.entity.infrastructure;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This entity is responsible for defining the parameters to control access to certain defined components.  The
 * exact implementation and target of those components are defined by the sub classes that extend from this class.
 * Ideally this can be simpley extended and applied to any number of services within the application.
 */
@Entity
@Table(name = "ACCESS_CONTROL", schema = "athena")
public abstract class AccessControl implements Serializable{

    private static final long serialVersionUID = -8698508477326258176L;

    /*
        Separator for the features listed in the disabledFeatures string.  This can be used to join from a collection
        into a string and similarly split from the string back into a collection.
     */
    public static final String CONTROLLER_SEPARATOR_CHARS = "|@|";

    @Id
    @SequenceGenerator(name = "SEQ_ACCESS_CONTROL", schema = "athena", sequenceName = "SEQ_ACCESS_CONTROL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ACCESS_CONTROL")
    private Long id;

    @Enumerated(EnumType.STRING)
    private AccessStatus accessStatus = AccessStatus.ENABLED;

    /*
        While the accessStatus will define whether or not the service/component that is controlled by an instance of
        AccessControl is turned on, we can use this disabled features string to store a list of features which will
         be inaccessible when access is enabled
     */
    @Column
    private String disabledFeatures;

    @OneToMany
    private Set<AccessItem> disabledItems;

    public Long getId() {
        return id;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }

    public void setAccessStatus(AccessStatus accessStatus) {
        this.accessStatus = accessStatus;
    }

    public Set<AccessItem> getDisabledFeatures() {
        return Collections.unmodifiableSet(disabledItems);
    }

    public void setDisabledFeatures(Set<AccessItem> disabledFeatures) {
        if (!(disabledFeatures.size() == disabledItems.size()) &&
            !CollectionUtils.containsAll(disabledItems, disabledFeatures)) {
            disabledItems.clear();
        }
        this.disabledItems = disabledFeatures;
    }

    public void addDisabledFeatures(String feature) {

        addDisabledFeature(new AccessItem(feature));
    }

    public void addDisabledFeature(AccessItem feature) {

        if(!disabledItems.contains(feature)) {
            disabledItems.add(feature);
        }
    }

    public abstract String getControlTitle() ;

    public boolean isEnabled() {
        return accessStatus == AccessStatus.ENABLED;
    }
}
