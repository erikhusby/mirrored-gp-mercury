/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.JsonDecorator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProductOrderRegulatoryInformation implements JsonDecorator {
    private ProductOrder productOrder;
    private ResearchProject researchProject;

    public ProductOrderRegulatoryInformation(@Nonnull ResearchProject researchProject, ProductOrder productOrder) {
        this.researchProject = researchProject;
        this.productOrder = productOrder;
    }


    @Override
    public String getJson() throws JSONException {
        JSONArray itemList = new JSONArray();
        Map<String, Collection<RegulatoryInfo>>
                regulatoryInfoByProject = setupRegulatoryInformation(researchProject);
        for (Map.Entry<String, Collection<RegulatoryInfo>> regulatoryEntries : regulatoryInfoByProject.entrySet()) {
            if (!regulatoryEntries.getValue().isEmpty()) {
                JSONObject item = new JSONObject();
                item.put("group", regulatoryEntries.getKey());
                JSONArray values = new JSONArray();
                for (RegulatoryInfo regulatoryInfo : regulatoryEntries.getValue()) {
                    JSONObject regulatoryInfoJson = new JSONObject();
                    regulatoryInfoJson.put("key", regulatoryInfo.getBusinessKey());
                    regulatoryInfoJson.put("value", regulatoryInfo.getDisplayText());
                    if (productOrder != null && productOrder.getRegulatoryInfos().contains(regulatoryInfo)) {
                        regulatoryInfoJson.put("selected", true);
                    }
                    values.put(regulatoryInfoJson);
                }
                item.put("value", values);
                itemList.put(item);
            }
        }

        return itemList.toString();
    }

    /**
     * Creates a map of available regulatory information for given researchProject.
     * The key is the project title of research project and any parent research projects (recursively).
     */
    public Map<String, Collection<RegulatoryInfo>> setupRegulatoryInformation(ResearchProject researchProject) {
        Map<String, Collection<RegulatoryInfo>> projectRegulatoryMap = new HashMap<>();
        projectRegulatoryMap.put(researchProject.getTitle(), researchProject.getRegulatoryInfos());
        for (ResearchProject project : researchProject.getAllParents()) {
            projectRegulatoryMap.put(project.getTitle(), project.getRegulatoryInfos());
        }
        return projectRegulatoryMap;
    }
}
