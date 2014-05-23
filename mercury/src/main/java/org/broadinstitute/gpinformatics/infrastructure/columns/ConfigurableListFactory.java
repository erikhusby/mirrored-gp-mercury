package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.ColumnSetsPreference;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates ConfigurableList instances.
 */
public class ConfigurableListFactory {

    @Inject
    private PreferenceDao preferenceDao;

    /**
     * Create a ConfigurableList instance.
     *
     * @param entityList  Entities for which to display data
     * @param downloadColumnSetName Name of the column set to display
     * @param entityName Name of the entity
     * @param entityId ID of the entity
     * @param domainUser BSPDomainUser object for the current user
     * @param sampleCollection Sample Collection for this data
     *
     * @return ConfigurableList instance
     */
    public ConfigurableList create(@Nonnull List<?> entityList,
            @Nonnull String downloadColumnSetName,
            @Nonnull String entityName,
            @Nonnull ColumnEntity entityId/*,
            @Nonnull BspDomainUser domainUser,
            @Nonnull SampleCollection sampleCollection*/) {

        List<ColumnTabulation> columnTabulations = buildColumnSetTabulations(downloadColumnSetName, entityName/*, domainUser, sampleCollection*/);
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, 0, "ASC",
                /*domainUser.isAdmin(), */entityId);
        configurableList.addRows(entityList);
        return configurableList;
    }

    /**
     * Build Column definitions for a column set name
     *
     * @param downloadColumnSetName name of column set that the user wants to download
     * @param entityName            e.g. "Sample"
     * @param domainUser            BSP Domain User
     * @param sampleCollection      Sample Collection for which to build the column definitions
     * @return list of column definitions
     */
    public List<ColumnTabulation> buildColumnSetTabulations(@Nonnull String downloadColumnSetName,
            @Nonnull String entityName/*,
            @Nonnull BspDomainUser domainUser,
            @Nonnull SampleCollection sampleCollection*/) {
        // Determine which set of columns to use
/*
        PreferenceDomain.domain columnSetDomain = null;
        for (PreferenceDomain.domain domain : PreferenceDomain.domain.values()) {
            if (downloadColumnSetName.startsWith(domain.toString())) {
                columnSetDomain = domain;
                break;
            }
        }
        if (columnSetDomain == null) {
            throw new RuntimeException("Failed to extract domain preference from " + downloadColumnSetName);
        }
*/
        String columnSetNameSuffix = downloadColumnSetName.substring(downloadColumnSetName.indexOf('|') + 1);

//        EntityPreferenceFactory entityPreferenceFactory = new EntityPreferenceFactory();
//        PreferenceDefinitionListNameListString columnSets = entityPreferenceFactory.getColumnSets(columnSetDomain,
//                domainUser, sampleCollection.getGroup(), sampleCollection, entityName);
        Preference globalPreference = preferenceDao.getGlobalPreference(PreferenceType.COLUMN_SETS);
        ColumnSetsPreference columnSetsPreference;
        try {
            columnSetsPreference =
                    (ColumnSetsPreference) globalPreference.getPreferenceDefinition().getDefinitionValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // How to store column defs?  If enum, might need multiple for different result entities.  Need enum for
        // columns that aren't in use yet, otherwise would have to do discovery?
        ColumnSetsPreference.ColumnSet columnSet = ConfigurableList.getColumnNameList(columnSetNameSuffix,
                ConfigurableList.ColumnSetType.DOWNLOAD, /*domainUser, sampleCollection.getGroup(), */columnSetsPreference);
//        ListConfig listConfig = entityPreferenceFactory.loadListConfig(entityName);
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        for (ColumnDefinition columnDefinition : columnSet.getColumnDefinitions()) {
            columnTabulations.add(columnDefinition.getColumnTabulation());
        }
        return columnTabulations;
    }
}
