package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPMaterialTypeList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Token Input support for Material Type.
 *
 * @author hrafal
 */
public class MaterialTypeTokenInput extends TokenInput<MaterialType> {

    @Inject
    private BSPMaterialTypeList materialTypeListCache;

    public MaterialTypeTokenInput() {
    }

    // There are commas in material types so use the | character for the separator
    @Override
    public String getTokenSeparator() {
        return "\\|";
    }

    // There are commas in material types so use the | character for the separator
    @Override
    public String getJoinSeparator() {
        return "|";
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
        return createItemListString(new ArrayList<MaterialType>(materialTypes));
    }

    @Override
    protected JSONObject createAutocomplete(JSONArray itemList, MaterialType materialType) throws JSONException {
        if (materialType != null) {
            JSONObject item = getJSONObject(materialType.getFullName(), materialType.getFullName(), false);
            itemList.put(item);
            return item;
        }

        return null;
    }

    public Collection<? extends org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType> getMercuryTokenObjects() {
        List<org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType> mercuryTokenObjects =
                new ArrayList<org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType>();

        for (MaterialType materialType : getTokenObjects()) {
            if (materialType != null) {
                org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType mercuryMaterialType =
                        new org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType(materialType.getCategory(), materialType.getName());

                mercuryTokenObjects.add(mercuryMaterialType);
            }
        }

        return mercuryTokenObjects;
    }
}
