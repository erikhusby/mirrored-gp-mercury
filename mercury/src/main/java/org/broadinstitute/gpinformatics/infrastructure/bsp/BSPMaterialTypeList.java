package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;

/**
 * Application wide access to BSP's material type list. The list is regularly refreshed by ExternalDataCacheControl.
 */
@ApplicationScoped
public class BSPMaterialTypeList extends AbstractCache implements Serializable {

    private static final Log logger = LogFactory.getLog(BSPMaterialTypeList.class);

    @Inject
    private Deployment deployment;

    private BSPManagerFactory bspManagerFactory;

    private List<MaterialType> materialTypes;

    private boolean serverValid;

    public BSPMaterialTypeList() {
    }

    @Inject
    public BSPMaterialTypeList(BSPManagerFactory bspManagerFactory) {
        this.bspManagerFactory = bspManagerFactory;
    }

    public boolean isServerValid() {
        return serverValid;
    }

    /**
     * @return list of bsp materialTypes
     */
    public List<MaterialType> getMaterialTypes() {

        if (materialTypes == null ) {
            refreshCache();
        }

        return materialTypes;
    }

    /**
     * Returns the BSP material type for the given fullName, or null if no material type exists with that name.
     * Comparison ignores case.
     *
     * @param fullName the full name to look for
     * @return the BSP material type or null
     */
    public MaterialType getByFullName(String fullName) {
        if (StringUtils.isNotBlank( fullName )) {
            for (MaterialType materialType : getMaterialTypes()) {
                if (materialType.getFullName().equalsIgnoreCase(fullName.trim())) {
                    return materialType;
                }
            }
        }
        return null;
    }

    public List<MaterialType> getByFullNames(List<String> fullNames) {
        List<MaterialType> results = new ArrayList<>();
        if ( fullNames != null) {
            for (String fullName : fullNames) {
                MaterialType materialType = getByFullName( fullName );
                if ( materialType != null ) {
                    results.add( materialType );
                }
            }
        }
        return results;
    }


    /**
     * Returns a list of BSP material types for the given category, or null if no material type exists with that category.
     * Comparison ignores case.
     *
     * @param category the category to look for
     * @return the list of BSP material types in the category or null
     */
    public List<MaterialType> getByCategory(String category) {
        List<MaterialType> results = new ArrayList<>();
        for (MaterialType materialType : getMaterialTypes()) {
            if (materialType.getCategory().equalsIgnoreCase(category)) {
                results.add( materialType );
            }
        }
        return results;
    }

    /**
     * Returns a list of materialTypes whose name or category match the given query.
     * @param query the query string to match on
     * @return a list of matching materialTypes
     */
    public List<MaterialType> find(String query) {
        Set<MaterialType> materialTypeHashSet = new HashSet<>();
        if ( StringUtils.isNotBlank( query )) {
            for (MaterialType materialType : getMaterialTypes()) {
                StringTokenizer st = new StringTokenizer( query.trim() );
                while (st.hasMoreTokens()) {
                    String queryTokenLowerCase = st.nextToken().toLowerCase();
                    if ( materialType.getFullName().toLowerCase().contains( queryTokenLowerCase ) ) {
                        materialTypeHashSet.add(materialType);
                    }
                }
            }
        }
        return new ArrayList<>(materialTypeHashSet);
    }

    @Override
    public synchronized void refreshCache() {
        try {
            List<MaterialType> materialTypeList = bspManagerFactory.createSampleManager().getMaterialTypes();
            serverValid = materialTypeList != null;

            if (!serverValid) {
                // BSP is down
                if (materialTypes != null) {
                    // I have the old set of materialTypes, so just return.
                    return;
                }
            }

            Collections.sort(materialTypeList, new Comparator<MaterialType>() {
                @Override
                public int compare(MaterialType o1, MaterialType o2) {
                    CompareToBuilder builder = new CompareToBuilder();
                    builder.append(o1.getCategory(), o2.getCategory());
                    builder.append(o1.getName(), o2.getName());
                    return builder.build();
                }
            });

            materialTypes = ImmutableList.copyOf(materialTypeList);
        } catch (Exception ex) {
            logger.error("Could not refresh the material type list", ex);
        }
    }

}
