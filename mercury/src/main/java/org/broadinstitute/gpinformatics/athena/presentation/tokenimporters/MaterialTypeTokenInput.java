package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPMaterialTypeList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is the user implementation of the token object
 *
 * @author hrafal
 */
@Named
public class MaterialTypeTokenInput extends TokenInput<MaterialType> {

    @Inject
    private BSPMaterialTypeList materialTypeListCache;

    public MaterialTypeTokenInput() {
    }

    @Override
    protected MaterialType getById(String name) {
        return materialTypeListCache.getByFullName(name);
    }

    public String getJsonString(String query) throws JSONException {

        if (materialTypeListCache == null) {
            return "";
        }

        Collection<MaterialType> materialTypes = materialTypeListCache.find(query);

        JSONArray itemList = new JSONArray();
        for (MaterialType materialType : materialTypes) {
            createAutocomplete(itemList, materialType);
        }

        return itemList.toString();
    }

    @Override
    protected String generateCompleteData() throws JSONException {
        JSONArray itemList = new JSONArray();
        for (MaterialType materialType : getTokenObjects()) {
            createAutocomplete(itemList, materialType);
        }

        return itemList.toString();
    }

    private static void createAutocomplete(JSONArray itemList, MaterialType materialType) throws JSONException {
        JSONObject item = getJSONObject(materialType.getFullName(), materialType.getFullName(), false);
        itemList.put(item);
    }

    public Collection<? extends org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType> getMercuryTokenObjects() {
        List<org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType> mercuryTokenObjects =
                new ArrayList<org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType>();

        for (MaterialType materialType : getTokenObjects()) {
            org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType mercuryMaterialType =
                    new org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType(materialType.getCategory(), materialType.getName());
            mercuryMaterialType.setFullName(materialType.getFullName());

            mercuryTokenObjects.add(mercuryMaterialType);
        }

        return mercuryTokenObjects;
    }
}
