package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.sample.MaterialInfo;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Application wide access to BSP's material type list. The list is regularly refreshed by ExternalDataCacheControl.
 */
@ApplicationScoped
public class BSPMaterialInfoList extends AbstractCache implements Serializable {

    private static final Log logger = LogFactory.getLog(BSPMaterialInfoList.class);

    @Inject
    private Deployment deployment;

    private BSPManagerFactory bspManagerFactory;

    private List<MaterialInfo> materialInfoList;

    private boolean serverValid;

    public BSPMaterialInfoList() {
    }

    @Inject
    public BSPMaterialInfoList(BSPManagerFactory bspManagerFactory) {
        this.bspManagerFactory = bspManagerFactory;
    }

    public boolean isServerValid() {
        return serverValid;
    }

    /**
     * @return list of bsp materialInfoList
     */
    public List<MaterialInfo> getMaterialInfoList() {
        if (materialInfoList == null ) {
            refreshCache();
        }

        return materialInfoList;
    }

    /**
     * Returns the BSP material type for the given fullName, or null if no material type exists with that name.
     * Comparison ignores case.
     *
     * @param fullName the full name to look for
     * @return the BSP material type or null
     */
    public MaterialInfo getByFullName(String fullName) {
        if (StringUtils.isNotBlank( fullName )) {
            for (MaterialInfo materialInfo : getMaterialInfoList()) {
                if (materialInfo.getFullName().equalsIgnoreCase(fullName.trim())) {
                    return materialInfo;
                }
            }
        }
        return null;
    }

    public List<MaterialInfo> getByFullNames(List<String> fullNames) {
        List<MaterialInfo> results = new ArrayList<>();
        if ( fullNames != null) {
            for (String fullName : fullNames) {
                MaterialInfo materialInfo = getByFullName( fullName );
                if ( materialInfo != null ) {
                    results.add( materialInfo );
                }
            }
        }
        return results;
    }


    /**
     * Returns a list of BSP material types for the given kitType, or null if no material type exists with that kitType.
     * Comparison ignores case.
     *
     * @param kitType the kitType to look for
     * @return the list of BSP material types in the kitType or null
     */
    public List<MaterialInfo> getByKitType(String kitType) {
        List<MaterialInfo> results = new ArrayList<>();
        for (MaterialInfo materialInfo : getMaterialInfoList()) {
            if (materialInfo.getKitType().equalsIgnoreCase(kitType)) {
                results.add( materialInfo );
            }
        }
        return results;
    }

    /**
     * Returns a list of materialInfoList whose name or category match the given query.
     * @param query the query string to match on
     * @return a list of matching materialInfoList
     */
    public List<MaterialInfo> find(String query) {
        Set<MaterialInfo> materialInfoHashSet = new HashSet<>();
        if (StringUtils.isNotBlank(query)) {
            for (MaterialInfo materialInfo : getMaterialInfoList()) {
                StringTokenizer st = new StringTokenizer(query.trim());
                while (st.hasMoreTokens()) {
                    String queryTokenLowerCase = st.nextToken().toLowerCase();
                    if (materialInfo.getFullName().toLowerCase().contains(queryTokenLowerCase)) {
                        materialInfoHashSet.add(materialInfo);
                    }
                }
            }
        }
        return new ArrayList<>(materialInfoHashSet);
    }

    @Override
    public synchronized void refreshCache() {
        try {
            List<MaterialInfo> materialInfoList = bspManagerFactory.createSampleManager().getMaterialInfo();
            serverValid = materialInfoList != null;

            if (!serverValid) {
                // BSP is down
                if (materialInfoList != null) {
                    // I have the old set of materialInfoList, so just return.
                    return;
                }
            }

            Collections.sort(materialInfoList, new Comparator<MaterialInfo>() {
                @Override
                public int compare(MaterialInfo o1, MaterialInfo o2) {
                    CompareToBuilder builder = new CompareToBuilder();
                    builder.append(o1.getKitType(), o2.getKitType());
                    builder.append(o1.getMaterialTypes(), o2.getMaterialTypes());
                    return builder.build();
                }
            });

            materialInfoList = ImmutableList.copyOf(materialInfoList);
        } catch (Exception ex) {
            logger.error("Could not refresh the material type list", ex);
        }
    }

}
